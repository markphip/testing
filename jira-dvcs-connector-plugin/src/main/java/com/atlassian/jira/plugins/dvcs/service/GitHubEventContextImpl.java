package com.atlassian.jira.plugins.dvcs.service;

import java.util.HashSet;
import java.util.Set;

import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.spi.github.message.GitHubPullRequestSynchronizeMessage;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventContext;
import com.atlassian.jira.plugins.dvcs.sync.GitHubPullRequestSynchronizeMessageConsumer;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;

/**
 * Context for GitHub event synchronisation
 *
 */
public class GitHubEventContextImpl implements GitHubEventContext
{
    private final Synchronizer synchronizer;
    private final MessagingService messagingService;

    private final Repository repository;
    private final boolean isSoftSync;
    private final String[] synchronizationTags;

    private final Set<Long> processedPullRequests = new HashSet<Long>();

    public GitHubEventContextImpl(final Synchronizer synchronizer, final MessagingService messagingService, final Repository repository, final boolean softSync, final String[] synchronizationTags)
    {
        this.synchronizer = synchronizer;
        this.messagingService = messagingService;
        this.repository = repository;
        isSoftSync = softSync;
        this.synchronizationTags = synchronizationTags;
    }

    @Override
    public void savePullRequest(long pullRequestId, int pullRequestNumber)
    {
        if (processedPullRequests.contains(pullRequestId))
        {
            return;
        }

        processedPullRequests.add(pullRequestId);

        Progress progress = synchronizer.getProgress(repository.getId());
        GitHubPullRequestSynchronizeMessage message = new GitHubPullRequestSynchronizeMessage(progress, progress.getAuditLogId(),
                isSoftSync, repository, pullRequestNumber);

        messagingService.publish(
                messagingService.get(GitHubPullRequestSynchronizeMessage.class, GitHubPullRequestSynchronizeMessageConsumer.ADDRESS),
                message, synchronizationTags);
    }
}
