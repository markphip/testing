package com.atlassian.jira.plugins.dvcs.spi.githubenterprise.webwork;

import static com.atlassian.jira.plugins.dvcs.analytics.DvcsConfigAddEndedAnalyticsEvent.FAILED_REASON_OAUTH_GENERIC;
import static com.atlassian.jira.plugins.dvcs.analytics.DvcsConfigAddEndedAnalyticsEvent.FAILED_REASON_OAUTH_RESPONSE;
import static com.atlassian.jira.plugins.dvcs.analytics.DvcsConfigAddEndedAnalyticsEvent.FAILED_REASON_OAUTH_SOURCECONTROL;
import static com.atlassian.jira.plugins.dvcs.analytics.DvcsConfigAddEndedAnalyticsEvent.FAILED_REASON_VALIDATION;
import static com.atlassian.jira.plugins.dvcs.spi.githubenterprise.GithubEnterpriseCommunicator.GITHUB_ENTERPRISE;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.dvcs.auth.OAuthStore;
import com.atlassian.jira.plugins.dvcs.auth.OAuthStore.Host;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException.InvalidResponseException;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.spi.github.webwork.GithubOAuthUtils;
import com.atlassian.jira.plugins.dvcs.spi.githubenterprise.GithubEnterpriseCommunicator;
import com.atlassian.jira.plugins.dvcs.util.CustomStringUtils;
import com.atlassian.jira.plugins.dvcs.util.SystemUtils;
import com.atlassian.jira.plugins.dvcs.webwork.CommonDvcsConfigurationAction;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.sal.api.ApplicationProperties;

public class AddGithubEnterpriseOrganization extends CommonDvcsConfigurationAction
{
    private static final long serialVersionUID = 3680766022095591693L;

    private final Logger log = LoggerFactory.getLogger(AddGithubEnterpriseOrganization.class);

    public static final String EVENT_TYPE_GITHUB_ENTERPRISE = "githubenterprise";

    private String organization;

    private String oauthClientIdGhe;
    private String oauthSecretGhe;

    // sent by GH on the way back
    private String code;
    private String url;

    private final OrganizationService organizationService;
    private final OAuthStore oAuthStore;
    private final ApplicationProperties applicationProperties;

    public AddGithubEnterpriseOrganization(final ApplicationProperties applicationProperties, final EventPublisher eventPublisher,
            final OAuthStore oAuthStore, final OrganizationService organizationService)
    {
        super(eventPublisher);
        this.organizationService = organizationService;
        this.oAuthStore = oAuthStore;
        this.applicationProperties = applicationProperties;
    }

    @Override
    @RequiresXsrfCheck
    protected String doExecute() throws Exception
    {
        triggerAddStartedEvent(EVENT_TYPE_GITHUB_ENTERPRISE);

        oAuthStore.store(new Host(GITHUB_ENTERPRISE, url), oauthClientIdGhe, oauthSecretGhe);

        // then continue
        return redirectUserToGithub();
    }

    GithubOAuthUtils getGithubOAuthUtils()
    {
        return new GithubOAuthUtils(applicationProperties.getBaseUrl(), oAuthStore.getClientId(GITHUB_ENTERPRISE),
                oAuthStore.getSecret(GITHUB_ENTERPRISE));
    }

    private String redirectUserToGithub()
    {
        final String githubAuthorizeUrl = getGithubOAuthUtils().createGithubRedirectUrl("AddGithubEnterpriseOrganization!finish", url,
                getXsrfToken(), organization, getAutoLinking(), getAutoSmartCommits());

        // param "t" is holding information where to redirect from
        // "wainting screen" (AddBitbucketOrganization, AddGithubOrganization
        // ...)
        return SystemUtils.getRedirect(this, githubAuthorizeUrl + urlEncode("&t=3"), true);
    }

