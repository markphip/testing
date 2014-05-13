package com.atlassian.jira.plugins.dvcs.sync;

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
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.message.GitHubSynchronizeChangesetsMessage;
import com.atlassian.jira.plugins.dvcs.spi.github.message.SynchronizeChangesetMessage;
import com.atlassian.jira.plugins.dvcs.spi.github.parsers.GithubChangesetFactory;
import org.apache.commons.collections.CollectionUtils;
import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.service.CommitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import javax.annotation.Resource;

/**
 * Consumer of {@link GitHubSynchronizeChangesetsMessage}-s.
 *
 * @author Miroslav Stencel
 *
 */
public class GithubSynchronizeChangesetsMessageConsumer implements MessageConsumer<GitHubSynchronizeChangesetsMessage>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(GithubSynchronizeChangesetsMessageConsumer.class);
    private static final String ID = GithubSynchronizeChangesetsMessageConsumer.class.getCanonicalName();
    public static final String KEY = GitHubSynchronizeChangesetsMessage.class.getCanonicalName();

    @Resource
    private MessagingService messagingService;

    @Resource
    protected RepositoryService repositoryService;

    @Resource
    protected ChangesetService changesetService;

    @Resource
    protected LinkedIssueService linkedIssueService;

    @Resource(name = "githubClientProvider")
    private GithubClientProvider gitHubClientProvider;

    @Override
    public void onReceive(Message<GitHubSynchronizeChangesetsMessage> message, GitHubSynchronizeChangesetsMessage payload)
    {
        Repository repository = payload.getRepository();
        final Progress progress = payload.getProgress();

        String firstSha = payload.getFirstSha();
        boolean softSync = payload.isSoftSync();

        String[] tags = message.getTags();
        int priority = getPriority(payload);

        CommitService commitService = gitHubClientProvider.getCommitService(repository);
        int pagelen = payload.getPagelen();
        if (firstSha != null)
        {
            // we need to skip first sha, therefore we request one more commit
            pagelen = pagelen + 1;
        }

        progress.incrementRequestCount(new Date());
        long startFlightTime = System.currentTimeMillis();
        PageIterator<RepositoryCommit> pages;

        try
        {
            pages = commitService.pageCommits(RepositoryId.createFromUrl(repository.getRepositoryUrl()), firstSha, null, pagelen);
        }
        finally
        {
            progress.addFlightTimeMs((int) (System.currentTimeMillis() - startFlightTime));
        }

        boolean stopPaging = false;
        String lastSha = null;

        Date lastCommitDate = repository.getLastCommitDate();

        if (pages.hasNext())
        {
            Collection<RepositoryCommit> commits = pages.next();
            for (RepositoryCommit commit : commits)
            {
                // skip first firstSha
                if (commit.getSha().equals(firstSha))
                {
                    continue;
                }

                lastSha = commit.getSha();
                Date synchronizedAt = new Date();
                String branch = getBranch(commit, payload);
                // transforming to Changeset to be sure that we use correct commitDate
                Changeset changeset = GithubChangesetFactory.transformToChangeset(commit, repository.getId(), branch);
                Date commitDate = changeset.getDate();
                if (commitDate != null && payload.getLastCommitDate() != null && !payload.getLastCommitDate().before(commitDate))
                {
                    // we found commit that has been synchronized, we can stop next page iteration
                    stopPaging = true;
                    break;
                }

                // if we find newer last commit date we mark this in repository object
                if (lastCommitDate == null || commitDate.after(lastCommitDate))
                {
                    lastCommitDate = commitDate;
                    repository.setLastCommitDate(lastCommitDate);
                    repositoryService.save(repository);
                }

                // changeset is already synchronized
                if (changesetService.getByNode(repository.getId(), commit.getSha()) != null)
                {
                    continue;
                }

                changeset.setSynchronizedAt(synchronizedAt);

                Set<String> issues = linkedIssueService.getIssueKeys(changeset.getMessage());
                MessageConsumerSupport.markChangesetForSmartCommit(repository, changeset, softSync && CollectionUtils.isNotEmpty(issues));

                changesetService.create(changeset, issues);

                progress.inProgress( //
                        payload.getProgress().getChangesetCount() + 1, //
                        payload.getProgress().getJiraCount() + issues.size(), //
                        0 //
                );
            }
        }

        if (stopPaging || !pages.hasNext())
        {
            Map<String, String> nodesToBranches = payload.getNodesToBranches();
            for (String node : nodesToBranches.keySet())
            {
                if (changesetService.getByNode(repository.getId(), node) == null)
                {
                    // we found node that is not synchronized

                    SynchronizeChangesetMessage newMessage = new SynchronizeChangesetMessage(payload.getRepository(), nodesToBranches.get(node), node,
                            payload.getRefreshAfterSynchronizedAt(), payload.getProgress(), payload.isSoftSync(), payload.getSyncAuditId());

                    MessageAddress<SynchronizeChangesetMessage> address = messagingService.get( //
                            SynchronizeChangesetMessage.class, //
                            GithubSynchronizeChangesetMessageConsumer.ADDRESS //
                    );

                    messagingService.publish(address, newMessage, priority, tags);
                }
            }
        } else
        {
            GitHubSynchronizeChangesetsMessage newMessage = new GitHubSynchronizeChangesetsMessage(repository,
                    payload.getRefreshAfterSynchronizedAt(), payload.getProgress(), lastSha, payload.getLastCommitDate(),
                    payload.getPagelen(), payload.getNodesToBranches(), payload.isSoftSync(), payload.getSyncAuditId());
            messagingService.publish(getAddress(), newMessage, priority, tags);
        }
    }

    private String getBranch(RepositoryCommit commit, GitHubSynchronizeChangesetsMessage originalMessage)
    {
        Map<String, String> changesetBranch = originalMessage.getNodesToBranches();

        String branch = changesetBranch.get(commit.getSha());

        changesetBranch.remove(commit.getSha());
        for (Commit parent : commit.getParents())
        {
            if (!changesetBranch.containsKey(parent.getSha()))
            {
                changesetBranch.put(parent.getSha(), branch);
            }
        }

        return branch;
    }

    private int getPriority(GitHubSynchronizeChangesetsMessage payload)
    {
        if (payload == null)
        {
            return MessagingService.DEFAULT_PRIORITY;
        }
        return payload.isSoftSync() ? MessagingService.SOFTSYNC_PRIORITY: MessagingService.DEFAULT_PRIORITY;
    }

    @Override
    public String getQueue()
    {
        return ID;
    }

    @Override
    public MessageAddress<GitHubSynchronizeChangesetsMessage> getAddress()
    {
        return messagingService.get(GitHubSynchronizeChangesetsMessage.class, KEY);
    }

    @Override
    public int getParallelThreads()
    {
        return MessageConsumer.THREADS_PER_CONSUMER;
    }
}
