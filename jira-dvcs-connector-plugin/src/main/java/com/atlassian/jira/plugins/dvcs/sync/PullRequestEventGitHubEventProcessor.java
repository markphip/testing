package com.atlassian.jira.plugins.dvcs.sync;

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
        Long pullRequestId = (Long) gitHubEvent.getPayload().get("id");
        Integer pullRequestNumber = (Integer) gitHubEvent.getPayload().get("number");
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
