package com.atlassian.jira.plugins.dvcs.spi.githubenterprise;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.auth.OAuthStore;
import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.service.ChangesetCache;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubCommunicator;

public class GithubEnterpriseCommunicator extends GithubCommunicator
{
    private static final Logger log = LoggerFactory.getLogger(GithubEnterpriseCommunicator.class);
    public static final String GITHUB_ENTERPRISE = "githube";

    private GithubEnterpriseCommunicator(ChangesetCache changesetCache, OAuthStore oAuthStore,
            GithubClientProvider githubClientProvider)
    {
        super(changesetCache, oAuthStore, githubClientProvider);
    }
        
    @Override
    public AccountInfo getAccountInfo(String hostUrl, String accountName)
    {
        UserService userService = new UserService(githubClientProvider.createClient(hostUrl));
        try
        {
            userService.getUser(accountName);
            return new AccountInfo(GithubCommunicator.GITHUB, !isOauthConfigured());
        } catch (RequestException e)
        {
            log.debug("Unable to retrieve account information. hostUrl: {}, account: {} " + e.getMessage(),
                    hostUrl, accountName);

            // GitHub Enterprise returns a 403 status for unauthorized requests.
            if (e.getStatus() == 403)
            {
                return new AccountInfo(GithubCommunicator.GITHUB, !isOauthConfigured());
            }

        } catch (IOException e)
        {
            log.debug("Unable to retrieve account information. hostUrl: {}, account: {} " + e.getMessage(),
                    hostUrl, accountName);
        }
        return null;
    }

    @Override
    public boolean isOauthConfigured()
    {
        return StringUtils.isNotBlank(oAuthStore.getUrl(GITHUB_ENTERPRISE))
                && StringUtils.isNotBlank(oAuthStore.getClientId(GITHUB_ENTERPRISE))
                && StringUtils.isNotBlank(oAuthStore.getSecret(GITHUB_ENTERPRISE));
    }
    
    @Override
    public String getDvcsType()
    {
        return GITHUB_ENTERPRISE;
    }
}
