package com.atlassian.jira.plugins.dvcs.sync.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.SyncAuditLogMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestDao;
import com.atlassian.jira.plugins.dvcs.dao.RepositoryDao;
import com.atlassian.jira.plugins.dvcs.dao.SyncAuditLogDao;
import com.atlassian.jira.plugins.dvcs.listener.PostponeOndemandPrSyncListener;
import com.atlassian.jira.plugins.dvcs.model.Branch;
import com.atlassian.jira.plugins.dvcs.model.BranchHead;
import com.atlassian.jira.plugins.dvcs.model.DefaultOrganizationProgress;
import com.atlassian.jira.plugins.dvcs.model.DefaultProgress;
import com.atlassian.jira.plugins.dvcs.model.OrganizationProgress;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.BranchService;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.service.remote.CachingDvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventService;
import com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import com.google.common.base.Throwables;
import com.google.common.collect.MapMaker;

/**
 * Synchronization service
 */
public class DefaultSynchronizer implements Synchronizer, DisposableBean, InitializingBean
{
    private final Logger log = LoggerFactory.getLogger(DefaultSynchronizer.class);

    private final String DISABLE_SYNCHRONIZATION_FEATURE = "dvcs.connector.synchronization.disabled";

    @Resource
    private MessagingService messagingService;

    @Resource
    private ChangesetService changesetService;

    @Resource
    private BranchService branchService;

    @Resource
    private DvcsCommunicatorProvider communicatorProvider;

    @Resource
    private RepositoryDao repositoryDao;

    @Resource
    private RepositoryPullRequestDao repositoryPullRequestDao;

    @Resource
    private PostponeOndemandPrSyncListener postponePrSyncHelper;

    @Resource
    private SyncAuditLogDao syncAudit;

    @Resource
    private FeatureManager featureManager;

    /**
     * Injected
     * {@link com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventService}
     * dependency.
     */
    @Resource
    private GitHubEventService gitHubEventService;

    // map of ALL Synchronisation Progresses - running and finished ones
    private final ConcurrentMap<Integer, Progress> progressMap = new MapMaker().makeMap();

    private final OrganizationProgress orgProgress = new DefaultOrganizationProgress();

    public DefaultSynchronizer()
    {
        super();
    }

    @Override
    public void doSync(final Repository repo, final EnumSet<SynchronizationFlag> flags)
    {
        if (featureManager.isEnabled(DISABLE_SYNCHRONIZATION_FEATURE))
        {
            log.info("The synchronization is disabled.");
            return;
        }

        if (repo.isLinked())
        {
            Progress progress = null;

            synchronized (this)
            {
                if (skipSync(repo, flags))
                {
                    return;
                }

                progress = startProgress(repo);
            }

            final boolean softSync = flags.contains(SynchronizationFlag.SOFT_SYNC);
            final boolean changesetsSync = flags.contains(SynchronizationFlag.SYNC_CHANGESETS);
            final boolean pullRequestSync = flags.contains(SynchronizationFlag.SYNC_PULL_REQUESTS);

            int auditId = 0;
            try
            {
                // audit
                auditId = syncAudit.newSyncAuditLog(repo.getId(), getSyncType(softSync), new Date(progress.getStartTime())).getID();
                progress.setAuditLogId(auditId);

                if (!softSync)
                {
                    // TODO This will deleted both changeset and PR messages, we
                    // should distinguish between them
                    // Stopping synchronization to delete failed messages for
                    // repository
                    stopSynchronization(repo);
                    if (changesetsSync)
                    {
                        // we are doing full sync, lets delete all existing
                        // changesets
                        // also required as GHCommunicator.getChangesets()
                        // returns only changesets not already stored in
                        // database
                        changesetService.removeAllInRepository(repo.getId());
                        branchService.removeAllBranchHeadsInRepository(repo.getId());
                        branchService.removeAllBranchesInRepository(repo.getId());

                        repo.setLastCommitDate(null);
                    }
                    if (pullRequestSync)
                    {
                        gitHubEventService.removeAll(repo);
                        repositoryPullRequestDao.removeAll(repo);
                        repo.setActivityLastSync(null);
                    }
                    repositoryDao.save(repo);
                }

                // first retry all failed messages
                try
                {
                    messagingService.retry(messagingService.getTagForSynchronization(repo));
                }
                catch (final Exception e)
                {
                    log.warn("Could not resume failed messages.", e);
                }

                if (!postponePrSyncHelper.isAfterPostponedTime())
                {
                    flags.remove(SynchronizationFlag.SYNC_PULL_REQUESTS);
                }

                final CachingDvcsCommunicator communicator = (CachingDvcsCommunicator) communicatorProvider.getCommunicator(repo
                        .getDvcsType());

                communicator.startSynchronisation(repo, flags, auditId);
            }
            catch (final Throwable t)
            {
                log.error(t.getMessage(), t);
                progress.setError("Error during sync. See server logs.");
                syncAudit.setException(auditId, t, false);
                Throwables.propagateIfInstanceOf(t, Error.class);
            }
            finally
            {
                messagingService.tryEndProgress(repo, progress, null, auditId);
            }
        }
    }

