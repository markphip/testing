package it.restart.com.atlassian.jira.plugins.dvcs.test;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.pageobjects.pages.DashboardPage;
import com.atlassian.jira.plugins.dvcs.crypto.Encryptor;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.pageobjects.component.BitBucketCommitEntry;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.JiraViewIssuePage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.OAuthCredentials;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketClientBuilderFactory;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.DefaultBitbucketClientBuilderFactory;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketConstants;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepositoryLink;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepositoryLinkHandler;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.HttpClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.RepositoryLinkRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.util.HttpSenderUtils;
import com.atlassian.jira.plugins.dvcs.util.PasswordUtil;
import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.pageobjects.elements.PageElement;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import it.com.atlassian.jira.plugins.dvcs.DvcsWebDriverTestCase;
import it.restart.com.atlassian.jira.plugins.dvcs.DashboardActivityStreamsPage;
import it.restart.com.atlassian.jira.plugins.dvcs.GreenHopperBoardPage;
import it.restart.com.atlassian.jira.plugins.dvcs.JiraAddUserPage;
import it.restart.com.atlassian.jira.plugins.dvcs.JiraLoginPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.JiraMove_QA1_IssuePage;
import it.restart.com.atlassian.jira.plugins.dvcs.OrganizationDiv;
import it.restart.com.atlassian.jira.plugins.dvcs.RepositoriesPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.RepositoriesPageController.AccountType;
import it.restart.com.atlassian.jira.plugins.dvcs.bitbucket.BitbucketLoginPage;
import it.restart.com.atlassian.jira.plugins.dvcs.bitbucket.BitbucketOAuthPage;
import it.restart.com.atlassian.jira.plugins.dvcs.common.MagicVisitor;
import it.restart.com.atlassian.jira.plugins.dvcs.common.OAuth;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPage;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPageAccount;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPageAccountRepository;
import org.apache.commons.httpclient.methods.GetMethod;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.atlassian.jira.plugins.dvcs.pageobjects.BitBucketCommitEntriesAssert.assertThat;
import static org.fest.assertions.api.Assertions.assertThat;

public class BitbucketTests extends DvcsWebDriverTestCase implements BasicTests, ActivityStreamsTest
{
    private static JiraTestedProduct jira = TestedProductFactory.create(JiraTestedProduct.class);
    private static final String ACCOUNT_NAME = "jirabitbucketconnector";
    private static final String OTHER_ACCOUNT_NAME = "dvcsconnectortest";
    private OAuth oAuth;


    @BeforeClass
    public void beforeClass()
    {
        // log in to JIRA
        new JiraLoginPageController(jira).login();
        // log in to Bitbucket
        new MagicVisitor(jira).visit(BitbucketLoginPage.class).doLogin();
        // setup up OAuth from bitbucket
        oAuth = new MagicVisitor(jira).visit(BitbucketOAuthPage.class).addConsumer();
        // jira.visit(JiraBitbucketOAuthPage.class).setCredentials(oAuth.key, oAuth.secret);
        jira.backdoor().plugins().disablePlugin("com.atlassian.jira.plugins.jira-development-integration-plugin");
    }

    @AfterClass
    public void afterClass()
    {
        // delete all organizations
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.getPage().deleteAllOrganizations();
        // remove OAuth in bitbucket
        new MagicVisitor(jira).visit(BitbucketOAuthPage.class).removeConsumer(oAuth.applicationId);
        // log out from bitbucket
        new MagicVisitor(jira).visit(BitbucketLoginPage.class).doLogout();
    }

