package com.atlassian.jira.plugins.dvcs.bitbucket.access;

import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugin.web.ContextProvider;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Map;

import static com.atlassian.jira.config.properties.APKeys.JIRA_BASEURL;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.transform;
import static java.lang.Math.max;
import static java.util.Collections.emptyList;

/**
 * Context provider for the Bitbucket access extension on the application access
 */
public class BitbucketInviteMessagePanelContextProvider extends BitbucketAccessExtensionContextProvider
{
    @VisibleForTesting
    static final String REQUIRED_WEB_RESOURCE_COMPLETE_KEY = "com.atlassian.jira.plugins.jira-bitbucket-connector-plugin:bitbucket-invite-message-panel-resources";

    private final PageBuilderService pageBuilderService;

    public BitbucketInviteMessagePanelContextProvider(@ComponentImport ApplicationProperties applicationProperties,
            BitbucketTeamService bitbucketTeamService, @ComponentImport PageBuilderService pageBuilderService)
    {
        super(applicationProperties, bitbucketTeamService);
        this.pageBuilderService = checkNotNull(pageBuilderService);
    }

    @Override
    public void init(Map<String, String> params) {}

    protected void requireResourcesAndData(List<Organization> bitbucketTeamsWithDefaultGroups)
    {
        if (bitbucketTeamsWithDefaultGroups.isEmpty())
        {
           return;
        }

        pageBuilderService.assembler().resources().requireWebResource(REQUIRED_WEB_RESOURCE_COMPLETE_KEY);
    }
}
