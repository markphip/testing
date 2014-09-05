package it.restart.com.atlassian.jira.plugins.dvcs.test.pullRequest;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.AuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BasicAuthAuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.HttpClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.RepositoryRemoteRestpoint;
import it.restart.com.atlassian.jira.plugins.dvcs.RepositoriesPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.bitbucket.BitbucketLoginPage;
import it.restart.com.atlassian.jira.plugins.dvcs.bitbucket.BitbucketOAuthPage;
import it.restart.com.atlassian.jira.plugins.dvcs.common.MagicVisitor;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPageAccount;
import it.restart.com.atlassian.jira.plugins.dvcs.testClient.BitbucketClient;
import it.restart.com.atlassian.jira.plugins.dvcs.testClient.MercurialDvcs;

public class BitbucketPRTest extends PullRequestTestCases<BitbucketPullRequest>
{
    public BitbucketPRTest()
    {
    }

    @Override
    protected void beforeEachTestInitialisation(final JiraTestedProduct jiraTestedProduct)
    {
        dvcs = new MercurialDvcs();
        dvcsHostClient = new BitbucketClient();
        addOrganizations(jiraTestedProduct);
    }

    private void addOrganizations(final JiraTestedProduct jiraTestedProduct)
    {
        new MagicVisitor(jiraTestedProduct).visit(BitbucketLoginPage.class).doLogin(ACCOUNT_NAME, PASSWORD);

        // Creates & adds OAuth settings
        oAuth = new MagicVisitor(jiraTestedProduct).visit(BitbucketOAuthPage.class).addConsumer();

        // adds Bitbucket account into Jira
        RepositoriesPageController repositoriesPageController = new RepositoriesPageController(jiraTestedProduct);
        repositoriesPageController.addOrganization(RepositoriesPageController.AccountType.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), false);
        repositoriesPageController.addOrganization(RepositoriesPageController.AccountType.BITBUCKET, FORK_ACCOUNT_NAME, getOAuthCredentials(), false);
    }

    @Override
    protected void initLocalTestRepository()
    {
        dvcsHostClient.createRepository(ACCOUNT_NAME, repositoryName, PASSWORD, dvcs.getDvcsType());
        dvcs.createTestLocalRepository(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD);
    }

    @Override
    protected void cleanupLocalTestRepository()
    {
        dvcsHostClient.removeRepositories();

        dvcs.deleteAllRepositories();
    }

    @Override
    protected AccountsPageAccount.AccountType getAccountType()
    {
        return AccountsPageAccount.AccountType.BITBUCKET;
    }
}
