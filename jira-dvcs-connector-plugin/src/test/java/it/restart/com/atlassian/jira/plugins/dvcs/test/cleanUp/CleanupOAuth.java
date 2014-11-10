package it.restart.com.atlassian.jira.plugins.dvcs.test.cleanUp;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.pageobjects.common.MagicVisitor;
import com.atlassian.pageobjects.TestedProductFactory;
import it.restart.com.atlassian.jira.plugins.dvcs.github.GithubLoginPage;
import it.restart.com.atlassian.jira.plugins.dvcs.github.GithubOAuthApplicationPage;

/**
 * 'Test' that just goes in and deletes OAuth tokens. Initial version is only for GH but can be expanded to run on BB
 * and GHE as well. It is a little flaky as it relies on GH redirecting back to itself in
 * #GithubOAuthApplicationPage.removeAllTestApplications() which doesn't always happen depending on the config but it is
 * good enough to use as a cleanup script
 */
public class CleanupOAuth
{
    public static void main(String[] args)
    {
        cleanupGitHubOAuth();
    }

    public static void cleanupGitHubOAuth()
    {
        JiraTestedProduct jira = TestedProductFactory.create(JiraTestedProduct.class);
        new MagicVisitor(jira).visit(GithubLoginPage.class).doLogin();
        GithubOAuthApplicationPage ghOAuthPage = new MagicVisitor(jira).visit(GithubOAuthApplicationPage.class);
        ghOAuthPage.removeAllTestApplications();
    }
}
