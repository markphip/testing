package com.atlassian.jira.plugins.dvcs.sync;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketHttpError;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BitbucketRequestException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.LinkedIssueService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.service.message.MessageAddress;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.service.remote.CachingDvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetPage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketNewChangeset;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.message.BitbucketSynchronizeChangesetMessage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers.ChangesetTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Consumer of {@link BitbucketSynchronizeChangesetMessage}-s.
 *
 * @author Stanislav Dvorscak
 *
 */
public class BitbucketSynchronizeChangesetMessageConsumer implements MessageConsumer<BitbucketSynchronizeChangesetMessage>
{
    private static final Logger log = LoggerFactory.getLogger(BitbucketSynchronizeChangesetMessageConsumer.class);

    private static final String ID = BitbucketSynchronizeChangesetMessageConsumer.class.getCanonicalName();
    public static final String KEY = BitbucketSynchronizeChangesetMessage.class.getCanonicalName();

    @Resource(name = "cachingBitbucketCommunicator")
    private CachingDvcsCommunicator cachingCommunicator;
    @Resource
    private ChangesetService changesetService;
    @Resource
    private RepositoryService repositoryService;
    @Resource
    private LinkedIssueService linkedIssueService;
    @Resource
    private MessagingService messagingService;

    public BitbucketSynchronizeChangesetMessageConsumer()
    {
    }

    @Override
    public void onReceive(Message<BitbucketSynchronizeChangesetMessage> message, final BitbucketSynchronizeChangesetMessage payload)
    {
        final BitbucketCommunicator communicator = (BitbucketCommunicator) cachingCommunicator.getDelegate();

        BitbucketChangesetPage page = null;

        try
        {
            page = FlightTimeInterceptor.execute(payload.getProgress(), new FlightTimeInterceptor.Callable<BitbucketChangesetPage>()
        {
            @Override
            public BitbucketChangesetPage call()
            {
                return communicator.getNextPage(payload.getRepository(), payload.getInclude(), payload.getExclude(), payload.getPage());
            }
            });
        }
        catch (BitbucketRequestException.NotFound_404 e)
        {
            retryWithoutInvalidNodes(message, payload, e);
        }

        if (page != null)
        {
            process(message, payload, page);
        }
    }

    private void retryWithoutInvalidNodes(Message<BitbucketSynchronizeChangesetMessage> message, BitbucketSynchronizeChangesetMessage payload, BitbucketRequestException.NotFound_404 e)
    {
        BitbucketHttpError error;
        try
        {
            error = ClientUtils.fromJson(e.getContent(), BitbucketHttpError.class);
        } catch (Exception e2)
        {
            log.warn("Content from http error response could not be parsed.", e2);
            throw e;
        }

        List<String> invalidShas = extractInvalidShas(error);

        List<String> includes = payload.getInclude();
        includes.removeAll(invalidShas);
        if (includes.isEmpty())
        {
            log.warn("Include nodes list is empty, all commits will be requested.");
        }
        List<String> excludes = payload.getExclude();
        excludes.removeAll(invalidShas);
        if (excludes.isEmpty())
        {
            log.warn("Exclude nodes list is empty, no commits will be excluded.");
        }

        messagingService.publish(
                getAddress(), //
                new BitbucketSynchronizeChangesetMessage(payload.getRepository(), //
                        payload.getRefreshAfterSynchronizedAt(), //
                        payload.getProgress(), //
                        includes, excludes, payload.getPage(), payload.getNodesToBranches(), payload.isSoftSync(), payload.getSyncAuditId()), payload.isSoftSync() ? MessagingService.SOFTSYNC_PRIORITY: MessagingService.DEFAULT_PRIORITY, message.getTags());
    }

    private List<String> extractInvalidShas(final BitbucketHttpError error)
    {
        if (error.getData() != null)
        {
            return error.getData().getShas();
        }

        return null;
    }

    private void process(Message<BitbucketSynchronizeChangesetMessage> message, BitbucketSynchronizeChangesetMessage payload, BitbucketChangesetPage page)
    {
        List<BitbucketNewChangeset> csets = page.getValues();
        boolean softSync = payload.isSoftSync();

        for (BitbucketNewChangeset ncset : csets)
        {
            Repository repo = payload.getRepository();
            Changeset fromDB = changesetService.getByNode(repo.getId(), ncset.getHash());
            if (fromDB != null)
            {
                continue;
            }
            assignBranch(ncset, payload);
            Changeset cset = ChangesetTransformer.fromBitbucketNewChangeset(repo.getId(), ncset);
            cset.setSynchronizedAt(new Date());
            Set<String> issues = linkedIssueService.getIssueKeys(cset.getMessage());
            
            MessageConsumerSupport.markChangesetForSmartCommit(repo, cset, softSync && CollectionUtils.isNotEmpty(issues));

            changesetService.create(cset, issues);

            if (repo.getLastCommitDate() == null || repo.getLastCommitDate().before(cset.getDate()))
            {
                payload.getRepository().setLastCommitDate(cset.getDate());
                repositoryService.save(payload.getRepository());
            }

            payload.getProgress().inProgress( //
                    payload.getProgress().getChangesetCount() + 1, //
                    payload.getProgress().getJiraCount() + issues.size(), //
                    0 //
                    );
        }

        if (StringUtils.isNotBlank(page.getNext()))
        {
            fireNextPage(page, payload, softSync, message.getTags());
        }
    }

    private void assignBranch(BitbucketNewChangeset cset, BitbucketSynchronizeChangesetMessage originalMessage)
    {
        Map<String, String> changesetBranch = originalMessage.getNodesToBranches();

        String branch = changesetBranch.get(cset.getHash());
        cset.setBranch(branch);
        changesetBranch.remove(cset.getHash());
        for (BitbucketNewChangeset parent : cset.getParents())
        {
            changesetBranch.put(parent.getHash(), branch);
        }
    }

    private void fireNextPage(BitbucketChangesetPage prevPage, BitbucketSynchronizeChangesetMessage originalMessage, boolean softSync, String[] tags)
    {
        messagingService.publish(
                getAddress(), //
                new BitbucketSynchronizeChangesetMessage(originalMessage.getRepository(), //
                        originalMessage.getRefreshAfterSynchronizedAt(), //
                        originalMessage.getProgress(), //
                        originalMessage.getInclude(), originalMessage.getExclude(), prevPage, originalMessage.getNodesToBranches(), originalMessage.isSoftSync(), originalMessage.getSyncAuditId()), softSync ? MessagingService.SOFTSYNC_PRIORITY: MessagingService.DEFAULT_PRIORITY, tags);
    }

    @Override
    public String getQueue()
    {
        return ID;
    }

    @Override
    public MessageAddress<BitbucketSynchronizeChangesetMessage> getAddress()
    {
        return messagingService.get(BitbucketSynchronizeChangesetMessage.class, KEY);
    }

    @Override
    public int getParallelThreads()
    {
        return MessageConsumer.THREADS_PER_CONSUMER;
    }
}
