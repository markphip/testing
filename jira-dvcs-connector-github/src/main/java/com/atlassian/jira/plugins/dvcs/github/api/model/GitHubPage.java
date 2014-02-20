package com.atlassian.jira.plugins.dvcs.github.api.model;

import java.net.URI;

/**
 * Page returned from paging.
 * 
 * @author Stanislav Dvorscak
 * 
 * @param <T>
 *            type of page element
 */
public class GitHubPage<T>
{

    /**
     * @see #getNextPage()
     */
    private final URI nextPage;

    /**
     * @see #getValues()
     */
    private final T[] page;

    /**
     * Constructor.
     * 
     * @param page
     *            {@link #getValues()}
     * @param nextPage
     *            {@link #getNextPage()}
     */
    public GitHubPage(T[] page, URI nextPage)
    {
        this.page = page;
        this.nextPage = nextPage;
    }

    /**
     * @return URI to get next page
     */
    public URI getNextPage()
    {
        return nextPage;
    }

    /**
     * @return current loaded page
     */
    public T[] getValues()
    {
        return page;
    }

}
