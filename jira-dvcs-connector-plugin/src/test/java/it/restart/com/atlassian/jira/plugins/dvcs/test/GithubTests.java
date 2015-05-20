package it.restart.com.atlassian.jira.plugins.dvcs.test;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.pageobjects.JiraLoginPageController;
import com.atlassian.jira.plugins.dvcs.pageobjects.common.MagicVisitor;
import com.atlassian.jira.plugins.dvcs.pageobjects.common.OAuth;
import com.atlassian.jira.plugins.dvcs.pageobjects.component.BitBucketCommitEntry;
import com.atlassian.jira.plugins.dvcs.pageobjects.component.OrganizationDiv;
import com.atlassian.jira.plugins.dvcs.pageobjects.component.RepositoryDiv;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.JiraViewIssuePageController;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.OAuthCredentials;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.RepositoriesPageController;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.RepositoriesPageController.AccountType;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.Account;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.AccountRepository;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.AccountsPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.remoterestpoint.ChangesetLocalRestpoint;
import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import it.com.atlassian.jira.plugins.dvcs.DvcsWebDriverTestCase;
import it.restart.com.atlassian.jira.plugins.dvcs.github.GithubLoginPage;
import it.restart.com.atlassian.jira.plugins.dvcs.github.GithubOAuthApplicationPage;
import it.restart.com.atlassian.jira.plugins.dvcs.github.GithubOAuthPage;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

import static com.atlassian.pageobjects.elements.query.Poller.waitUntilTrue;
import static it.restart.com.atlassian.jira.plugins.dvcs.test.GithubTestHelper.GITHUB_API_URL;
import static it.restart.com.atlassian.jira.plugins.dvcs.test.GithubTestHelper.REPOSITORY_NAME;
import static it.util.TestAccounts.DVCS_CONNECTOR_TEST_ACCOUNT;
import static it.util.TestAccounts.JIRA_BB_CONNECTOR_ACCOUNT;
import static java.util.Arrays.asList;
import static org.fest.assertions.api.Assertions.assertThat;

public class GithubTests extends DvcsWebDriverTestCase implements BasicTests
{
    private static final JiraTestedProduct JIRA = TestedProductFactory.create(JiraTestedProduct.class);
    private static final List<String> BASE_REPOSITORY_NAMES = asList(
            "missingcommits",
            "repo1",
            "noauthor",
            "test-project");

    private OAuth oAuth;

    @BeforeClass
    public void beforeClass()
    {
        JIRA.backdoor().restoreDataFromResource(TEST_DATA);
        // log in to JIRA
        new JiraLoginPageController(JIRA).login();
        // log in to github
        new MagicVisitor(JIRA).visit(GithubLoginPage.class).doLogin();
        // setup up OAuth from github
        oAuth = new MagicVisitor(JIRA).visit(GithubOAuthPage.class).addConsumer(JIRA.getProductInstance().getBaseUrl());
        JIRA.backdoor().plugins().disablePlugin("com.atlassian.jira.plugins.jira-development-integration-plugin");
    }

    @AfterClass
    public void afterClass()
    {
        // delete all organizations
        RepositoriesPageController rpc = new RepositoriesPageController(JIRA);
        rpc.getPage().deleteAllOrganizations();
        // remove OAuth in github
        new MagicVisitor(JIRA).visit(GithubOAuthApplicationPage.class).removeConsumer(oAuth);

        // log out from github
        new MagicVisitor(JIRA).visit(GithubLoginPage.class).doLogout();
    }

