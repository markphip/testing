package com.atlassian.jira.plugins.dvcs.spi.bitbucket.webwork;

import com.atlassian.jira.plugins.dvcs.analytics.DvcsConfigAddEndedAnalyticsEvent;
import com.atlassian.jira.plugins.dvcs.auth.OAuthStore;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.util.DebugOutputStream;
import com.atlassian.jira.plugins.dvcs.util.SystemUtils;
import com.atlassian.sal.api.UrlMode;
import org.apache.commons.lang.StringUtils;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.SignatureType;
import org.scribe.model.Token;
import org.scribe.oauth.OAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import webwork.action.Action;

public class redirectUserToBitbucketDelegate
{
    AddBitbucketOrganization addBitbucketOrganization;
    private final static Logger log = LoggerFactory.getLogger(AddBitbucketOrganization.class);

    public redirectUserToBitbucketDelegate(AddBitbucketOrganization addBitbucketOrganization)
    {
        this.addBitbucketOrganization = addBitbucketOrganization;
    }

    private String redirectUserToBitbucket()
    {
        try
        {
            OAuthService service = createOAuthScribeService();
            Token requestToken = service.getRequestToken();
            String authUrl = service.getAuthorizationUrl(requestToken);

            addBitbucketOrganization.getRequest().getSession().setAttribute(AddBitbucketOrganization.SESSION_KEY_REQUEST_TOKEN, requestToken);

            return SystemUtils.getRedirect(addBitbucketOrganization, authUrl, true);
        }
        catch (Exception e)
        {
            log.warn("Error redirect user to bitbucket server.", e);
            addBitbucketOrganization.addErrorMessage("The authentication with Bitbucket has failed. Please check your OAuth settings.");
            addBitbucketOrganization.triggerAddFailedEvent(DvcsConfigAddEndedAnalyticsEvent.FAILED_REASON_OAUTH_TOKEN);
            return Action.INPUT;
        }
    }

    OAuthService createOAuthScribeService()
    {
        String redirectBackUrl = new StringBuilder()
                .append(addBitbucketOrganization.getAP().getBaseUrl(UrlMode.AUTO))
                .append("/secure/admin/AddOrganizationProgressAction!default.jspa?organization=")
                .append(addBitbucketOrganization.getOrganization())
                .append("&autoLinking=")
                .append(addBitbucketOrganization.getAutoLinking())
                .append("&url=")
                .append(addBitbucketOrganization.getUrl())
                .append("&autoSmartCommits=")
                .append(addBitbucketOrganization.getAutoSmartCommits())
                .append("&atl_token=")
                .append(addBitbucketOrganization.getXsrfToken())
                .append("&t=1")
                .append(addBitbucketOrganization.getSourceAsUrlParam())
                .toString();

        return createBitbucketOAuthScribeService(redirectBackUrl);
    }

    private OAuthService createBitbucketOAuthScribeService(String callbackUrl)
    {
        ServiceBuilder sb = new ServiceBuilder().apiKey(addBitbucketOrganization.getOAuthStore().getClientId(OAuthStore.Host.BITBUCKET.id))
                .signatureType(SignatureType.Header)
                .apiSecret(addBitbucketOrganization.getOAuthStore().getSecret(OAuthStore.Host.BITBUCKET.id))
                .provider(new Bitbucket10aScribeApi(addBitbucketOrganization.getUrl(), addBitbucketOrganization.getHttpClientProvider()))
                .debugStream(new DebugOutputStream(log));

        if (!StringUtils.isBlank(callbackUrl))
        {
            sb.callback(callbackUrl);
        }

        return sb.build();
    }

}
