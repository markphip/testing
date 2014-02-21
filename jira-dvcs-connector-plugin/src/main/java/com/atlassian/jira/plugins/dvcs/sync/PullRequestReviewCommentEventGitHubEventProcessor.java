package com.atlassian.jira.plugins.dvcs.sync;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.event.PullRequestReviewCommentPayload;
import org.eclipse.egit.github.core.service.PullRequestService;

import com.atlassian.jira.plugins.dvcs.github.api.model.GitHubEvent;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventContext;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventProcessor;

/**
 * The {@link PullRequestReviewCommentPayload} implementation of the {@link GitHubEventProcessor}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class PullRequestReviewCommentEventGitHubEventProcessor implements GitHubEventProcessor
{

    /**
     * Injected {@link GithubClientProvider} dependency.
     */
    @Resource
    private GithubClientProvider githubClientProvider;

    /**
     * Extracts pull request number from pull request URL.<br>
     * e.g.: https://api.github.com/repos/stanislav-dvorscak/jira-dvcs-test/pulls/7 will be 7
     */
    private Pattern PULL_REQUEST_NUMBER_PATTERN = Pattern.compile("^.*/([0-9]*)$");

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(Repository repository, GitHubEvent gitHubEvent, boolean isSoftSync, String[] synchronizationTags,
            GitHubEventContext context)
    {
        @SuppressWarnings("unchecked")
        Map<String, Object> commentPayload = (Map<String, Object>) gitHubEvent.getPayload().get("comment");
        @SuppressWarnings("unchecked")
        Map<String, Object> links = (Map<String, Object>) commentPayload.get("_links");
        @SuppressWarnings("unchecked")
        Map<String, Object> pullRequestLink = (Map<String, Object>) links.get("pull_request");
        String pullRequestUrl = (String) pullRequestLink.get("href");

        Matcher pullRequestNumberMatcher = PULL_REQUEST_NUMBER_PATTERN.matcher(pullRequestUrl);
        Integer pullRequestNumber = pullRequestNumberMatcher.matches() ? Integer.parseInt(pullRequestNumberMatcher.group(1)) : null;
        if (pullRequestNumber == null)
        {
            throw new RuntimeException("Links payload (Repository: " + repository.getOrgName() + "/" + repository.getSlug()
                    + " GitHubEvent: " + gitHubEvent.getId() + " ) does not contains valid pull request url, pull_request: "
                    + pullRequestUrl);
        }

        PullRequestService pullRequestService = githubClientProvider.getPullRequestService(repository);

        PullRequest pullRequest;
        try
        {
            pullRequest = pullRequestNumber != null ? pullRequestService.getPullRequest(
                    RepositoryId.createFromUrl(repository.getRepositoryUrl()), pullRequestNumber) : null;
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        if (pullRequest == null)
        {
            throw new RuntimeException("Pull request does not exist, repository: " + repository.getOrgName() + "/" + repository.getName()
                    + " pull request number: " + pullRequestNumber);
        }

        context.savePullRequest(pullRequest.getId(), pullRequest.getNumber());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getEventType()
    {
        return "PullRequestReviewCommentEvent";
    }

}
