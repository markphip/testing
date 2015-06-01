package com.atlassian.jira.plugins.dvcs.spi.bitbucket.webwork;

import com.atlassian.jira.plugins.dvcs.auth.OAuthStore;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator;
import com.atlassian.jira.plugins.dvcs.util.CustomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import webwork.action.Action;

import static com.atlassian.jira.plugins.dvcs.analytics.DvcsConfigAddEndedAnalyticsEvent.FAILED_REASON_OAUTH_SOURCECONTROL;
import static com.atlassian.jira.plugins.dvcs.analytics.DvcsConfigAddEndedAnalyticsEvent.FAILED_REASON_OAUTH_UNAUTH;

/**
 * Created by gtaylor on 1/06/15.
 */
public class DoAddOrganizationAction implements DoExecuteAction
{
    AddBitbucketOrganization addBitbucketOrganization;
    OrganizationService organizationService;
    private final static Logger log = LoggerFactory.getLogger(AddBitbucketOrganization.class);

    public DoAddOrganizationAction(AddBitbucketOrganization addBitbucketOrganization)
    {
        this.addBitbucketOrganization = addBitbucketOrganization;
    }

    public String doExecute(){
        return doAddOrganization();
    }

    private String doAddOrganization()
    {
        try
        {
            Organization newOrganization = new Organization();
            newOrganization.setName(addBitbucketOrganization.getOrganization());
            newOrganization.setHostUrl(addBitbucketOrganization.getUrl());
            newOrganization.setDvcsType(BitbucketCommunicator.BITBUCKET);
            newOrganization.setCredential(new Credential(addBitbucketOrganization.getOAuthStore().getClientId(OAuthStore.Host.BITBUCKET.id),
                    addBitbucketOrganization.getOAuthStore().getSecret(OAuthStore.Host.BITBUCKET.id),
                    addBitbucketOrganization.getAccessToken()));
            newOrganization.setAutolinkNewRepos(addBitbucketOrganization.hadAutolinkingChecked());
            newOrganization.setSmartcommitsOnNewRepos(addBitbucketOrganization.hadAutoSmartCommitsChecked());
            organizationService.save(newOrganization);
        }
        catch (SourceControlException.UnauthorisedException e)
        {
            addBitbucketOrganization.addErrorMessage("Failed adding the account: [" + e.getMessage() + "]");
            log.debug("Failed adding the account: [" + e.getMessage() + "]");
            addBitbucketOrganization.triggerAddFailedEvent(FAILED_REASON_OAUTH_UNAUTH);
            return Action.INPUT;
        }
        catch (SourceControlException e)
        {
            addBitbucketOrganization.addErrorMessage("Failed adding the account: [" + e.getMessage() + "]");
            log.debug("Failed adding the account: [" + e.getMessage() + "]");
            addBitbucketOrganization.triggerAddFailedEvent(FAILED_REASON_OAUTH_SOURCECONTROL);
            return Action.INPUT;
        }

        //triggerAddSucceededEvent(EVENT_TYPE_BITBUCKET);

        // go back to main DVCS configuration page
        return addBitbucketOrganization.getRedirect("ConfigureDvcsOrganizations.jspa?atl_token=" + CustomStringUtils.encode(addBitbucketOrganization.getXsrfToken())
                + addBitbucketOrganization.getSourceAsUrlParam());
    }

}
