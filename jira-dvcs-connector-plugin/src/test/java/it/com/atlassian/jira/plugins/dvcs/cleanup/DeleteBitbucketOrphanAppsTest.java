package it.com.atlassian.jira.plugins.dvcs.cleanup;

import com.atlassian.jira.plugins.dvcs.pageobjects.common.BitbucketTestedProduct;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitbucketOAuthPage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketConsumer;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.AuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BasicAuthAuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.HttpClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.ConsumerRemoteRestpoint;
import com.beust.jcommander.internal.Lists;

import java.util.List;

/**
 * Delete the orphan OAuth Applications created by Webdriver tests for Bitbucket.
 */
public class DeleteBitbucketOrphanAppsTest extends DeleteOrphanAppsBaseTest
{
    protected static final BitbucketTestedProduct BITBUCKET = new BitbucketTestedProduct(JIRA.getTester());

    @Override
    protected void login(final String repoOwner, final String repoPassword)
    {
        BITBUCKET.login(repoOwner, repoPassword);
    }

    @Override
    protected void logout()
    {
        BITBUCKET.logout();
    }

    @Override
    protected void deleteOrphanOAuthApplications(final String repoOwner, final String repoPassword)
    {
        BitbucketOAuthPage page = goToOAuthPage(repoOwner);
        List<BitbucketConsumer> expiredConsumers = findExpiredConsumers(repoOwner, repoPassword);

        for (BitbucketConsumer consumer : expiredConsumers)
        {
            page.removeConsumer(consumer.getId().toString());
        }
    }

    /**
     * @return list of expired consumers to be deleted from Bitbucket
     */
    private List<BitbucketConsumer> findExpiredConsumers(final String repoOwner, final String repoPassword)
    {
        ConsumerRemoteRestpoint consumerRemoteRestpoint = createConsumerRemoteRestpoint(repoOwner, repoPassword);
        List<BitbucketConsumer> expiredConsumers = Lists.newArrayList();

        List<BitbucketConsumer> consumers = consumerRemoteRestpoint.getConsumers(repoOwner);
        for (BitbucketConsumer consumer : consumers)
        {
            if (super.isConsumerExpired(consumer.getName()))
            {
                expiredConsumers.add(consumer);
            }
        }
        return expiredConsumers;
    }

    private ConsumerRemoteRestpoint createConsumerRemoteRestpoint(final String repoOwner, final String repoPassword)
    {
        HttpClientProvider httpClientProvider = new HttpClientProvider();
        httpClientProvider.setUserAgent(BitbucketRemoteClient.TEST_USER_AGENT);

        AuthProvider basicAuthProvider = new BasicAuthAuthProvider(BitbucketRemoteClient.BITBUCKET_URL,
                repoOwner, repoPassword, httpClientProvider);

        return new ConsumerRemoteRestpoint(basicAuthProvider.provideRequestor());
    }

    private BitbucketOAuthPage goToOAuthPage(final String repoOwner)
    {
        return BITBUCKET.visit(BitbucketOAuthPage.class, repoOwner);
    }
}