    @Override
    protected void doValidation()
    {
        if (StringUtils.isBlank(url) || StringUtils.isBlank(organization))
        {
            addErrorMessage("Please provide both url and organization parameters.");
        }

        if (!SystemUtils.isValid(url))
        {
            addErrorMessage("Please provide valid GitHub host URL.");
        }

        if (url.endsWith("/"))
        {
            url = StringUtils.chop(url);

        }

        // TODO validation of account is disabled because of private mode
        // AccountInfo accountInfo = organizationService.getAccountInfo(url,
        // organization);
        // if (accountInfo == null)
        // {
        // addErrorMessage("Invalid user/team account.");
        // }
        if (invalidInput())
        {
            triggerAddFailedEvent(FAILED_REASON_VALIDATION);
        }
    }

    public String doFinish()
    {
        try
        {
            return doAddOrganization(getGithubOAuthUtils().requestAccessToken(url, code));
        }
        catch (final InvalidResponseException ire)
        {
            addErrorMessage(ire.getMessage() + " Possibly bug in releases of GitHub Enterprise prior to 11.10.290.");
            triggerAddFailedEvent(FAILED_REASON_OAUTH_RESPONSE);
            return INPUT;

        }
        catch (final SourceControlException sce)
        {
            addErrorMessage(sce.getMessage());
            log.warn(sce.getMessage());
            if (sce.getCause() != null)
            {
                log.warn("Caused by: " + sce.getCause().getMessage());
            }
            triggerAddFailedEvent(FAILED_REASON_OAUTH_SOURCECONTROL);
            return INPUT;

        }
        catch (final Exception e)
        {
            addErrorMessage("Error obtaining access token.");
            triggerAddFailedEvent(FAILED_REASON_OAUTH_GENERIC);
            return INPUT;
        }

    }

    private String doAddOrganization(final String accessToken)
    {
        try
        {
            final Organization newOrganization = new Organization();
            newOrganization.setName(organization);
            newOrganization.setHostUrl(url);
            newOrganization.setDvcsType(GithubEnterpriseCommunicator.GITHUB_ENTERPRISE);
            newOrganization.setAutolinkNewRepos(hadAutolinkingChecked());
            newOrganization.setCredential(new Credential(oAuthStore.getClientId(GITHUB_ENTERPRISE),
                    oAuthStore.getSecret(GITHUB_ENTERPRISE), accessToken));
            newOrganization.setSmartcommitsOnNewRepos(hadAutolinkingChecked());

            organizationService.saveAsync(newOrganization);

        }
        catch (final SourceControlException e)
        {
            addErrorMessage("Failed adding the account: [" + e.getMessage() + "]");
            log.debug("Failed adding the account: [" + e.getMessage() + "]");
            e.printStackTrace();
            triggerAddFailedEvent(FAILED_REASON_OAUTH_SOURCECONTROL);
            return INPUT;
        }

        triggerAddSucceededEvent(EVENT_TYPE_GITHUB_ENTERPRISE);
        return getRedirect("ConfigureDvcsOrganizations.jspa?atl_token=" + CustomStringUtils.encode(getXsrfToken()) + getSourceAsUrlParam());
    }

    public static String encode(final String url)
    {
        return CustomStringUtils.encode(url);
    }

    public String getCode()
    {
        return code;
    }

    public void setCode(final String code)
    {
        this.code = code;
    }

    public String getOrganization()
    {
        return organization;
    }

    public void setOrganization(final String organization)
    {
        this.organization = organization;
    }

    public String getOauthClientIdGhe()
    {
        return oAuthStore.getClientId(GITHUB_ENTERPRISE);
    }

    public void setOauthClientIdGhe(final String oauthClientIdGhe)
    {
        this.oauthClientIdGhe = oauthClientIdGhe;
    }

    public String getOauthSecretGhe()
    {
        return oAuthStore.getSecret(GITHUB_ENTERPRISE);
    }

    public void setOauthSecretGhe(final String oauthSecretGhe)
    {
        this.oauthSecretGhe = oauthSecretGhe;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(final String url)
    {
        this.url = url;
    }

    private void triggerAddFailedEvent(final String reason)
    {
        super.triggerAddFailedEvent(EVENT_TYPE_GITHUB_ENTERPRISE, reason);
    }
}