package com.atlassian.jira.plugins.dvcs.spi.bitbucket.webwork;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.dvcs.analytics.event.DvcsType;
import com.atlassian.jira.plugins.dvcs.analytics.event.FailureReason;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.SmartCommitsAnalyticsService;
import com.atlassian.jira.plugins.dvcs.auth.OAuthStore;
import com.atlassian.jira.plugins.dvcs.auth.OAuthStore.Host;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketOAuthAuthentication;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.HttpClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.util.DebugOutputStream;
import com.atlassian.jira.plugins.dvcs.util.CustomStringUtils;
import com.atlassian.jira.plugins.dvcs.util.SystemUtils;
import com.atlassian.jira.plugins.dvcs.webwork.CommonDvcsConfigurationAction;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.ApplicationProperties;
import org.apache.commons.lang.StringUtils;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.SignatureType;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Webwork action used to configure the bitbucket organization.
 */
@Scanned
public class AddBitbucketOrganization extends CommonDvcsConfigurationAction
{
    private static final long serialVersionUID = 4366205447417138381L;
    private final static Logger log = LoggerFactory.getLogger(AddBitbucketOrganization.class);

    public static final String SESSION_KEY_REQUEST_TOKEN = "requestToken";

    private String url;
    private String organization;
    private String adminUsername;
    private String adminPassword;
    private String oauthBbClientId;
    private String oauthBbSecret;
    private String accessToken = "";

    private final OrganizationService organizationService;
    private final HttpClientProvider httpClientProvider;
    private final com.atlassian.sal.api.ApplicationProperties ap;
    private final OAuthStore oAuthStore;
    private final AddBitbucketAction actionDelegate;
    private final SmartCommitsAnalyticsService smartCommitsAnalyticsService;

    public AddBitbucketOrganization(@ComponentImport ApplicationProperties ap,
            @ComponentImport EventPublisher eventPublisher,
            OAuthStore oAuthStore,
            OrganizationService organizationService,
            HttpClientProvider httpClientProvider,
            SmartCommitsAnalyticsService smartCommitsAnalyticsService)
    {
        super(eventPublisher);
        this.ap = ap;
        this.organizationService = organizationService;
        this.oAuthStore = oAuthStore;
        this.httpClientProvider = httpClientProvider;
        this.smartCommitsAnalyticsService = smartCommitsAnalyticsService;

        if (StringUtils.isBlank(System.getProperty(BitbucketRemoteClient.BITBUCKET_TEST_URL_CONFIGURATION)))
        {
            actionDelegate = new ProductionAction();
        }
        else
        {
            actionDelegate = new TestAction();
        }
    }

    @Override
    @RequiresXsrfCheck
    protected String doExecute() throws Exception
    {
        triggerAddStartedEvent(DvcsType.BITBUCKET);
        storeLatestOAuth();
        return actionDelegate.doExecute();
    }

    private String redirectUserToBitbucket()
    {
        try
        {
            OAuthService service = createOAuthScribeService();
            Token requestToken = service.getRequestToken();
            String authUrl = service.getAuthorizationUrl(requestToken);

            request.getSession().setAttribute(SESSION_KEY_REQUEST_TOKEN, requestToken);

            return SystemUtils.getRedirect(this, authUrl, true);
        }
        catch (Exception e)
        {
            log.warn("Error redirect user to bitbucket server.", e);
            addErrorMessage("The authentication with Bitbucket has failed. Please check your OAuth settings.");
            triggerAddFailedEvent(FailureReason.OAUTH_TOKEN);
            return INPUT;
        }
    }

    OAuthService createOAuthScribeService()
    {
        // param "t" is holding information where to redirect from "wainting screen" (AddBitbucketOrganization, AddGithubOrganization ...)
        String redirectBackUrl = new StringBuilder()
                .append(ap.getBaseUrl())
                .append("/secure/admin/AddOrganizationProgressAction!default.jspa?organization=")
                .append(organization)
                .append("&autoLinking=")
                .append(getAutoLinking())
                .append("&url=")
                .append(url)
                .append("&autoSmartCommits=")
                .append(getAutoSmartCommits())
                .append("&atl_token=")
                .append(getXsrfToken())
                .append("&t=1")
                .append(getSourceAsUrlParam())
                .toString();

        return createBitbucketOAuthScribeService(redirectBackUrl);
    }

    private OAuthService createBitbucketOAuthScribeService(String callbackUrl)
    {
        ServiceBuilder sb = new ServiceBuilder().apiKey(oAuthStore.getClientId(Host.BITBUCKET.id))
                .signatureType(SignatureType.Header)
                .apiSecret(oAuthStore.getSecret(Host.BITBUCKET.id))
                .provider(new Bitbucket10aScribeApi(url, httpClientProvider))
                .debugStream(new DebugOutputStream(log));

        if (!StringUtils.isBlank(callbackUrl))
        {
            sb.callback(callbackUrl);
        }

        return sb.build();
    }

    private void storeLatestOAuth()
    {
        oAuthStore.store(Host.BITBUCKET, oauthBbClientId, oauthBbSecret);
    }

