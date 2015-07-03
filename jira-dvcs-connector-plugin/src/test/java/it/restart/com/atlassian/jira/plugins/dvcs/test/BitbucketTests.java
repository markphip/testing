package it.restart.com.atlassian.jira.plugins.dvcs.test;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.pageobjects.model.DefaultIssueActions;
import com.atlassian.jira.pageobjects.pages.viewissue.IssueMenu;
import com.atlassian.jira.pageobjects.pages.viewissue.MoveIssuePage;
import com.atlassian.jira.pageobjects.pages.viewissue.ViewIssuePage;
import com.atlassian.jira.plugins.dvcs.pageobjects.JiraLoginPageController;
import com.atlassian.jira.plugins.dvcs.pageobjects.common.BitbucketTestedProduct;
import com.atlassian.jira.plugins.dvcs.pageobjects.common.OAuth;
import com.atlassian.jira.plugins.dvcs.pageobjects.component.BitBucketCommitEntry;
import com.atlassian.jira.plugins.dvcs.pageobjects.component.OrganizationDiv;
import com.atlassian.jira.plugins.dvcs.pageobjects.component.RepositoryDiv;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitbucketOAuthPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.JiraViewIssuePage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.OAuthCredentials;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.RepositoriesPageController;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.RepositoriesPageController.AccountType;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.Account;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.AccountRepository;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.DvcsAccountsPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.remoterestpoint.ChangesetLocalRestpoint;
import com.atlassian.jira.plugins.dvcs.util.HttpSenderUtils;
import com.atlassian.jira.plugins.dvcs.util.PasswordUtil;
import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.webdriver.testing.rule.WebDriverSupport;
import it.com.atlassian.jira.plugins.dvcs.DvcsWebDriverTestCase;
import it.restart.com.atlassian.jira.plugins.dvcs.DashboardActivityStreamsPage;
import it.restart.com.atlassian.jira.plugins.dvcs.GreenHopperBoardPage;
import it.restart.com.atlassian.jira.plugins.dvcs.JiraAddUserPage;
import org.apache.commons.httpclient.methods.GetMethod;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static com.atlassian.jira.permission.ProjectPermissions.BROWSE_PROJECTS;
import static com.atlassian.jira.plugins.dvcs.pageobjects.BitBucketCommitEntriesAssert.assertThat;
import static com.atlassian.pageobjects.elements.query.Poller.waitUntilFalse;
import static com.atlassian.pageobjects.elements.query.Poller.waitUntilTrue;
import static it.restart.com.atlassian.jira.plugins.dvcs.test.IntegrationTestUserDetails.ACCOUNT_NAME;
import static it.util.TestAccounts.DVCS_CONNECTOR_TEST_ACCOUNT;
import static it.util.TestAccounts.JIRA_BB_CONNECTOR_ACCOUNT;
import static org.fest.assertions.api.Assertions.assertThat;

public class BitbucketTests extends DvcsWebDriverTestCase implements BasicTests, ActivityStreamsTest
{
    private static final JiraTestedProduct JIRA = TestedProductFactory.create(JiraTestedProduct.class);
    private static final BitbucketTestedProduct BIT_BUCKET = new BitbucketTestedProduct(JIRA.getTester());

    private OAuth oAuth;
    private static final List<String> BASE_REPOSITORY_NAMES = Arrays.asList("public-hg-repo", "private-hg-repo", "public-git-repo", "private-git-repo");
    private static final String GADGET_ID = "gadget-10001";

    @BeforeClass
    public void beforeClass()
    {
        new JiraLoginPageController(JIRA).login();

        oAuth = BIT_BUCKET.loginAndGoTo(JIRA_BB_CONNECTOR_ACCOUNT, PasswordUtil.getPassword(JIRA_BB_CONNECTOR_ACCOUNT),
                BitbucketOAuthPage.class, JIRA_BB_CONNECTOR_ACCOUNT).addConsumer();

        JIRA.backdoor().plugins().disablePlugin("com.atlassian.jira.plugins.jira-development-integration-plugin");
    }