    @BeforeMethod
    public void beforeMethod()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(JIRA);
        rpc.getPage().deleteAllOrganizations();
    }

    @Override
    @Test
    public void addOrganization()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(JIRA);
        OrganizationDiv organization = rpc.addOrganization(AccountType.GITHUB, JIRA_BB_CONNECTOR_ACCOUNT,
                getOAuthCredentials(), false);

        assertThat(organization).isNotNull();
        assertThat(organization.getRepositoryNames()).containsAll(BASE_REPOSITORY_NAMES);
    }

    @Override
    @Test
    public void addOrganizationWaitForSync()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(JIRA);
        OrganizationDiv organization = rpc.addOrganization(AccountType.GITHUB, JIRA_BB_CONNECTOR_ACCOUNT,
                getOAuthCredentials(), false);

        assertThat(organization).isNotNull();
        assertThat(organization.getRepositoryNames()).containsAll(BASE_REPOSITORY_NAMES);

        final String expectedMessage = "Mon Feb 06 2012";
        final RepositoryDiv repositoryDiv = organization.findRepository(REPOSITORY_NAME);
        assertThat(repositoryDiv).isNotNull();
        repositoryDiv.enableSync();
        repositoryDiv.sync();
        assertThat(repositoryDiv.getMessage()).isEqualTo(expectedMessage);

        ChangesetLocalRestpoint changesetLocalRestpoint = new ChangesetLocalRestpoint();
        List<String> commitsForQA2 = changesetLocalRestpoint.getCommitMessages("QA-2", 6);
        assertThat(commitsForQA2).contains("BB modified 1 file to QA-2 and QA-3 from TestRepo-QA");
        List<String> commitsForQA3 = changesetLocalRestpoint.getCommitMessages("QA-3", 1);
        assertThat(commitsForQA3).contains("BB modified 1 file to QA-2 and QA-3 from TestRepo-QA");
    }

    @Override
    @Test (expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = ".*Error!\\nThe url \\[https://nonexisting.org\\] is incorrect or the server is not responding.*")
    public void addOrganizationInvalidUrl()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(JIRA);
        rpc.addOrganization(AccountType.GITHUB, "https://nonexisting.org/someaccount", getOAuthCredentials(), false, true);
    }

    @Override
    @Test (expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = ".*Error!\\nInvalid user/team account.*")
    public void addOrganizationInvalidAccount()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(JIRA);
        rpc.addOrganization(AccountType.GITHUB, "I_AM_SURE_THIS_ACCOUNT_IS_INVALID", getOAuthCredentials(), false, true);
    }


    @Override
    @Test (expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = "Invalid OAuth")
    public void addOrganizationInvalidOAuth()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(JIRA);
        rpc.addOrganization(AccountType.GITHUB, JIRA_BB_CONNECTOR_ACCOUNT, new OAuthCredentials("xxx", "yyy"), true, true);
    }

    @Test
    @Override
    public void testPostCommitHookAddedAndRemoved()
    {
        testPostCommitHookAddedAndRemoved(JIRA_BB_CONNECTOR_ACCOUNT, AccountType.GITHUB, REPOSITORY_NAME, JIRA, getOAuthCredentials());
    }

    @Override
    protected boolean postCommitHookExists(final String accountName, final String jiraCallbackUrl)
    {
        List<String> actualHookUrls = GithubTestHelper.getHookUrls(accountName, GITHUB_API_URL, REPOSITORY_NAME);
        return actualHookUrls.contains(jiraCallbackUrl);
    }

    @Test
    @Override
    public void testCommitStatistics()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(JIRA);
        final OrganizationDiv organization = rpc.addOrganization(AccountType.GITHUB, JIRA_BB_CONNECTOR_ACCOUNT,
                getOAuthCredentials(), false);
        final RepositoryDiv repositoryDiv = organization.findRepository("test-project");
        repositoryDiv.enableSync();
        repositoryDiv.sync();

        // QA-2
        List<BitBucketCommitEntry> commitMessages = new JiraViewIssuePageController(JIRA, "QA-3").getCommits(1); // throws AssertionError with other than 1 message
        assertThat(commitMessages).hasSize(1);

        BitBucketCommitEntry commitMessage = commitMessages.get(0);
        List<PageElement> statistics = commitMessage.getStatistics();
        assertThat(statistics).hasSize(1);
        assertThat(commitMessage.getAdditions(statistics.get(0))).isEqualTo("+1");
        assertThat(commitMessage.getDeletions(statistics.get(0))).isEqualTo("-");

        // QA-4
        commitMessages = new JiraViewIssuePageController(JIRA, "QA-4").getCommits(1); // throws AssertionError with other than 1 message
        assertThat(commitMessages).hasSize(1);

        commitMessage = commitMessages.get(0);
        statistics = commitMessage.getStatistics();
        assertThat(statistics).hasSize(1);
        assertThat(commitMessage.isAdded(statistics.get(0))).isTrue();
    }

    @Override
    @Test
    public void shouldBeAbleToSeePrivateRepositoriesFromTeamAccount()
    {
        // we should see 'private-dvcs-connector-test' repo
        RepositoriesPageController rpc = new RepositoriesPageController(JIRA);
        OrganizationDiv organization = rpc.addOrganization(AccountType.GITHUB, "atlassian",
                getOAuthCredentials(), false);

        assertThat(organization.containsRepository("private-dvcs-connector-test"));
    }

    @Test
    public void linkingRepositoryWithoutAdminPermission()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(JIRA);
        rpc.addOrganization(AccountType.GITHUB, DVCS_CONNECTOR_TEST_ACCOUNT, getOAuthCredentials(), false);

        AccountsPage accountsPage = JIRA.visit(AccountsPage.class);
        Account account = accountsPage.getAccount(Account.AccountType.GIT_HUB, DVCS_CONNECTOR_TEST_ACCOUNT);
        AccountRepository repository = account.enableRepository("testemptyrepo", true);

        // check that repository is enabled
        waitUntilTrue(repository.isEnabled());
        waitUntilTrue(repository.hasWarning());
    }

    @Test
    public void linkingRepositoryWithAdminPermission()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(JIRA);
        rpc.addOrganization(AccountType.GITHUB, JIRA_BB_CONNECTOR_ACCOUNT, getOAuthCredentials(), false);

        AccountsPage accountsPage = JIRA.visit(AccountsPage.class);
        Account account = accountsPage.getAccount(Account.AccountType.GIT_HUB, JIRA_BB_CONNECTOR_ACCOUNT);
        AccountRepository repository = account.enableRepository(REPOSITORY_NAME, false);

        // check that repository is enabled
        waitUntilTrue(repository.isEnabled());
        waitUntilTrue(repository.hasWarning());
    }

    @Test
    public void autoLinkingRepositoryWithoutAdminPermission()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(JIRA);
        final OrganizationDiv organization = rpc.addOrganization(AccountType.GITHUB, DVCS_CONNECTOR_TEST_ACCOUNT,
                getOAuthCredentials(), false);
        organization.enableAllRepos();

        AccountsPage accountsPage = JIRA.visit(AccountsPage.class);
        Account account = accountsPage.getAccount(Account.AccountType.GIT_HUB, DVCS_CONNECTOR_TEST_ACCOUNT);

        for (AccountRepository repository : account.getRepositories())
        {
            waitUntilTrue(repository.isEnabled());
        }
    }

    @Test
    public void autoLinkingRepositoryWithAdminPermission()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(JIRA);
        final OrganizationDiv organization = rpc.addOrganization(AccountType.GITHUB, JIRA_BB_CONNECTOR_ACCOUNT,
                getOAuthCredentials(), false);
        organization.enableAllRepos();

        AccountsPage accountsPage = JIRA.visit(AccountsPage.class);
        Account account = accountsPage.getAccount(Account.AccountType.GIT_HUB, JIRA_BB_CONNECTOR_ACCOUNT);

        for (AccountRepository repository : account.getRepositories())
        {
            waitUntilTrue(repository.isEnabled());
            Poller.waitUntilFalse(repository.hasWarning());
        }
    }

    //-------------------------------------------------------------------
    //--------- these methods should go to some common utility/class ----
    //-------------------------------------------------------------------

    private OAuthCredentials getOAuthCredentials()
    {
        return new OAuthCredentials(oAuth.key, oAuth.secret);
    }
}