    @BeforeMethod
    public void beforeMethod()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.getPage().deleteAllOrganizations();
    }

    @Override
    @Test
    public void addOrganization()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        OrganizationDiv organization = rpc.addOrganization(AccountType.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), false);

        assertThat(organization).isNotNull();
        assertThat(organization.getRepositories(true).size()).isEqualTo(4);

        // check add user extension
        PageElement dvcsExtensionsPanel = jira.visit(JiraAddUserPage.class).getDvcsExtensionsPanel();
        assertThat(dvcsExtensionsPanel.isVisible());
    }

    @Override
    @Test
    public void addOrganizationWaitForSync()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        OrganizationDiv organization = rpc.addOrganization(AccountType.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), true);

        assertThat(organization).isNotNull();
        assertThat(organization.getRepositories(true).size()).isEqualTo(4);
        assertThat(organization.getRepositories(true).get(3).getMessage()).isEqualTo("Fri Mar 02 2012");

        assertLinksWereInstalled(ACCOUNT_NAME, organization.getRepositories(true).get(3).getRepositoryName(), "QA");

        assertThat(getCommitsForIssue("QA-2", 1)).hasItemWithCommitMessage("BB modified 1 file to QA-2 and QA-3 from TestRepo-QA");
        assertThat(getCommitsForIssue("QA-3", 2)).hasItemWithCommitMessage("BB modified 1 file to QA-2 and QA-3 from TestRepo-QA");
    }

    @Override
    @Test(expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = ".*Error!\\nThe url \\[https://privatebitbucket.org\\] is incorrect or the server is not responding.*")
    public void addOrganizationInvalidUrl()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.addOrganization(AccountType.BITBUCKET, "https://privatebitbucket.org/someaccount", getOAuthCredentials(), false);
    }

    @Override
    @Test(expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = ".*Error!\\nInvalid user/team account.*")
    public void addOrganizationInvalidAccount()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.addOrganization(AccountType.BITBUCKET, "I_AM_SURE_THIS_ACCOUNT_IS_INVALID", getOAuthCredentials(), false);
    }

    @Override
    @Test(expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = ".*Error!\\nThe authentication with Bitbucket has failed. Please check your OAuth settings.*")
    public void addOrganizationInvalidOAuth()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        OrganizationDiv organization = rpc.addOrganization(AccountType.BITBUCKET, ACCOUNT_NAME, new OAuthCredentials("bad", "credentials"), true);

        assertThat(organization).isNotNull();
        assertThat(organization.getRepositories(true).size()).isEqualTo(4);
    }

    @Test
    @Override
    public void testCommitStatistics()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.addOrganization(AccountType.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), true);

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
    public void testPostCommitHookAdded()
    {
        // remove existing
        String bitbucketServiceConfigUrl = "https://bitbucket.org/!api/1.0/repositories/jirabitbucketconnector/public-hg-repo/services";
        HttpSenderUtils.removeJsonElementsUsingIDs(bitbucketServiceConfigUrl, "jirabitbucketconnector", PasswordUtil.getPassword("jirabitbucketconnector"));

        // add organization
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.addOrganization(AccountType.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), true);

        // check postcommit hook is there
        String baseUrl = jira.getProductInstance().getBaseUrl();
        String syncUrl = baseUrl + "/rest/bitbucket/1.0/repository/";

        String servicesConfig = HttpSenderUtils.makeHttpRequest(new GetMethod(bitbucketServiceConfigUrl),
                "jirabitbucketconnector", PasswordUtil.getPassword("jirabitbucketconnector"));
        if (!servicesConfig.contains(syncUrl))
        {
            // retrying once more
            servicesConfig = HttpSenderUtils.makeHttpRequest(new GetMethod(bitbucketServiceConfigUrl),
                    "jirabitbucketconnector", PasswordUtil.getPassword("jirabitbucketconnector"));
        }

        assertThat(servicesConfig).contains(syncUrl);

        // delete repository
        rpc = new RepositoriesPageController(jira);
        rpc.getPage().deleteAllOrganizations();

        // check that postcommit hook is removed
        servicesConfig = HttpSenderUtils.makeHttpRequest(new GetMethod(bitbucketServiceConfigUrl),
                "jirabitbucketconnector", PasswordUtil.getPassword("jirabitbucketconnector"));
        assertThat(servicesConfig).doesNotContain(syncUrl);
    }

    @Test
    @Override
    public void testActivityPresentedForQA5()
    {
        // add organization
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.addOrganization(AccountType.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), true);

        // Activity streams gadget expected at dashboard page!
        DashboardActivityStreamsPage page = jira.visit(DashboardActivityStreamsPage.class);
        assertThat(page.isActivityStreamsGadgetVisible()).isTrue();

        WebElement iframeElm = jira.getTester().getDriver().getDriver().findElement(By.id("gadget-10001"));
        String iframeSrc = iframeElm.getAttribute("src");
        jira.getTester().gotoUrl(iframeSrc);

        page = jira.getPageBinder().bind(DashboardActivityStreamsPage.class);

        // Activity streams should contain at least one changeset with 'more files' link.
        assertThat(page.isMoreFilesLinkVisible()).isTrue();
        page.checkIssueActivityPresentedForQA5();

        // TODO commenting out this part of test, page objects should be fixed for Jira 6.1
        page.setIssueKeyFilter("QA-4");
        page = jira.getPageBinder().bind(DashboardActivityStreamsPage.class);

        // because commit contains both keys QA-4 and QA-5, so should be present on both issues' activity streams
        page.checkIssueActivityPresentedForQA5();

        page.setIssueKeyFilter("QA-5");
        page = jira.getPageBinder().bind(DashboardActivityStreamsPage.class);

        page.checkIssueActivityPresentedForQA5();

        // delete repository
        rpc = new RepositoriesPageController(jira);
        rpc.getPage().deleteAllOrganizations();

        page = jira.visit(DashboardActivityStreamsPage.class);
        page.checkIssueActivityNotPresentedForQA5();
    }

    @Override
    @Test
    public void testAnonymousAccess()
    {
        setupAnonymousAccessAllowed();
        // add organization
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.addOrganization(AccountType.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), true);

        // Activity streams gadget expected at dashboard page!
        DashboardActivityStreamsPage page = jira.visit(DashboardActivityStreamsPage.class);
        assertThat(page.isActivityStreamsGadgetVisible()).isTrue();

        WebElement iframeElm = jira.getTester().getDriver().getDriver().findElement(By.id("gadget-10001"));
        String iframeSrc = iframeElm.getAttribute("src");
        jira.getTester().gotoUrl(iframeSrc);

        page = jira.getPageBinder().bind(DashboardActivityStreamsPage.class);
        page.checkIssueActivityPresentedForQA5();

        // logout user
        jira.getTester().getDriver().manage().deleteAllCookies();

        // Activity streams gadget expected at dashboard page!
        jira.visit(DashboardPage.class);
        assertThat(page.isActivityStreamsGadgetVisible()).isTrue();

        jira.getTester().gotoUrl(iframeSrc);
        page = jira.getPageBinder().bind(DashboardActivityStreamsPage.class);
        page.checkIssueActivityNotPresentedForQA5();

        // log in to JIRA
        new JiraLoginPageController(jira).login();
        setupAnonymousAccessForbidden();
    }

    @Test
    public void greenHopperIntegration_ShouldAddDvcsCommitsTab()
    {
        // add organization
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.addOrganization(AccountType.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), true);

        GreenHopperBoardPage greenHopperBoardPage = jira.getPageBinder().navigateToAndBind(GreenHopperBoardPage.class);
        greenHopperBoardPage.goToQABoardPlan();
        greenHopperBoardPage.assertCommitsAppearOnIssue("QA-1", 5);
    }

    @Test
    public void moveIssue_ShouldKeepAlsoCommits()
    {
        // add organization
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.addOrganization(AccountType.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), true);

        // move issue from QA project to BBC project
        JiraMove_QA1_IssuePage movingPage = jira.getPageBinder().navigateToAndBind(JiraMove_QA1_IssuePage.class, jira.getPageBinder());
        movingPage.stepOne_typeProjectName("Bitbucket Connector")
                  .clickNext()
                  .clickNext()
                  .submit();

        // check commits kept
        // in fact, Jira will make the redirect to moved/created issue BBC-1
        assertThat(getCommitsForIssue("QA-1", 5)).hasItemWithCommitMessage("QA-1 test modification");
    }

    @Override
    @Test
    public void linkingRepositoryWithoutAdminPermission()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.addOrganization(AccountType.BITBUCKET, OTHER_ACCOUNT_NAME, getOAuthCredentials(), false);

        AccountsPage accountsPage = jira.visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountsPageAccount.AccountType.BITBUCKET, OTHER_ACCOUNT_NAME);
        AccountsPageAccountRepository repository = account.enableRepository("testemptyrepo", true);

        // check that repository is enabled
        Assert.assertTrue(repository.isEnabled());
        Assert.assertTrue(repository.hasWarning());
    }

    @Override
    @Test
    public void linkingRepositoryWithAdminPermission()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.addOrganization(AccountType.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), false);

        AccountsPage accountsPage = jira.visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountsPageAccount.AccountType.BITBUCKET, ACCOUNT_NAME);
        AccountsPageAccountRepository repository = account.enableRepository("private-git-repo", false);

        // check that repository is enabled
        Assert.assertTrue(repository.isEnabled());
        Assert.assertFalse(repository.hasWarning());
    }

    @Override
    @Test
    public void autoLinkingRepositoryWithoutAdminPermission()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.addOrganization(AccountType.BITBUCKET, OTHER_ACCOUNT_NAME, getOAuthCredentials(), true);

        AccountsPage accountsPage = jira.visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountsPageAccount.AccountType.BITBUCKET, OTHER_ACCOUNT_NAME);

        for (AccountsPageAccountRepository repository : account.getRepositories())
        {
            Assert.assertTrue(repository.isEnabled());
        }
    }

    @Override
    @Test
    public void autoLinkingRepositoryWithAdminPermission()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.addOrganization(AccountType.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), true);

        AccountsPage accountsPage = jira.visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountsPageAccount.AccountType.BITBUCKET, ACCOUNT_NAME);
        for (AccountsPageAccountRepository repository : account.getRepositories())
        {
            Assert.assertTrue(repository.isEnabled());
            Assert.assertFalse(repository.hasWarning());
        }
    }

    //-------------------------------------------------------------------
    //--------- these methods should go to some common utility/class ----
    //-------------------------------------------------------------------

    private OAuthCredentials getOAuthCredentials()
    {
        return new OAuthCredentials(oAuth.key, oAuth.secret);
    }

    private List<BitBucketCommitEntry> getCommitsForIssue(String issueKey, int exectedNumberOfCommits)
    {
        return jira.visit(JiraViewIssuePage.class, issueKey)
                .openBitBucketPanel()
                .waitForNumberOfMessages(exectedNumberOfCommits, 1000L, 5);
    }

    private void setupAnonymousAccessAllowed()
    {
        jira.getTester().gotoUrl(jira.getProductInstance().getBaseUrl() + "/secure/admin/AddPermission!default.jspa?schemeId=0&permissions=10");
        jira.getTester().getDriver().waitUntilElementIsVisible(By.id("type_group"));
        jira.getTester().getDriver().waitUntilElementIsVisible(By.id("add_submit"));
        jira.getTester().getDriver().findElement(By.id("type_group")).click();
        jira.getTester().getDriver().findElement(By.id("add_submit")).click();
    }

    private void setupAnonymousAccessForbidden()
    {
        jira.getTester().gotoUrl(jira.getProductInstance().getBaseUrl() + "/secure/admin/EditPermissions!default.jspa?schemeId=0");
        jira.getTester().getDriver().waitUntilElementIsVisible(By.id("del_perm_10_"));
        jira.getTester().getDriver().findElement(By.id("del_perm_10_")).click();
        jira.getTester().getDriver().waitUntilElementIsVisible(By.id("delete_submit"));
        jira.getTester().getDriver().findElement(By.id("delete_submit")).click();
    }

    @Override
    public void shouldBeAbleToSeePrivateRepositoriesFromTeamAccount()
    {
        // Not relevant for Bitbucket - it uses same API for organizations and users
        // but maybe we will add something here one day
    }

    private RepositoryLinkRemoteRestpoint getRepositoryLinkRemoteRestpoint(String owner, String password)
    {
        BitbucketClientBuilderFactory bitbucketClientBuilderFactory = new DefaultBitbucketClientBuilderFactory(new Encryptor()
        {

            @Override
            public String encrypt(final String input, final String organizationName, final String hostUrl)
            {
                return input;
            }

            @Override
            public String decrypt(final String input, final String organizationName, final String hostUrl)
            {
                return input;
            }
        }, "DVCS Connector Tests", new HttpClientProvider());
        Credential credential = new Credential();
        credential.setAdminUsername(owner);
        credential.setAdminPassword(password);
        BitbucketRemoteClient bitbucketClient = bitbucketClientBuilderFactory.authClient("https://bitbucket.org", null, credential).build();
        return bitbucketClient.getRepositoryLinksRest();
    }

    private void assertLinksWereInstalled(String owner, String repositoryName, String... projectKeys)
    {
        RepositoryLinkRemoteRestpoint repositoryLinkRemoteRestpoint = getRepositoryLinkRemoteRestpoint(owner, PasswordUtil.getPassword(owner));

        List<BitbucketRepositoryLink> repositoryLinks = filterLinksToThisJira(repositoryLinkRemoteRestpoint.getRepositoryLinks(owner, repositoryName));
        assertThat(repositoryLinks).isNotNull().isNotEmpty().hasSize(1).as("Exactly one link to the instance for the repository should be installed");
        assertThat(getProjectKeysFromLinkOrNull(repositoryLinks.get(0))).containsExactly(projectKeys).as("Links for project are not installed");
    }

    //TODO these methods are from BitbucketLinker to tests Bitbucket links, some refactor and code reuse would be needed
    private List<BitbucketRepositoryLink> filterLinksToThisJira(List<BitbucketRepositoryLink> currentBitbucketLinks)
    {
        List<BitbucketRepositoryLink> linksToThisJira = Lists.newArrayList();
        for (BitbucketRepositoryLink repositoryLink : currentBitbucketLinks)
        {
            // make sure that is of type jira or custom (new version of linking)
            if (isCustomOrJiraType(repositoryLink))
            {
                BitbucketRepositoryLinkHandler handler = repositoryLink.getHandler();
                String displayTo = handler.getDisplayTo();
                if (displayTo!=null && displayTo.toLowerCase().startsWith(jira.getProductInstance().getBaseUrl().toLowerCase()))
                {
                    // remove links just to OUR jira instance
                    linksToThisJira.add(repositoryLink);
                }
            }
        }
        return linksToThisJira;
    }

    private boolean isCustomOrJiraType(BitbucketRepositoryLink repositoryLink)
    {
        return repositoryLink.getHandler() != null &&
                (BitbucketConstants.REPOSITORY_LINK_TYPE_JIRA.equals(repositoryLink.getHandler().getName())
                        || BitbucketConstants.REPOSITORY_LINK_TYPE_CUSTOM.equals(repositoryLink.getHandler().getName()));
    }

    private HashSet<String> getProjectKeysFromLinkOrNull(BitbucketRepositoryLink bitbucketRepositoryLink)
    {
        String regexp = null;
        try
        {
            regexp = bitbucketRepositoryLink.getHandler().getRawRegex();
            Matcher matcher = Pattern.compile("[A-Z|a-z]{2,}(|)+").matcher(regexp);
            matcher.find();
            String pipedProjectKeys = matcher.group(0);
            return Sets.newHashSet(Splitter.on("|").split(pipedProjectKeys));
        } catch (Exception e)
        {
            return null;
        }
    }
}