    @AfterClass
    public void afterClass()
    {
        // delete all organizations
        RepositoriesPageController rpc = new RepositoriesPageController(JIRA);
        rpc.getPage().deleteAllOrganizations();

        BIT_BUCKET.visit(BitbucketOAuthPage.class, JIRA_BB_CONNECTOR_ACCOUNT).removeConsumer(oAuth.applicationId);
        BIT_BUCKET.logout();
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
        OrganizationDiv organization = addOrganization(AccountType.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), false);

        assertThat(organization).isNotNull();
        assertThat(organization.getRepositoryNames()).containsAll(BASE_REPOSITORY_NAMES);

        // check add user extension
        PageElement dvcsExtensionsPanel = JIRA.visit(JiraAddUserPage.class).getDvcsExtensionsPanel();
        assertThat(dvcsExtensionsPanel.isVisible());
    }

    @Override
    @Test
    public void addOrganizationWaitForSync()
    {
        OrganizationDiv organization = addOrganization(AccountType.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), false);

        assertThat(organization).isNotNull();
        assertThat(organization.getRepositoryNames()).containsAll(BASE_REPOSITORY_NAMES);
        final String repositoryName = "public-hg-repo";
        final String expectedMessage = "Fri Mar 02 2012";

        RepositoryDiv repositoryDiv = organization.findRepository(repositoryName);
        repositoryDiv.enableSync();
        repositoryDiv.sync();

        assertThat(repositoryDiv).isNotNull();
        assertThat(repositoryDiv.getMessage()).isEqualTo(expectedMessage);

        assertThat(getCommitsForIssue("QA-2", 1)).hasItemWithCommitMessage("BB modified 1 file to QA-2 and QA-3 from TestRepo-QA");
        assertThat(getCommitsForIssue("QA-3", 2)).hasItemWithCommitMessage("BB modified 1 file to QA-2 and QA-3 from TestRepo-QA");
    }

    @Override
    @Test (expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = ".*Error!\\nThe authentication with Bitbucket has failed. Please check your OAuth settings.*")
    public void addOrganizationInvalidOAuth()
    {
        addOrganization(AccountType.BITBUCKET, ACCOUNT_NAME, new OAuthCredentials("bad", "credentials"), false, true);
    }

    @Test
    @Override
    public void testCommitStatistics()
    {
        addOrganization(AccountType.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), false);

        enableRepositoryAsAdmin("public-hg-repo").synchronize();

        // QA-2
        List<BitBucketCommitEntry> commitMessages = getCommitsForIssue("QA-2", 1); // throws AssertionError with other than 1 message

        BitBucketCommitEntry commitMessage = commitMessages.get(0);
        List<PageElement> statistics = commitMessage.getStatistics();
        assertThat(statistics).hasSize(1);
        assertThat(commitMessage.isAdded(statistics.get(0))).isTrue();

        // QA-3
        commitMessages = getCommitsForIssue("QA-3", 2); // throws AssertionError with other than 2 messages

        // commit 1
        commitMessage = commitMessages.get(0);
        statistics = commitMessage.getStatistics();
        assertThat(statistics).hasSize(1);
        assertThat(commitMessage.isAdded(statistics.get(0))).isTrue();
        // commit 2
        commitMessage = commitMessages.get(1);
        statistics = commitMessage.getStatistics();
        assertThat(statistics).hasSize(1);
        assertThat(commitMessage.getAdditions(statistics.get(0))).isEqualTo("+3");
        assertThat(commitMessage.getDeletions(statistics.get(0))).isEqualTo("-");
    }

    @Test
    @Override
    public void testPostCommitHookAddedAndRemoved()
    {
        testPostCommitHookAddedAndRemoved(JIRA_BB_CONNECTOR_ACCOUNT, AccountType.BITBUCKET, "public-hg-repo", JIRA, getOAuthCredentials());
    }

    @Override
    protected boolean postCommitHookExists(final String accountName, final String jiraCallbackUrl)
    {
        String bitbucketServiceConfigUrl = "https://bitbucket.org/!api/1.0/repositories/"+accountName+"/public-hg-repo/services";

        String postDeleteServicesConfig = HttpSenderUtils.makeHttpRequest(new GetMethod(bitbucketServiceConfigUrl),
                accountName, PasswordUtil.getPassword(accountName));
        return postDeleteServicesConfig.contains(jiraCallbackUrl);
    }

    @Test
    @Override
    public void testActivityPresentedForQA5()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(JIRA);
        rpc.addOrganization(AccountType.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), false);

        enableRepositoryAsAdmin("public-hg-repo").synchronize();

        DashboardActivityStreamsPage page = visitActivityStreamGadget(GADGET_ID, true);

        // Activity streams should contain at least one changeset with 'more files' link.
        assertThat(page.isMoreFilesLinkVisible()).isTrue();
        page.checkIssueActivityPresentedForQA5();

        page.setIssueKeyFilter("QA-4");
        // because commit contains both keys QA-4 and QA-5, so should be present on both issues' activity streams
        page.checkIssueActivityPresentedForQA5();

        page.setIssueKeyFilter("QA-5");
        page.checkIssueActivityPresentedForQA5();

        // delete repository
        rpc = new RepositoriesPageController(JIRA);
        rpc.getPage().deleteAllOrganizations();

        page = visitActivityStreamGadget(GADGET_ID, true);
        page.checkIssueActivityNotPresentedForQA5();
    }

    @Override
    @Test
    public void testAnonymousAccess()
    {
        setupAnonymousAccessAllowed();
        try
        {
            // add organization
            addOrganization(AccountType.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), false);

            enableRepositoryAsAdmin("public-hg-repo").synchronize();

            // Activity streams gadget expected at dashboard page!
            DashboardActivityStreamsPage page = visitActivityStreamGadget(GADGET_ID, false);

            page.checkIssueActivityPresentedForQA5();

            // logout user
            JIRA.getTester().getDriver().manage().deleteAllCookies();

            page = visitActivityStreamGadget(GADGET_ID, false);
            // anonymous user should not see QA-5 activity stream
            page.checkIssueActivityNotPresentedForQA5();
        }
        finally
        {
            // always clean up the anonymous setting change
            new JiraLoginPageController(JIRA).login();
            setupAnonymousAccessForbidden();
        }
    }

    @Test
    public void greenHopperIntegration_ShouldAddDvcsCommitsTab()
    {
        // add organization
        addOrganization(AccountType.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), false);

        enableRepositoryAsAdmin("public-hg-repo").synchronize();

        setSize(new Dimension(1024, 1280));

        GreenHopperBoardPage greenHopperBoardPage = JIRA.getPageBinder().navigateToAndBind(GreenHopperBoardPage.class);
        greenHopperBoardPage.goToQABoardPlan();
        greenHopperBoardPage.assertCommitsAppearOnIssue("QA-1", 5);
    }

    @Test
    public void moveIssue_ShouldKeepAlsoCommits()
    {
        addOrganization(AccountType.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), false);

        enableRepositoryAsAdmin("public-hg-repo").synchronize();

        final String issueKey = "QA-1";

        final String commitMessage = "QA-1 test modification";
        final int numberOfCommits = 5;

        ChangesetLocalRestpoint changesetLocalRestpoint = new ChangesetLocalRestpoint();
        List<String> originalCommitMessages = changesetLocalRestpoint.getCommitMessages(issueKey, numberOfCommits);
        assertThat(originalCommitMessages).contains(commitMessage);

        // move issue from QA project to BBC project
        moveIssueToProject(issueKey, "Bitbucket Connector");

        // check commits kept in fact, Jira will make the redirect to moved/created issue BBC-1
        List<String> movedCommitMessages = changesetLocalRestpoint.getCommitMessages(issueKey, numberOfCommits);
        assertThat(movedCommitMessages).contains(commitMessage);
    }

    @Override
    @Test
    public void linkingRepositoryWithoutAdminPermission()
    {
        addOrganization(AccountType.BITBUCKET, DVCS_CONNECTOR_TEST_ACCOUNT, getOAuthCredentials(), false);

        AccountRepository repository = enableRepository(DVCS_CONNECTOR_TEST_ACCOUNT, "testemptyrepo", true);

        // check that repository is enabled
        waitUntilTrue(repository.isEnabled());
        waitUntilTrue(repository.hasWarning());
    }

    @Override
    @Test
    public void linkingRepositoryWithAdminPermission()
    {
        addOrganization(AccountType.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), false);

        AccountRepository repository = enableRepositoryAsAdmin("private-git-repo");

        // check that repository is enabled
        waitUntilTrue(repository.isEnabled());
        waitUntilFalse(repository.hasWarning());
    }

    @Override
    @Test
    public void autoLinkingRepositoryWithoutAdminPermission()
    {
        OrganizationDiv organization = addOrganization(AccountType.BITBUCKET, DVCS_CONNECTOR_TEST_ACCOUNT, getOAuthCredentials(), false);
        organization.enableAllRepos();

        DvcsAccountsPage accountsPage = JIRA.visit(DvcsAccountsPage.class);
        Account account = accountsPage.getAccount(Account.AccountType.BITBUCKET, DVCS_CONNECTOR_TEST_ACCOUNT);

        for (AccountRepository repository : account.getRepositories())
        {
            waitUntilTrue(repository.isEnabled());
        }
    }

    @Override
    @Test
    public void autoLinkingRepositoryWithAdminPermission()
    {
        OrganizationDiv organization = addOrganization(AccountType.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), false);
        organization.enableAllRepos();

        DvcsAccountsPage accountsPage = JIRA.visit(DvcsAccountsPage.class);
        Account account = accountsPage.getAccount(Account.AccountType.BITBUCKET, ACCOUNT_NAME);
        for (AccountRepository repository : account.getRepositories())
        {
            waitUntilTrue(repository.isEnabled());
            waitUntilFalse(repository.hasWarning());
        }
    }

    @Test
    public void testFullSync()
    {
        addOrganization(AccountType.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), false);
        enableRepositoryAsAdmin("public-hg-repo").fullSynchronize();
    }

    //-------------------------------------------------------------------
    //--------- these methods should go to some common utility/class ----
    //-------------------------------------------------------------------

    // code copied from WindowSizeRule from atlassian-selenium,
    // will be reworked to have a proper TestNG implementation of the WindowSize annotation
    private void setSize(Dimension dimension)
    {
        final WebDriverSupport<? extends WebDriver> support = WebDriverSupport.fromAutoInstall();

        support.getDriver().manage().window().setPosition(new Point(0, 0));
        support.getDriver().manage().window().setSize(dimension);
        // _not_ a mistake... don't ask
        support.getDriver().manage().window().setSize(dimension);
    }

    private OAuthCredentials getOAuthCredentials()
    {
        return new OAuthCredentials(oAuth.key, oAuth.secret);
    }

    private List<BitBucketCommitEntry> getCommitsForIssue(String issueKey, int exectedNumberOfCommits)
    {
        return JIRA.visit(JiraViewIssuePage.class, issueKey)
                .openBitBucketPanel()
                .waitForNumberOfMessages(exectedNumberOfCommits, 1000L, 10);
    }

    private void setupAnonymousAccessAllowed()
    {
        JIRA.getTester().gotoUrl(JIRA.getProductInstance().getBaseUrl() + "/secure/admin/AddPermission!default.jspa?schemeId=0&permissions=" + BROWSE_PROJECTS.permissionKey());
        JIRA.getTester().getDriver().waitUntilElementIsVisible(By.id("type_group"));
        JIRA.getTester().getDriver().waitUntilElementIsVisible(By.id("add_submit"));
        JIRA.getTester().getDriver().findElement(By.id("type_group")).click();
        JIRA.getTester().getDriver().findElement(By.id("add_submit")).click();
    }

    private void setupAnonymousAccessForbidden()
    {
        JIRA.getTester().gotoUrl(JIRA.getProductInstance().getBaseUrl() + "/secure/admin/EditPermissions!default.jspa?schemeId=0");
        String deleteLinkId = "del_perm_" + BROWSE_PROJECTS.permissionKey() + "_";
        JIRA.getTester().getDriver().waitUntilElementIsVisible(By.id(deleteLinkId));
        JIRA.getTester().getDriver().findElement(By.id(deleteLinkId)).click();
        JIRA.getTester().getDriver().waitUntilElementIsVisible(By.id("delete_submit"));
        JIRA.getTester().getDriver().findElement(By.id("delete_submit")).click();
    }

    private OrganizationDiv addOrganization(AccountType accountType, String accountName, OAuthCredentials oAuthCredentials, boolean autosync)
    {
        RepositoriesPageController rpc = new RepositoriesPageController(JIRA);
        try
        {
            return rpc.addOrganization(accountType, accountName, oAuthCredentials, autosync);
        }
        catch (NoSuchElementException e)
        {
            rpc = new RepositoriesPageController(JIRA);
            return rpc.addOrganization(accountType, accountName, oAuthCredentials, autosync);
        }
    }

    public OrganizationDiv addOrganization(AccountType accountType, String accountName, OAuthCredentials oAuthCredentials, boolean autosync, boolean expectError)
    {
        RepositoriesPageController rpc = new RepositoriesPageController(JIRA);
        return rpc.addOrganization(accountType, accountName, oAuthCredentials, autosync, expectError);
    }

    private DashboardActivityStreamsPage visitActivityStreamGadget(final String gadgetId, final boolean isEditMode)
    {
        // Activity streams gadget expected at dashboard page!
        DashboardActivityStreamsPage page = JIRA.visit(DashboardActivityStreamsPage.class, isEditMode);
        assertThat(page.isActivityStreamsGadgetVisible()).isTrue();

        WebElement iframeElm = JIRA.getTester().getDriver().getDriver().findElement(By.id(gadgetId));
        String iframeSrc = iframeElm.getAttribute("src");
        JIRA.getTester().gotoUrl(iframeSrc);

        page = JIRA.getPageBinder().bind(DashboardActivityStreamsPage.class, isEditMode);
        return page;
    }

    /**
     * enable a repo under the {@link IntegrationTestUserDetails#ACCOUNT_NAME} account.
     */
    private AccountRepository enableRepositoryAsAdmin(final String repositoryName)
    {
        return enableRepositoryAsAdmin(ACCOUNT_NAME, repositoryName);
    }

    private AccountRepository enableRepositoryAsAdmin(final String accountName, final String repositoryName)
    {
        return enableRepository(accountName, repositoryName, false);
    }

    private AccountRepository enableRepository(final String accountName, final String repositoryName, final boolean noAdminPermission)
    {
        DvcsAccountsPage accountsPage = JIRA.visit(DvcsAccountsPage.class);
        Account account = accountsPage.getAccount(Account.AccountType.BITBUCKET, accountName);
        return account.enableRepository(repositoryName, noAdminPermission);
    }

    private void moveIssueToProject(final String issueKey, final String newProject)
    {
        ViewIssuePage viewIssuePage = JIRA.goToViewIssue(issueKey);
        IssueMenu issueMenu = viewIssuePage.getIssueMenu();
        issueMenu.invoke(DefaultIssueActions.MOVE);
        final MoveIssuePage moveIssuePage = JIRA.getPageBinder().bind(MoveIssuePage.class, issueKey);
        moveIssuePage.setNewProject(newProject).next().next().move();
    }

    @Override
    public void shouldBeAbleToSeePrivateRepositoriesFromTeamAccount()
    {
        // Not relevant for Bitbucket - it uses same API for organizations and users
        // but maybe we will add something here one day
    }

}
