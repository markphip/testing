package com.atlassian.jira.plugins.dvcs.github.api;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Iterator;

import com.atlassian.jira.plugins.dvcs.github.api.model.GitHubPage;

public class GitHubPageIterable<T> implements Iterable<T>
{

    private GitHubRESTClient gitHubRESTClient;
    private GitHubPage<T> firstPage;

    /**
     * Constructor.
     * 
     * @param gitHubRESTClient
     * @param firstPage
     */
    public GitHubPageIterable(GitHubRESTClient gitHubRESTClient, GitHubPage<T> firstPage)
    {
        this.gitHubRESTClient = gitHubRESTClient;
        this.firstPage = firstPage;
    }

    private static final class GitHubPageIterator<T> implements Iterator<T>
    {

        private final GitHubRESTClient gitHubRESTClient;

        private GitHubPage<T> currentPage;
        private Iterator<T> currentPageIterator;

        public GitHubPageIterator(GitHubRESTClient gitHubRESTClient, GitHubPage<T> firstPage)
        {
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
                currentPage = gitHubRESTClient.getNextPage((Class<T[]>) Array.newInstance(getType(), 0).getClass(), currentPage);
                currentPageIterator = Arrays.asList(currentPage.getValues()).iterator();
            }
            return result;
        }

        @SuppressWarnings("unchecked")
        private Class<T> getType()
        {
            return (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
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
        return new GitHubPageIterator<T>(gitHubRESTClient, firstPage);
    }

}
