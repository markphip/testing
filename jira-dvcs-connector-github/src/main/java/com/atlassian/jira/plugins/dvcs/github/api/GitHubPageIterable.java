package com.atlassian.jira.plugins.dvcs.github.api;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;

import com.atlassian.jira.plugins.dvcs.github.api.model.GitHubPage;

public class GitHubPageIterable<T> implements Iterable<T>
{

    private Class<T> type;
    private GitHubRESTClient gitHubRESTClient;
    private GitHubPage<T> firstPage;

    /**
     * Constructor.
     * 
     * @param gitHubRESTClient
     * @param firstPage
     */
    public GitHubPageIterable(Class<T> type, GitHubRESTClient gitHubRESTClient, GitHubPage<T> firstPage)
    {
        this.type = type;
        this.gitHubRESTClient = gitHubRESTClient;
        this.firstPage = firstPage;
    }

    private static final class GitHubPageIterator<T> implements Iterator<T>
    {

        private final GitHubRESTClient gitHubRESTClient;

        private Class<T> type;
        private GitHubPage<T> currentPage;
        private Iterator<T> currentPageIterator;

        public GitHubPageIterator(Class<T> type, GitHubRESTClient gitHubRESTClient, GitHubPage<T> firstPage)
        {
            this.type = type;
            this.gitHubRESTClient = gitHubRESTClient;
            currentPage = firstPage;
            currentPageIterator = Arrays.asList(currentPage.getValues()).iterator();
        }

        @Override
        public boolean hasNext()
        {
            return currentPageIterator.hasNext();
        }

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
