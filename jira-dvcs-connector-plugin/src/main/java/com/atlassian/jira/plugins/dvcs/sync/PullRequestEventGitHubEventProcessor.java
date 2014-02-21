package com.atlassian.jira.plugins.dvcs.sync;

import java.util.Map;

import org.eclipse.egit.github.core.event.PullRequestPayload;

import com.atlassian.jira.plugins.dvcs.github.api.model.GitHubEvent;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventContext;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventProcessor;

/**
 * Processors responsible for processing events, which are about {@link PullRequestPayload}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class PullRequestEventGitHubEventProcessor implements GitHubEventProcessor
{
    /**
     * {@inheritDoc}
     */
    @Override
    public void process(Repository repository, GitHubEvent gitHubEvent, boolean isSoftSync, String[] synchronizationTags,
            GitHubEventContext context)
    {
        @SuppressWarnings("unchecked")
        Map<String, Object> pullRequestPayload = (Map<String, Object>) gitHubEvent.getPayload().get("pull_request");
        long pullRequestId = ((Number) pullRequestPayload.get("id")).longValue();
        Integer pullRequestNumber = (Integer) pullRequestPayload.get("number");
        context.savePullRequest(pullRequestId, pullRequestNumber);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getEventType()
    {
        return "PullRequestEvent";
    }

}
