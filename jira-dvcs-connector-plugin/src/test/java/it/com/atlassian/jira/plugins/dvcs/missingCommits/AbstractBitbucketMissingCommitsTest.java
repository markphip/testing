package it.com.atlassian.jira.plugins.dvcs.missingCommits;

import com.atlassian.jira.plugins.dvcs.pageobjects.common.BitbucketTestedProduct;
import com.atlassian.jira.plugins.dvcs.pageobjects.common.OAuth;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitBucketConfigureOrganizationsPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitbucketOAuthPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.AccountsPageAccount;
import com.atlassian.jira.plugins.dvcs.remoterestpoint.BitbucketRepositoriesRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.AuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BasicAuthAuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BitbucketRequestException;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.HttpClientProvider;
import org.testng.annotations.BeforeClass;

/**
 * Base Bitbucket class for GIT and Mercurial tests.
 */
public abstract class AbstractBitbucketMissingCommitsTest
        extends AbstractMissingCommitsTest<BitBucketConfigureOrganizationsPage>
{
    protected static BitbucketRepositoriesRemoteRestpoint bitbucketRepositoriesREST;

    protected static final BitbucketTestedProduct BIT_BUCKET = new BitbucketTestedProduct(JIRA.getTester());

    @BeforeClass
    public static void setup()
    {
        HttpClientProvider httpClientProvider = new HttpClientProvider();
        httpClientProvider.setUserAgent(BitbucketRemoteClient.TEST_USER_AGENT);
        AuthProvider basicAuthProvider = new BasicAuthAuthProvider(BitbucketRemoteClient.BITBUCKET_URL,
                DVCS_REPO_OWNER, DVCS_REPO_PASSWORD, httpClientProvider);

        bitbucketRepositoriesREST = new BitbucketRepositoriesRemoteRestpoint(basicAuthProvider.provideRequestor());
    }

    @Override
    void removeOldDvcsRepository()
    {
        try
        {
            bitbucketRepositoriesREST.removeExistingRepository(MISSING_COMMITS_REPOSITORY_NAME_PREFIX, DVCS_REPO_OWNER);
        }
        catch (BitbucketRequestException.NotFound_404 ignored) {} // the repo does not exist
    }

    @Override
    void removeRemoteDvcsRepository()
    {
        removeRepository(getMissingCommitsRepositoryName());

        for (BitbucketRepository repository : bitbucketRepositoriesREST.getAllRepositories(DVCS_REPO_OWNER))
        {
            if (timestampNameTestResource.isExpired(repository.getName()))
            {
                removeRepository(repository.getName());
            }
        }
    }

    @Override
    OAuth loginToDvcsAndGetJiraOAuthCredentials()
    {
        // log in to Bitbucket and set up OAuth
        return BIT_BUCKET
                .loginAndGoTo(DVCS_REPO_OWNER, DVCS_REPO_PASSWORD, BitbucketOAuthPage.class, DVCS_REPO_OWNER)
                .addConsumer();
    }

    @Override
    void removeOAuth()
    {
        try
        {
            if (oAuth != null)
            {
                removeConsumer(oAuth.applicationId);
            }
        }
        finally
        {
            BIT_BUCKET.logout();
        }
    }

    @Override
    protected Class<BitBucketConfigureOrganizationsPage> getConfigureOrganizationsPageClass()
    {
        return BitBucketConfigureOrganizationsPage.class;
    }

    @Override
    protected AccountsPageAccount.AccountType getAccountType()
    {
        return AccountsPageAccount.AccountType.BITBUCKET;
    }

    private void removeRepository(String name)
    {
        try
        {
            bitbucketRepositoriesREST.removeExistingRepository(name, DVCS_REPO_OWNER);
        }
        catch (BitbucketRequestException.NotFound_404 ignored) {} // the repo does not exist
    }

    private void removeConsumer(final String applicationId)
    {
        BIT_BUCKET.visit(BitbucketOAuthPage.class, DVCS_REPO_OWNER).removeConsumer(applicationId);
    }
}
