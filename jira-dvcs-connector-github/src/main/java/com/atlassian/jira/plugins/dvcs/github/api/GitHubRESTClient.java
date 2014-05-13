package com.atlassian.jira.plugins.dvcs.github.api;

import com.atlassian.jira.plugins.dvcs.github.api.model.GitHubEvent;
import com.atlassian.jira.plugins.dvcs.github.api.model.GitHubPage;
import com.atlassian.jira.plugins.dvcs.github.api.model.GitHubRepositoryHook;
import com.atlassian.jira.plugins.dvcs.model.Repository;

import java.util.List;

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
    List<GitHubRepositoryHook> getHooks(Repository repository);

    /**
     * Returns events which happened on provided repository.
     * 
     * @param repository
     *            for which repository
     * @return first page
     * @see #getNextPage(Class, GitHubPage)
     */
    GitHubPage<GitHubEvent> getEvents(Repository repository);

    /**
     * Loads next page for current loaded page.
     * 
     * @param arrayElementType
     *            type of page elements
     * @param currentPage
     *            cursor
     * @return next page
     */
    <E> GitHubPage<E> getNextPage(Class<E[]> arrayElementType, GitHubPage<E> currentPage);

}
