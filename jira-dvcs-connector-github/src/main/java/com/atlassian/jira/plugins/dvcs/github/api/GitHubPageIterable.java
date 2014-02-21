package com.atlassian.jira.plugins.dvcs.github.api;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;

import com.atlassian.jira.plugins.dvcs.github.api.model.GitHubPage;

/**
 * Utility, which iterates over all {@link GitHubPage}-s.
 * 
 * @author Stanislav Dvorscak
 * 
 * @param <T>
 *            type of entities inside page
 */
public class GitHubPageIterable<T> implements Iterable<T>
{

    /**
     * Class type of entities inside a page.
     */
    private Class<T> type;

    /**
     * Used for loading next pages.
     */
    private GitHubRESTClient gitHubRESTClient;

    /**
     * First page, which was already loaded.
     */
    private GitHubPage<T> firstPage;

    /**
     * Constructor.
     * 
     * @param type
     *            class type of entities inside a page.
     * @param gitHubRESTClient
     *            used for loading next pages.
     * @param firstPage
     *            first page, which was already loaded.
     */
    public GitHubPageIterable(Class<T> type, GitHubRESTClient gitHubRESTClient, GitHubPage<T> firstPage)
    {
        this.type = type;
        this.gitHubRESTClient = gitHubRESTClient;
        this.firstPage = firstPage;
    }

    /**
     * Iterates over all result pages.
     * 
     * @author Stanislav Dvorscak
     * 
     * @param <T>
     *            type of entities inside page
     */
    private static final class GitHubPageIterator<T> implements Iterator<T>
    {

        /**
         * Used for loading next pages.
         */
        private final GitHubRESTClient gitHubRESTClient;

        /**
         * Class type of entities inside a page.
         */
        private Class<T> type;

        /**
         * Current loaded page.
         */
        private GitHubPage<T> currentPage;

        /**
         * Iterator build up over {@link #currentPage}.
         */
        private Iterator<T> currentPageIterator;

        public GitHubPageIterator(Class<T> type, GitHubRESTClient gitHubRESTClient, GitHubPage<T> firstPage)
        {
            this.type = type;
            this.gitHubRESTClient = gitHubRESTClient;
            currentPage = firstPage;
            currentPageIterator = Arrays.asList(currentPage.getValues()).iterator();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext()
        {
            return currentPageIterator.hasNext();
        }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        @Override
        public T next()
        {
            T result = currentPageIterator.next();
            if (!currentPageIterator.hasNext() && currentPage.getNextPage() != null)
            {
                currentPage = gitHubRESTClient.getNextPage((Class<T[]>) Array.newInstance(type, 0).getClass(), currentPage);
                currentPageIterator = Arrays.asList(currentPage.getValues()).iterator();
            }
            return result;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<T> iterator()
    {
        return new GitHubPageIterator<T>(type, gitHubRESTClient, firstPage);
    }

}
