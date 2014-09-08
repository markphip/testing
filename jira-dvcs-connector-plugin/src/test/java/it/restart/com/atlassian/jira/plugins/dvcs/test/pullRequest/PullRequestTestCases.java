package it.restart.com.atlassian.jira.plugins.dvcs.test.pullRequest;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.base.resource.TimestampNameTestResource;
import com.atlassian.jira.plugins.dvcs.model.PullRequestStatus;
import com.atlassian.jira.plugins.dvcs.model.dev.RestDevResponse;
import com.atlassian.jira.plugins.dvcs.model.dev.RestPrRepository;
import com.atlassian.jira.plugins.dvcs.model.dev.RestPullRequest;
import com.google.common.base.Function;
import it.restart.com.atlassian.jira.plugins.dvcs.JiraLoginPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.RepositoriesPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPage;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPageAccount;
import it.restart.com.atlassian.jira.plugins.dvcs.test.AbstractDVCSTest;
import it.restart.com.atlassian.jira.plugins.dvcs.testClient.Dvcs;
import it.restart.com.atlassian.jira.plugins.dvcs.testClient.DvcsHostClient;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import javax.annotation.Nullable;

/**
 * Base class that contains the test cases for the PullRequest scenarios.
 * <p/>
 * Implementors will need to override the abstract methods as required. If necessary you may need to provide a custom
 * implementation of {@link it.restart.com.atlassian.jira.plugins.dvcs.testClient.Dvcs} or {@link
 * it.restart.com.atlassian.jira.plugins.dvcs.testClient.DvcsHostClient}.
 * <p/>
 * Also due to the coupling between this class and its implementors you should be wary of relying on the state of parent
 * member variables like {@link #dvcs} or {@link #getJiraTestedProduct()} which should be intialised during {@link
 * #beforeEachPullRequestTest()}.
 */
public abstract class PullRequestTestCases<T> extends AbstractDVCSTest
{
    protected static final int EXPIRATION_DURATION_5_MIN = 5 * 60 * 1000;
    protected static final String TEST_PROJECT_KEY = "TST";

    protected static final String COMMIT_AUTHOR = "Stanislav Dvorscak";
    private static final String COMMIT_AUTHOR_EMAIL = "sdvorscak@atlassian.com";

    /**
     * Repository owner.
     */
    protected static final String ACCOUNT_NAME = "jirabitbucketconnector";

    /**
     * Fork repository owner.
     */
    protected static final String FORK_ACCOUNT_NAME = "dvcsconnectortest";

    /**
     * Appropriate {@link #ACCOUNT_NAME} password.
     */
    protected static final String PASSWORD = System.getProperty("jirabitbucketconnector.password");

    /**
     * Appropriate {@link #FORK_ACCOUNT_NAME} password.
     */
    protected static final String FORK_ACCOUNT_PASSWORD = System.getProperty("dvcsconnectortest.password");

    protected Dvcs dvcs;
    protected DvcsHostClient<T> dvcsHostClient;

    protected TimestampNameTestResource timestampNameTestResource = new TimestampNameTestResource();
    protected DvcsPRTestHelper dvcsPRTestHelper;

    /**
     * Issue key used for testing.
     */
    protected String issueKey;

    protected String repositoryName;

    public PullRequestTestCases()
    {
    }

    protected String getTestIssueSummary()
    {
        return this.getClass().getCanonicalName();
    }

    protected String getRepositoryNameSuffix()
    {
        return this.getClass().getSimpleName().toLowerCase();
    }

    @BeforeClass
    public void beforeEachPullRequestTestClass()
    {
        new JiraLoginPageController(getJiraTestedProduct()).login();

        beforeEachTestInitialisation(getJiraTestedProduct());

        dvcsPRTestHelper = new DvcsPRTestHelper(dvcs, ACCOUNT_NAME, COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL, PASSWORD);
    }

    /**
     * Setup prior to running a test case, implementing classes <b>MUST</b> associate the organisations in JIRA
     * <b>AND</b> initialise the following member variables on the parent: <ul> <li>PullRequestTestCases#dvcs</li>
     * <li>PullRequestTestCases#dvcsHostClient</li> </ul>
     */
    protected abstract void beforeEachTestInitialisation(JiraTestedProduct jiraTestedProduct);

    @AfterClass (alwaysRun = true)
    public void afterEachPullRequestTestClass()
    {
        RepositoriesPageController repositoriesPageController = new RepositoriesPageController(getJiraTestedProduct());
        repositoriesPageController.getPage().deleteAllOrganizations();
    }

