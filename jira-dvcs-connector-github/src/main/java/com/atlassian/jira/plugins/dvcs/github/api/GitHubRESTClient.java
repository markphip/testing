package com.atlassian.jira.plugins.dvcs.github.api;

import com.atlassian.jira.plugins.dvcs.github.api.model.GitHubEvent;
import com.atlassian.jira.plugins.dvcs.github.api.model.GitHubRepositoryHook;
import com.atlassian.jira.plugins.dvcs.model.Repository;

/**
 * API abstraction over GitHub REST API.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public interface GitHubRESTClient
{

    /**
     * @param repository
     *            on which repository
     * @param hook
     *            for creation
     * @return created hook
     */
    GitHubRepositoryHook addHook(Repository repository, GitHubRepositoryHook hook);

    /**
     * @param repository
     *            on which repository
     * @param hook
     *            for deletion
     */
    void deleteHook(Repository repository, GitHubRepositoryHook hook);

    /**
     * @param repository
     *            for which repository
     * @return returns hooks for provided repository.
     */
    GitHubRepositoryHook[] getHooks(Repository repository);

    /**
     * Returns events which happened on provided repository.
     * 
     * @param repository
     * @param page
     * @param rowsPerPage
     * @return
     */
    GitHubEvent[] getEvents(Repository repository, int page, int rowsPerPage);

}
