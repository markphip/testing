package com.atlassian.jira.plugins.dvcs.bitbucket.access;

import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import com.google.common.annotations.VisibleForTesting;

import java.util.List;

/**
 * Context provider for the Bitbucket invite message panels in application access defaults
 */
public class BitbucketInviteMessagePanelContextProvider extends BaseBitbucketAccessExtensionContextProvider
{
    @VisibleForTesting
    static final String REQUIRED_WEB_RESOURCE_COMPLETE_KEY = "com.atlassian.jira.plugins.jira-bitbucket-connector-plugin:bitbucket-invite-message-panel-resources";

    public BitbucketInviteMessagePanelContextProvider(@ComponentImport ApplicationProperties applicationProperties,
            BitbucketTeamService bitbucketTeamService, @ComponentImport PageBuilderService pageBuilderService)
    {
        super(applicationProperties, bitbucketTeamService, pageBuilderService);
    }

    protected void requireResourcesAndData(List<Organization> bitbucketTeamsWithDefaultGroups)
    {
        if (!bitbucketTeamsWithDefaultGroups.isEmpty())
        {
            pageBuilderService.assembler().resources().requireWebResource(REQUIRED_WEB_RESOURCE_COMPLETE_KEY);
        }
    }
}