    @BeforeMethod
    protected void beforeEachPullRequestTest()
    {
        repositoryName = timestampNameTestResource.randomName(getRepositoryNameSuffix(), EXPIRATION_DURATION_5_MIN);
        initLocalTestRepository();
        issueKey = addTestIssue(TEST_PROJECT_KEY, getTestIssueSummary());
    }

    /**
     * Initialise the local repository for testing, called before each method
     *
     * @see #beforeEachPullRequestTest()
     */
    protected abstract void initLocalTestRepository();

    @AfterMethod
    protected void afterEachPullRequestTest()
    {
        cleanupLocalTestRepository();
    }

    /**
     * Called after each test method, clean up any test repositories
     *
     * @see #afterEachPullRequestTest()
     */
    protected abstract void cleanupLocalTestRepository();

    /**
     * The type of account we should look for when refreshing
     */
    protected abstract AccountsPageAccount.AccountType getAccountType();

    @Test
    public void testOpenPullRequestUpdateApproveAndMerge()
    {
        String pullRequestName = issueKey + ": Open PR";
        String fixBranchName = issueKey + "_fix";
        Collection<String> firstRoundCommits = dvcsPRTestHelper.createBranchAndCommits(repositoryName, fixBranchName, issueKey, 2);

        DvcsHostClient.PullRequestDetails<T> pullRequestDetails = dvcsHostClient.openPullRequest(ACCOUNT_NAME, repositoryName, PASSWORD, pullRequestName, "Open PR description",
                fixBranchName, dvcs.getDefaultBranchName());
        String pullRequestLocation = pullRequestDetails.getLocation();

        // Wait for remote system after creation of pullRequest
        sleep(500);

        RestPrRepository restPrRepository = refreshSyncAndGetFirstPrRepository();

        RestPrRepositoryPRTestAsserter asserter = new RestPrRepositoryPRTestAsserter(repositoryName, pullRequestLocation, pullRequestName, ACCOUNT_NAME,
                fixBranchName, dvcs.getDefaultBranchName());

        asserter.assertBasicPullRequestConfiguration(restPrRepository, firstRoundCommits);

        // We will check up on these later once there has been a status change and a full sync of the PR
        Collection<String> secondRoundCommits = dvcsPRTestHelper.addMoreCommitsAndPush(repositoryName, fixBranchName, issueKey, 2);

        final String updatedPullRequestName = pullRequestName + "updated";
        pullRequestDetails = dvcsHostClient.updatePullRequest(ACCOUNT_NAME, repositoryName, PASSWORD, pullRequestDetails.getPullRequest(),
                updatedPullRequestName, "updated desc", dvcs.getDefaultBranchName());
        dvcsHostClient.commentPullRequest(ACCOUNT_NAME, repositoryName, PASSWORD, pullRequestDetails.getPullRequest(), "Some comment after update");

        if (getAccountType() == AccountsPageAccount.AccountType.BITBUCKET)
        {
            dvcsHostClient.approvePullRequest(ACCOUNT_NAME, repositoryName, PASSWORD, pullRequestDetails.getId());

            sleep(500);

            restPrRepository = refreshSyncAndGetFirstPrRepository();
            asserter.assertPullRequestApproved(restPrRepository.getPullRequests().get(0));
        }

        dvcsHostClient.mergePullRequest(ACCOUNT_NAME, repositoryName, PASSWORD, pullRequestDetails.getId());

        sleep(1000);

        restPrRepository = refreshSyncAndGetFirstPrRepository();

        final RestPullRequest restPullRequest = restPrRepository.getPullRequests().get(0);
        Assert.assertEquals(restPullRequest.getStatus(), PullRequestStatus.MERGED.toString());
        Assert.assertEquals(restPullRequest.getTitle(), updatedPullRequestName);

        // Comments are not critical so let us just check them at the end rather than having a separate refresh and wait
        Assert.assertEquals(restPullRequest.getCommentCount(), 1);

        // Commits should be pulled in now that we have merged
        Collection<String> allCommits = new ArrayList<String>(firstRoundCommits);
        allCommits.addAll(secondRoundCommits);

        asserter.assertCommitsMatch(restPrRepository.getPullRequests().get(0), allCommits);
    }

