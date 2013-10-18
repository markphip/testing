package com.atlassian.jira.plugins.dvcs.dao.impl.transform;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.dao.RepositoryDao;
import com.atlassian.jira.plugins.dvcs.model.PullRequest;
import com.atlassian.jira.plugins.dvcs.model.PullRequestRef;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PullRequestTransformer
{
    public static final Logger log = LoggerFactory.getLogger(PullRequestTransformer.class);

    private final RepositoryService repositoryService;

    public PullRequestTransformer(final RepositoryService repositoryService)
    {
        this.repositoryService = repositoryService;
    }

    public PullRequest transform(RepositoryPullRequestMapping pullRequestMapping)
    {
        if (pullRequestMapping == null)
        {
            return null;
        }

        Repository repository = repositoryService.get(pullRequestMapping.getToRepositoryId());

        final PullRequest pullRequest = new PullRequest(pullRequestMapping.getToRepositoryId());
        pullRequest.setRemoteId(pullRequestMapping.getRemoteId());
        pullRequest.setRepositoryId(pullRequestMapping.getToRepositoryId());
        pullRequest.setName(pullRequestMapping.getName());
        pullRequest.setUrl(pullRequestMapping.getUrl());

        pullRequest.setSource(new PullRequestRef(pullRequestMapping.getSourceBranch(), pullRequestMapping.getSourceRepo(), createRepositoryUrl(repository.getOrgHostUrl(), pullRequestMapping.getSourceRepo())));
        pullRequest.setDestination(new PullRequestRef(pullRequestMapping.getDestinationBranch(), createRepositoryLabel(repository), repository.getRepositoryUrl()));

        pullRequest.setStatus(pullRequestMapping.getLastStatus());
        pullRequest.setCreatedOn(pullRequestMapping.getCreatedOn());
        pullRequest.setUpdatedOn(pullRequestMapping.getUpdatedOn());
        pullRequest.setAuthor(pullRequestMapping.getAuthor());

        return pullRequest;
    }

    private String createRepositoryUrl(String hostUrl, String repositoryLabel)
    {
        // normalize
        if (hostUrl != null && hostUrl.endsWith("/"))
        {
            hostUrl = hostUrl.substring(0, hostUrl.length() - 1);
        }
        return hostUrl + "/" + repositoryLabel;
    }

    private String createRepositoryLabel(Repository repository)
    {
        return repository.getOwner() + "/" + repository.getSlug();
    }
}
