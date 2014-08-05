package com.atlassian.jira.plugins.dvcs.service.api;

import com.atlassian.jira.plugins.dvcs.model.PullRequest;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.PullRequestService;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;

public class DvcsPullRequestServiceImpl implements DvcsPullRequestService
{
    private PullRequestService pullRequestService;

    public DvcsPullRequestServiceImpl(final PullRequestService pullRequestService)
    {
        this.pullRequestService = pullRequestService;
    }

    @Nonnull
    @Override
    public Set<String> getIssueKeys(int repositoryId, int pullRequestId)
    {
        return pullRequestService.getIssueKeys(repositoryId, pullRequestId);
    }

    @Override
    @Nonnull
    public List<PullRequest> getPullRequests(final Iterable<String> issueKeys)
    {
        return ImmutableList.copyOf(pullRequestService.getByIssueKeys(issueKeys));
    }

    @Override
    public List<PullRequest> getPullRequests(final Iterable<String> issueKeys, final String dvcsType)
    {
        return ImmutableList.copyOf(pullRequestService.getByIssueKeys(issueKeys, dvcsType));
    }

    @Override
    public String getCreatePullRequestUrl(Repository repository, String sourceSlug, String sourceBranch,
                                          String destinationSlug, String destinationBranch, String eventSource)
    {
        return pullRequestService.getCreatePullRequestUrl(repository, sourceSlug, sourceBranch, destinationSlug,
                destinationBranch, eventSource);
    }
}