    private Progress startProgress(final Repository repository)
    {
        final DefaultProgress progress = new DefaultProgress();
        progress.start();
        putProgress(repository, progress);
        return progress;
    }

    protected String getSyncType(final boolean softSync)
    {
        return softSync ? SyncAuditLogMapping.SYNC_TYPE_SOFT : SyncAuditLogMapping.SYNC_TYPE_FULL;
    }

    private boolean skipSync(final Repository repository, final EnumSet<SynchronizationFlag> flags)
    {
        final Progress progress = getProgress(repository.getId());

        if (progress == null || progress.isFinished())
        {
            return false;
        }

        if (flags.contains(SynchronizationFlag.WEBHOOK_SYNC))
        {
            log.info("Postponing post webhook synchronization. It will start after the running synchronization finishes.");

            final EnumSet<SynchronizationFlag> currentFlags = progress.getRunAgainFlags();
            if (currentFlags == null)
            {
                progress.setRunAgainFlags(flags);
            }
            else
            {
                currentFlags.addAll(flags);
            }
        }
        return true;
    }

    private void updateBranches(final Repository repo, final List<Branch> newBranches)
    {
        branchService.updateBranches(repo, newBranches);
    }

    private void updateBranchHeads(final Repository repo, final List<Branch> newBranches, final List<BranchHead> oldHeads)
    {
        branchService.updateBranchHeads(repo, newBranches, oldHeads);
    }

    private List<String> extractBranchHeadsFromBranches(final List<Branch> branches)
    {
        if (branches == null)
        {
            return null;
        }
        final List<String> result = new ArrayList<String>();
        for (final Branch branch : branches)
        {
            for (final BranchHead branchHead : branch.getHeads())
            {
                result.add(branchHead.getHead());
            }
        }
        return result;
    }

    private List<String> extractBranchHeads(final List<BranchHead> branchHeads)
    {
        if (branchHeads == null)
        {
            return null;
        }
        final List<String> result = new ArrayList<String>();
        for (final BranchHead branchHead : branchHeads)
        {
            result.add(branchHead.getHead());
        }
        return result;
    }

    @Override
    public void stopSynchronization(final Repository repository)
    {
        messagingService.cancel(messagingService.getTagForSynchronization(repository));
    }

    @Override
    public void pauseSynchronization(final Repository repository, final boolean pause)
    {
        if (pause)
        {
            messagingService.pause(messagingService.getTagForSynchronization(repository));
        }
        else
        {
            messagingService.resume(messagingService.getTagForSynchronization(repository));
        }

    }

    @Override
    public Progress getProgress(final int repositoryId)
    {
        return progressMap.get(repositoryId);
    }

    @Override
    public void putProgress(final Repository repository, final Progress progress)
    {
        progressMap.put(repository.getId(), progress);
    }

    @Override
    public void removeProgress(final Repository repository)
    {
        progressMap.remove(repository.getId());
    }

    @Override
    public void destroy() throws Exception
    {
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
    }

    @Override
    public OrganizationProgress getOrganizationProgress()
    {
        return orgProgress;
    }
}