    public String doFinish()
    {
        // now get the access token
        Verifier verifier = new Verifier(request.getParameter("oauth_verifier"));
        Token requestToken = (Token) request.getSession().getAttribute(SESSION_KEY_REQUEST_TOKEN);

        if (requestToken == null)
        {
            log.debug("Request token is NULL. It has been removed in the previous attempt of adding organization. Now we will stop.");
            return getRedirect("ConfigureDvcsOrganizations.jspa?atl_token=" + CustomStringUtils.encode(getXsrfToken()));
        }

        request.getSession().removeAttribute(SESSION_KEY_REQUEST_TOKEN);

        OAuthService service = createOAuthScribeService();
        Token accessTokenObj = service.getAccessToken(requestToken, verifier);
        accessToken = BitbucketOAuthAuthentication.generateAccessTokenString(accessTokenObj);

        httpClientProvider.closeIdleConnections();

        return doAddOrganization();
    }

    private String doAddOrganization()
    {

        try
        {
            Organization newOrganization = new Organization();
            newOrganization.setName(organization);
            newOrganization.setHostUrl(url);
            newOrganization.setDvcsType(BitbucketCommunicator.BITBUCKET);
            newOrganization.setCredential(new Credential(oAuthStore.getClientId(Host.BITBUCKET.id), oAuthStore.getSecret(Host.BITBUCKET.id), accessToken));
            newOrganization.setAutolinkNewRepos(hadAutolinkingChecked());
            newOrganization.setSmartcommitsOnNewRepos(hadAutoSmartCommitsChecked());
            organizationService.save(newOrganization);
        }
        catch (SourceControlException.UnauthorisedException e)
        {
            addErrorMessage("Failed adding the account: [" + e.getMessage() + "]");
            log.debug("Failed adding the account: [" + e.getMessage() + "]");
            triggerAddFailedEvent(FailureReason.OAUTH_UNAUTH);
            return INPUT;
        }
        catch (SourceControlException e)
        {
            addErrorMessage("Failed adding the account: [" + e.getMessage() + "]");
            log.debug("Failed adding the account: [" + e.getMessage() + "]");
            triggerAddFailedEvent(FailureReason.OAUTH_SOURCECONTROL);
            return INPUT;
        }

        triggerAddSucceededEvent(DvcsType.BITBUCKET);
        smartCommitsAnalyticsService.fireNewOrganizationAddedWithSmartCommits(DvcsType.BITBUCKET, hadAutoSmartCommitsChecked());

        // go back to main DVCS configuration page
        return getRedirect("ConfigureDvcsOrganizations.jspa?atl_token=" + CustomStringUtils.encode(getXsrfToken())
                + getSourceAsUrlParam());
    }

    @Override
    protected void doValidation()
    {
        setUrlAndTokenIfTesting();

        if (StringUtils.isBlank(organization) || StringUtils.isBlank(url))
        {
            addErrorMessage("Invalid request, missing url or organization/account information.");
        }

        if (StringUtils.isNotBlank(organization))
        {
            Organization integratedAccount = organizationService.findIntegratedAccount();
            if (integratedAccount != null && organization.trim().equalsIgnoreCase(integratedAccount.getName()))
            {
                addErrorMessage("It is not possible to add the same account as the integrated one.");
            }
        }

        AccountInfo accountInfo = organizationService.getAccountInfo(url, organization, BitbucketCommunicator.BITBUCKET);
        // Bitbucket REST API to determine existence of accountInfo accepts valid email associated with BB account, but
        // it is not possible to create an account containing the '@' character.
        // [https://confluence.atlassian.com/display/BITBUCKET/account+Resource#accountResource-GETtheaccountprofile]
        if (accountInfo == null || organization.contains("@"))
        {
            addErrorMessage("Invalid user/team account.");
        }

        if (organizationService.getByHostAndName(url, organization) != null)
        {
            addErrorMessage("Account is already integrated with JIRA.");
        }

        if (invalidInput())
        {
            triggerAddFailedEvent(FailureReason.VALIDATION);
        }
    }

    private void setUrlAndTokenIfTesting()
    {
        if (StringUtils.isNotBlank(System.getProperty(BitbucketRemoteClient.BITBUCKET_TEST_URL_CONFIGURATION)))
        {
            log.info("Setting the URL for testing {}", System.getProperty(BitbucketRemoteClient.BITBUCKET_TEST_URL_CONFIGURATION));
            url = System.getProperty(BitbucketRemoteClient.BITBUCKET_TEST_URL_CONFIGURATION);
            accessToken = "oauth_verifier=2370445076&oauth_token=NpPhUdKULLszcQfNsR";
        }
    }

    public String getAdminPassword()
    {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword)
    {
        this.adminPassword = adminPassword;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public String getOrganization()
    {
        return organization;
    }

    public void setOrganization(String organization)
    {
        this.organization = organization;
    }

    public String getAdminUsername()
    {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername)
    {
        this.adminUsername = adminUsername;
    }

    public String getOauthBbClientId()
    {
        return oauthBbClientId;
    }

    public void setOauthBbClientId(String oauthBbClientId)
    {
        this.oauthBbClientId = oauthBbClientId;
    }

    public String getOauthBbSecret()
    {
        return oauthBbSecret;
    }

    public void setOauthBbSecret(String oauthBbSecret)
    {
        this.oauthBbSecret = oauthBbSecret;
    }

    private void triggerAddFailedEvent(FailureReason reason)
    {
        super.triggerAddFailedEvent(DvcsType.BITBUCKET, reason);
    }

    private interface AddBitbucketAction
    {
        String doExecute();
    }

    private final class ProductionAction implements AddBitbucketAction
    {
        public String doExecute()
        {
            return redirectUserToBitbucket();
        }
    }

    private final class TestAction implements AddBitbucketAction
    {
        public String doExecute()
        {
            return doAddOrganization();
        }
    }
}
