package it.restart.com.atlassian.jira.plugins.dvcs.test.pullRequest;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.base.resource.GitHubTestSupport;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.OAuthCredentials;
import com.atlassian.jira.plugins.dvcs.spi.githubenterprise.GithubEnterpriseClientProvider;
import it.restart.com.atlassian.jira.plugins.dvcs.RepositoriesPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.common.MagicVisitor;
import it.restart.com.atlassian.jira.plugins.dvcs.common.OAuth;
import it.restart.com.atlassian.jira.plugins.dvcs.github.GithubLoginPage;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPageAccount;
import it.restart.com.atlassian.jira.plugins.dvcs.test.GithubEnterpriseTests;
import it.restart.com.atlassian.jira.plugins.dvcs.testClient.GitHubClient;
import it.restart.com.atlassian.jira.plugins.dvcs.test.IntegrationTestUserDetails;
import it.restart.com.atlassian.jira.plugins.dvcs.testClient.GitHubDvcs;
import org.eclipse.egit.github.core.PullRequest;
import org.testng.annotations.AfterClass;

public class GithubEnterprisePRTest extends PullRequestTestCases<PullRequest>
{
    private static final String GITHUB_ENTERPRISE_USER_AGENT = "jira-dvcs-plugin-test";

    private GitHubTestSupport gitHubTestSupport;

    public GithubEnterprisePRTest()
    {
    }

    @Override
    protected void beforeEachTestInitialisation(final JiraTestedProduct jiraTestedProduct)
    {
        gitHubTestSupport = new GitHubTestSupport(new MagicVisitor(jiraTestedProduct));
        gitHubTestSupport.beforeClass();
        setupGitHubTestResource(gitHubTestSupport);
        dvcs = new GitHubDvcs(gitHubTestSupport);
        dvcsHostClient = new GitHubClient(gitHubTestSupport);

        addOrganizations(jiraTestedProduct);
    }

    private void addOrganizations(final JiraTestedProduct jiraTestedProduct)
    {
        OAuth oAuth = gitHubTestSupport.addOAuth(GithubEnterpriseTests.GITHUB_ENTERPRISE_URL, jiraTestedProduct.getProductInstance().getBaseUrl(),
                GitHubTestSupport.Lifetime.DURING_CLASS);

        new MagicVisitor(jiraTestedProduct).visit(GithubLoginPage.class, GithubEnterpriseTests.GITHUB_ENTERPRISE_URL).doLogin();
        OAuthCredentials oAuthCredentials = new OAuthCredentials(oAuth.key, oAuth.secret);
        RepositoriesPageController repositoriesPageController = new RepositoriesPageController(jiraTestedProduct);
        repositoriesPageController.getPage().deleteAllOrganizations();

        RepositoriesPageController.AccountType accountType = RepositoriesPageController.AccountType.getGHEAccountType(GithubEnterpriseTests.GITHUB_ENTERPRISE_URL);
        repositoriesPageController.addOrganization(accountType, IntegrationTestUserDetails.ACCOUNT_NAME, oAuthCredentials, false);
        repositoriesPageController.addOrganization(accountType, GitHubTestSupport.ORGANIZATION, oAuthCredentials, false);
    }

    protected void setupGitHubTestResource(GitHubTestSupport gitHubTestSupport)
    {
        org.eclipse.egit.github.core.client.GitHubClient gitHubClient = GithubEnterpriseClientProvider.createClient(GithubEnterpriseTests.GITHUB_ENTERPRISE_URL, GITHUB_ENTERPRISE_USER_AGENT);
        gitHubClient.setCredentials(ACCOUNT_NAME, PASSWORD);
        gitHubTestSupport.addOwner(ACCOUNT_NAME, gitHubClient);
        gitHubTestSupport.addOwner(GitHubTestSupport.ORGANIZATION, gitHubClient);

        org.eclipse.egit.github.core.client.GitHubClient gitHubClient2 = GithubEnterpriseClientProvider.createClient(GithubEnterpriseTests.GITHUB_ENTERPRISE_URL, GITHUB_ENTERPRISE_USER_AGENT);
        gitHubClient2.setCredentials(FORK_ACCOUNT_NAME, FORK_ACCOUNT_PASSWORD);
        gitHubTestSupport.addOwner(FORK_ACCOUNT_NAME, gitHubClient2);
    }

    @AfterClass (alwaysRun = true)
    protected void afterClass()
    {
        if (gitHubTestSupport != null)
        {
            gitHubTestSupport.afterClass();
        }
    }

    @Override
    protected void initLocalTestRepository()
    {
        gitHubTestSupport.beforeMethod();
        dvcsHostClient.createRepository(ACCOUNT_NAME, repositoryName, null, dvcs.getDvcsType());
        dvcs.createTestLocalRepository(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD);
    }

    @Override
    protected void cleanupLocalTestRepository()
    {
        gitHubTestSupport.afterMethod();
        dvcs.deleteAllRepositories();
    }

    @Override
    protected AccountsPageAccount.AccountType getAccountType()
    {
        return AccountsPageAccount.AccountType.GIT_HUB_ENTERPRISE;
    }
}
