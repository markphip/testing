package com.atlassian.jira.plugins.dvcs.bitbucket.access;

import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import com.google.common.annotations.VisibleForTesting;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Context provider for the Bitbucket invite message panels in JIM import user access page
 */
public class JIMBitbucketInviteMessagePanelContextProvider extends BitbucketAccessExtensionContextProvider
{
    @VisibleForTesting
    static final String REQUIRED_WEB_RESOURCE_COMPLETE_KEY = "com.atlassian.jira.plugins.jira-bitbucket-connector-plugin:jim-bitbucket-invite-message-panel-resources";

    private final PageBuilderService pageBuilderService;

    public JIMBitbucketInviteMessagePanelContextProvider(@ComponentImport ApplicationProperties applicationProperties,
            BitbucketTeamService bitbucketTeamService, @ComponentImport PageBuilderService pageBuilderService)
    {
        super(applicationProperties, bitbucketTeamService);
        this.pageBuilderService = checkNotNull(pageBuilderService);
    }

    @Override
    protected void requireResourcesAndData(final List<Organization> bitbucketTeamsWithDefaultGroups)
    {
        if (!bitbucketTeamsWithDefaultGroups.isEmpty())
        {
            pageBuilderService.assembler().resources().requireWebResource(REQUIRED_WEB_RESOURCE_COMPLETE_KEY);
        }
    }
}
