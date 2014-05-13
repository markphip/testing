package com.atlassian.jira.plugins.dvcs.spi.bitbucket;

import com.atlassian.jira.plugins.dvcs.crypto.Encryptor;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.AuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BasicAuthAuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.HttpClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.NoAuthAuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.scribe.ThreeLegged10aOauthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.scribe.TwoLeggedOauthProvider;
import com.atlassian.jira.plugins.dvcs.util.DvcsConstants;
import com.atlassian.plugin.PluginAccessor;
import org.apache.commons.lang.StringUtils;

public class DefaultBitbucketClientBuilderFactory implements BitbucketClientBuilderFactory
{

    private final Encryptor encryptor;
    private final String userAgent;
    private final HttpClientProvider httpClientProvider;

    public DefaultBitbucketClientBuilderFactory(Encryptor encryptor, PluginAccessor pluginAccessor, HttpClientProvider httpClientProvider)
    {
        this.encryptor = encryptor;
        this.userAgent = DvcsConstants.getUserAgent(pluginAccessor);
        this.httpClientProvider = httpClientProvider;
        httpClientProvider.setUserAgent(userAgent);
    }

    @Override
    public BitbucketClientBuilder forOrganization(Organization organization)
    {
        AuthProvider authProvider = createProvider(organization.getHostUrl(), organization.getName(), organization.getCredential());
        return new DefaultBitbucketRemoteClientBuilder(authProvider);
    }

    @Override
    public BitbucketClientBuilder forRepository(Repository repository)
    {
        AuthProvider authProvider = createProvider(repository.getOrgHostUrl(), repository.getOrgName(), repository.getCredential());
        return new DefaultBitbucketRemoteClientBuilder(authProvider);
    }

    @Override
    public BitbucketClientBuilder noAuthClient(String hostUrl)
    {
        AuthProvider authProvider = new NoAuthAuthProvider(hostUrl, httpClientProvider);
        return new DefaultBitbucketRemoteClientBuilder(authProvider);
    }

    @Override
    public BitbucketClientBuilder authClient(String hostUrl, String name, Credential credential)
    {
        AuthProvider authProvider = createProvider(hostUrl, name, credential);
        return new DefaultBitbucketRemoteClientBuilder(authProvider);
    }

    private AuthProvider createProvider(String hostUrl, String name, Credential credential)
    {
        String username =  credential.getAdminUsername();
        String password = credential.getAdminPassword();
        String key = credential.getOauthKey();
        String secret = credential.getOauthSecret();
        String accessToken = credential.getAccessToken();

        AuthProvider oAuthProvider;
        // 3LO (key, secret, token)
        if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(secret) && StringUtils.isNotBlank(accessToken))
        {
            oAuthProvider = new ThreeLegged10aOauthProvider(hostUrl, key, secret, accessToken, httpClientProvider);
        }

        // 2LO (key, secret)
        else if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(secret))
        {
            oAuthProvider = new TwoLeggedOauthProvider(hostUrl, key, secret, httpClientProvider);
        }

        // basic (username, password)
        else if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password))
        {
            String decryptedPassword = encryptor.decrypt(password, name, hostUrl);
            oAuthProvider =  new BasicAuthAuthProvider(hostUrl, username,decryptedPassword, httpClientProvider);
        } else
        {
            oAuthProvider = new NoAuthAuthProvider(hostUrl, httpClientProvider);
        }

        return oAuthProvider;
    }
}
