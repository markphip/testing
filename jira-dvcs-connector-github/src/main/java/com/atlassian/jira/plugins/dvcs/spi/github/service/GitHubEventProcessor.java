package com.atlassian.jira.plugins.dvcs.spi.github.service;

import com.atlassian.jira.plugins.dvcs.github.api.model.GitHubEvent;
import com.atlassian.jira.plugins.dvcs.model.Repository;

/**
 * Defines the contract for the GitHub event processing.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public interface GitHubEventProcessor
{

    /**
     * Processes incoming event.
     * 
     * @param repository
     *            current proceed repository
     * @param event
     *            to process
     * @param isSoftSync
     *            is soft synchronization?
     * @param synchronizationTags
     *            tags of current synchronization
     * @param context
     *            context for GitHub event synchronization
     */
    void process(Repository repository, GitHubEvent event, boolean isSoftSync, String[] synchronizationTags, GitHubEventContext context);

    /**
     * @return The type of the payload which is supported by this processor.
     */
    String getEventType();

}
