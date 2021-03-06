package it.com.atlassian.jira.plugins.dvcs;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.pageobjects.JiraLoginPageController;
import com.atlassian.jira.plugins.dvcs.pageobjects.component.BitBucketCommitEntry;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BaseConfigureOrganizationsPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.JiraViewIssuePage;
import com.atlassian.pageobjects.TestedProductFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.util.List;

/**
 * Base class for BitBucket integration tests. Initializes the JiraTestedProduct and logs admin in.
 */
public abstract class BaseOrganizationTest<T extends BaseConfigureOrganizationsPage> extends DvcsWebDriverTestCase
{
    protected static final JiraTestedProduct JIRA = TestedProductFactory.create(JiraTestedProduct.class);
    protected T configureOrganizations;

    @BeforeMethod
    public void loginToJira()
    {
        // log in to JIRA
        new JiraLoginPageController(JIRA).login(getConfigureOrganizationsPageClass());
        configureOrganizations = JIRA.getPageBinder().navigateToAndBind(getConfigureOrganizationsPageClass());
        configureOrganizations.setJiraTestedProduct(JIRA);
        configureOrganizations.deleteAllOrganizations();
    }

    protected abstract Class<T> getConfigureOrganizationsPageClass();

    @AfterMethod
    public void logout()
    {
        JIRA.getTester().getDriver().manage().deleteAllCookies();
    }


    protected List<BitBucketCommitEntry> getCommitsForIssue(String issueKey, int exectedNumberOfCommits)
    {
        return getCommitsForIssue(issueKey, exectedNumberOfCommits, 1000L, 5);
    }

    protected List<BitBucketCommitEntry> getCommitsForIssue(String issueKey, int exectedNumberOfCommits,
            long retryThreshold, int maxRetryCount)
    {
        return JIRA.visit(JiraViewIssuePage.class, issueKey)
                .openBitBucketPanel()
                .waitForNumberOfMessages(exectedNumberOfCommits, retryThreshold, maxRetryCount);
    }

    protected BaseConfigureOrganizationsPage goToConfigPage()
    {
        configureOrganizations = JIRA.visit(getConfigureOrganizationsPageClass());
        configureOrganizations.setJiraTestedProduct(JIRA);
        return configureOrganizations;
    }
}
