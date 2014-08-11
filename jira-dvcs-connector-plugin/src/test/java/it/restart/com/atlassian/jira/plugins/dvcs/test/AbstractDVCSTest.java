package it.restart.com.atlassian.jira.plugins.dvcs.test;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.pageobjects.pages.JiraLoginPage;
import com.atlassian.jira.plugins.dvcs.model.dev.RestDevResponse;
import com.atlassian.jira.plugins.dvcs.model.dev.RestPrRepository;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.OAuthCredentials;
import com.atlassian.jira.plugins.dvcs.remoterestpoint.PullRequestLocalRestpoint;
import com.atlassian.jira.rest.api.issue.IssueCreateResponse;
import com.atlassian.jira.testkit.client.Backdoor;
import com.atlassian.jira.testkit.client.restclient.Issue;
import com.atlassian.jira.testkit.client.restclient.SearchRequest;
import com.atlassian.jira.testkit.client.restclient.SearchResult;
import com.atlassian.jira.testkit.client.util.TestKitLocalEnvironmentData;
import com.atlassian.pageobjects.TestedProductFactory;
import it.restart.com.atlassian.jira.plugins.dvcs.JiraLoginPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.common.OAuth;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

/**
 * Abstract test for all DVCS tests.
 *
 * @author Stanislav Dvorscak
 */
public abstract class AbstractDVCSTest
{

    /**
     * @see #getJiraTestedProduct()
     */
    private JiraTestedProduct jiraTestedProduct;

    private Collection<String> createdIssueKeys = new ArrayList<String>();

    protected OAuth oAuth;

    protected Backdoor testKit;

    /**
     * Prepares common test environment.
     */
    @BeforeClass()
    public void onTestsEnvironmentSetup()
    {
        testKit = new Backdoor(new TestKitLocalEnvironmentData(new Properties(), "."));
        jiraTestedProduct = TestedProductFactory.create(JiraTestedProduct.class);
        new JiraLoginPageController(jiraTestedProduct).login();
    }

    /**
     * Destroys test environment.
     */
    @AfterMethod (alwaysRun = true)
    public void onTestCleanUp()
    {
        for (String createdIssueKey : createdIssueKeys)
        {
            testKit.issues().deleteIssue(createdIssueKey, true);
        }
    }

    /**
     * @return JIRA, which is used for testing.
     */
    protected JiraTestedProduct getJiraTestedProduct()
    {
        return jiraTestedProduct;
    }

    /**
     * Adds test issue with provided informations.
     *
     * @param projectKey project key, to which will be assigned issue
     * @param issueSummary summary of created issue e.g.: name of test class, or something unique
     * @return key of created issue
     */
    protected String addTestIssue(String projectKey, String issueSummary)
    {
        deleteIssuesBySummary(issueSummary);
        return createTestIssue(projectKey, issueSummary);
    }

    /**
     * Creates test issue with provided information.
     *
     * @param projectKey project key, to which will be assigned issue
     * @param issueSummary summary of created issue e.g.: name of test class, or something unique
     * @return key of created issue
     */
    private String createTestIssue(String projectKey, String issueSummary)
    {
        // creates issue for testing
        final IssueCreateResponse issue = testKit.issues().createIssue(projectKey, issueSummary, JiraLoginPage.USER_ADMIN);

        String createdIssueKey = issue.key();
        createdIssueKeys.add(createdIssueKey);

        return createdIssueKey;
    }

    /**
     * Delete any issues whose summary is the same
     */
    private void deleteIssuesBySummary(String issueSummary)
    {
        final SearchResult search = testKit.search().getSearch(new SearchRequest().jql("summary ~ \"" + issueSummary + "\""));
        for (Issue issue : search.issues)
        {
            createdIssueKeys.remove(issue.key);
            testKit.issues().deleteIssue(issue.key, true);
        }
    }

    protected OAuthCredentials getOAuthCredentials()
    {
        return new OAuthCredentials(oAuth.key, oAuth.secret);
    }

    protected RestDevResponse<RestPrRepository> getPullRequestResponse(String issueKey)
    {
        PullRequestLocalRestpoint pullRequestLocalRest = new PullRequestLocalRestpoint();
        return pullRequestLocalRest.getPullRequest(issueKey);
    }
}