    @Test
    public void testDecline()
    {
        String pullRequestName = issueKey + ": Open PR";
        String fixBranchName = issueKey + "_fix";
        Collection<String> commits = dvcsPRTestHelper.createBranchAndCommits(repositoryName, fixBranchName, issueKey, 2);

        DvcsHostClient.PullRequestDetails<T> pullRequestDetails = dvcsHostClient.openPullRequest(ACCOUNT_NAME, repositoryName, PASSWORD, pullRequestName, "Open PR description",
                fixBranchName, dvcs.getDefaultBranchName());
        String pullRequestLocation = pullRequestDetails.getLocation();

        // Wait for remote system after creation of pullRequest
        sleep(500);

        RestPrRepository restPrRepository = refreshSyncAndGetFirstPrRepository();

        RestPrRepositoryPRTestAsserter asserter = new RestPrRepositoryPRTestAsserter(repositoryName, pullRequestLocation, pullRequestName, ACCOUNT_NAME,
                fixBranchName, dvcs.getDefaultBranchName());

        asserter.assertBasicPullRequestConfiguration(restPrRepository, commits);

        dvcsHostClient.declinePullRequest(ACCOUNT_NAME, repositoryName, PASSWORD, pullRequestDetails.getPullRequest());

        restPrRepository = refreshSyncAndGetFirstPrRepository();

        final RestPullRequest restPullRequest = restPrRepository.getPullRequests().get(0);
        Assert.assertEquals(restPullRequest.getStatus(), PullRequestStatus.DECLINED.toString());
        Assert.assertEquals(restPullRequest.getTitle(), pullRequestName);

        asserter.assertCommitsMatch(restPrRepository.getPullRequests().get(0), commits);
    }

    @Test
    public void testFork()
    {
        String pullRequestName = issueKey + ": Open PR";
        String fixBranchName = issueKey + "_fix";
        Collection<String> commits = dvcsPRTestHelper.createBranchAndCommits(repositoryName, fixBranchName, issueKey, 2);

        forkRepository(ACCOUNT_NAME, repositoryName, FORK_ACCOUNT_NAME, FORK_ACCOUNT_PASSWORD);

        dvcsPRTestHelper.addMoreCommitsAndPush(FORK_ACCOUNT_NAME, FORK_ACCOUNT_PASSWORD, repositoryName, fixBranchName, issueKey, 2);

        DvcsHostClient.PullRequestDetails<T> pullRequestDetails = dvcsHostClient.openForkPullRequest(ACCOUNT_NAME, repositoryName, pullRequestName, "Open PR description",
                fixBranchName, dvcs.getDefaultBranchName(), FORK_ACCOUNT_NAME, FORK_ACCOUNT_PASSWORD);
        String pullRequestLocation = pullRequestDetails.getLocation();

        RestPrRepository restPrRepository = refreshSyncAndGetFirstPrRepository();

        RestPrRepositoryPRTestAsserter asserter = new RestPrRepositoryPRTestAsserter(repositoryName, pullRequestLocation, pullRequestName, ACCOUNT_NAME, FORK_ACCOUNT_NAME,
                fixBranchName, dvcs.getDefaultBranchName());

        asserter.assertBasicPullRequestConfiguration(restPrRepository, commits);
    }

    private void forkRepository(final String owner, final String repositoryName, final String forkOwner, final String forkPassword) {
        dvcsHostClient.fork(owner, repositoryName, forkOwner, forkPassword);

        getJiraTestedProduct().getTester().getDriver().waitUntil(new Function<WebDriver, Boolean>()
        {
            @Override
            public Boolean apply(@Nullable final WebDriver input)
            {
                return dvcsHostClient.isRepositoryExists(forkOwner, repositoryName, forkPassword);
            }
        }, 5);

        dvcs.createTestLocalRepository(forkOwner, repositoryName, forkOwner, forkPassword);
    }

    private RestPrRepository refreshSyncAndGetFirstPrRepository()
    {
        AccountsPageAccount account = refreshAccount(ACCOUNT_NAME);
        account.synchronizeRepository(repositoryName);

        RestDevResponse<RestPrRepository> response = getPullRequestResponse(issueKey);

        Assert.assertEquals(response.getRepositories().size(), 1);
        return response.getRepositories().get(0);
    }

    protected AccountsPageAccount refreshAccount(final String accountName)
    {
        AccountsPage accountsPage = getJiraTestedProduct().visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(getAccountType(), accountName);
        account.refresh();

        return account;
    }

    protected void sleep(final long millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch (InterruptedException e)
        {
        }
    }
}
