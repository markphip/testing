package com.atlassian.jira.plugins.dvcs.github.api;

import java.net.URI;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.atlassian.jira.plugins.dvcs.github.api.model.GitHubPage;

/**
 * Unit tests for {@link GitHubPageIterable}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubPageIterableTest
{

    /**
     * Tested object.
     */
    private GitHubPageIterable<Integer> testedObject;

    /**
     * Mock for {@link GitHubRESTClient} - it is used by iterator for next page retrieving.
     */
    @Mock
    private GitHubRESTClient gitHubRESTClient;

    /**
     * Mock for initial first page.
     */
    @Mock
    private GitHubPage<Integer> firstGitHubPage;

    /**
     * Prepares environment for each test method.
     */
    @BeforeMethod
    public void beforeTestMethod()
    {
        MockitoAnnotations.initMocks(this);

        testedObject = new GitHubPageIterable<Integer>(Integer.class, gitHubRESTClient, firstGitHubPage);
    }

    /**
     * Unit test for {@link GitHubPageIterable#iterator()}.
     * 
     * @throws Exception
     */
    @Test
    public void test() throws Exception
    {
        URI secondGitHubPageURI = new URI("uri_2");
        URI thirdGitHubPageURI = new URI("uri_3");

        Mockito.when(firstGitHubPage.getValues()).thenReturn(new Integer[] { 1, 2, 3 });
        Mockito.when(firstGitHubPage.getNextPage()).thenReturn(secondGitHubPageURI);

        @SuppressWarnings("unchecked")
        GitHubPage<Integer> secondGitHubPage = Mockito.mock(GitHubPage.class);
        Mockito.when(secondGitHubPage.getValues()).thenReturn(new Integer[] { 4, 5, 6 });
        Mockito.when(secondGitHubPage.getNextPage()).thenReturn(thirdGitHubPageURI);
        Mockito.when(gitHubRESTClient.getNextPage(Integer[].class, firstGitHubPage)).thenReturn(secondGitHubPage);

        @SuppressWarnings("unchecked")
        GitHubPage<Integer> lastGitHubPage = Mockito.mock(GitHubPage.class);
        // yes, last page is empty, at least in several cases occurred that last page of GitHub paging is empty page.
        Mockito.when(lastGitHubPage.getValues()).thenReturn(new Integer[] {});
        Mockito.when(lastGitHubPage.getNextPage()).thenReturn(null);
        Mockito.when(gitHubRESTClient.getNextPage(Integer[].class, secondGitHubPage)).thenReturn(lastGitHubPage);

        int sum = 0;
        for (Integer value : testedObject)
        {
            sum += value;
        }
        Assert.assertEquals(sum, 21);
    }
}
