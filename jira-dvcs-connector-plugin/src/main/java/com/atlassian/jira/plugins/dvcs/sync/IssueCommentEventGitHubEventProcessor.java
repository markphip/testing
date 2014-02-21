package com.atlassian.jira.plugins.dvcs.sync;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.event.IssueCommentPayload;
import org.eclipse.egit.github.core.service.PullRequestService;

import com.atlassian.jira.plugins.dvcs.github.api.model.GitHubEvent;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventContext;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventProcessor;

/**
 * The {@link IssueCommentPayload} event processor.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class IssueCommentEventGitHubEventProcessor implements GitHubEventProcessor
{

    /**
     * Injected {@link GithubClientProvider} dependency.
     */
    @Resource
    private GithubClientProvider githubClientProvider;

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(Repository repository, GitHubEvent gitHubEvent, boolean isSoftSync, String[] synchronizationTags,
            GitHubEventContext context)
    {
        @SuppressWarnings("unchecked")
        Map<String, Object> issuePayload = (Map<String, Object>) gitHubEvent.getPayload().get("issue");

        if (isPullRequestIssueComment(issuePayload))
        {
            // pull request number is the same as issue number (pull request - is no more than special kind of issue)
            Integer pullRequestNumber = (Integer) issuePayload.get("number");
            PullRequest pullRequest = getRemotePullRequest(repository, pullRequestNumber);
            if (pullRequest == null)
            {
                // pull request has to exist - GitHub does not provide support for issue / pull request deletion - they can be only closed
                throw new RuntimeException("Pull request does not exist, repository: " + repository.getOrgName() + "/"
                        + repository.getName() + " pull request number: " + pullRequestNumber);
            }
            context.savePullRequest(pullRequest.getId(), pullRequest.getNumber());
        }
    }

    /**
     * Checks, that this comment is pull request issue comment => contains pull request information in payload.
     * 
     * @param issuePayload
     * @return true if this comment is pull request issue comment
     */
    private boolean isPullRequestIssueComment(Map<String, Object> issuePayload)
    {
        @SuppressWarnings("unchecked")
        Map<String, Object> pullRequestInfo = (Map<String, Object>) issuePayload.get("pull_request");

        // GitHub API until v3:
        // if issue comment is not done on pull request issue, than GitHub provides empty "pull_request" object instead of null
        // e.g.: "pull_request": { "html_url": null, "diff_url": null, "patch_url": null }
        //
        // GitHub API from v3 (include):
        // if issue comment is not done on pull request issue, than "pull_request" will be omitted from payload
        return pullRequestInfo != null && !StringUtils.isBlank((String) pullRequestInfo.get("html_url"));
    }

    /**
     * Loads remote pull request information.
     * 
     * @param repository
     *            in which repository
     * @param pullRequestNumber
     *            {@link PullRequest#getNumber()}
     * @return
     */
    private PullRequest getRemotePullRequest(Repository repository, int pullRequestNumber)
    {
        PullRequestService pullRequestService = githubClientProvider.getPullRequestService(repository);
        try
        {
            return pullRequestService.getPullRequest(RepositoryId.createFromUrl(repository.getRepositoryUrl()), pullRequestNumber);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getEventType()
    {
        return "IssueCommentEvent";
    }

}
