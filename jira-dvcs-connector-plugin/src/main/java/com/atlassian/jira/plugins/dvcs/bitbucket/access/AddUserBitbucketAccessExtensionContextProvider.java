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
 * Context provider for the Bitbucket access extension on the add user page.
 */
public class AddUserBitbucketAccessExtensionContextProvider extends BitbucketAccessExtensionContextProvider
{
    private static final String ORGANIZATION_GROUP_PAIR_SEPARATOR = ";";

    private static final String ORGANIZATION_GROUP_SEPARATOR = ":";

    @VisibleForTesting
    static final String REQUIRED_DATA_BITBUCKET_INVITE_TO_GROUPS_KEY = "bitbucket-invite-to-groups";

    @VisibleForTesting
    static final String REQUIRED_WEB_RESOURCE_COMPLETE_KEY = "com.atlassian.jira.plugins.jira-bitbucket-connector-plugin:add-user-bitbucket-access-extension-resources";


    public AddUserBitbucketAccessExtensionContextProvider(@ComponentImport ApplicationProperties applicationProperties,
            BitbucketTeamService bitbucketTeamService, @ComponentImport PageBuilderService pageBuilderService)
    {
        super(applicationProperties, bitbucketTeamService, pageBuilderService);
    }

    @Override
    public void init(Map<String, String> params) {}

    protected void requireResourcesAndData(final List<Organization> bitbucketTeamsWithDefaultGroups)
    {
        String inviteToGroups = inviteToGroups(bitbucketTeamsWithDefaultGroups);
        pageBuilderService.assembler().resources().requireWebResource(REQUIRED_WEB_RESOURCE_COMPLETE_KEY);
        pageBuilderService.assembler().data().requireData(REQUIRED_DATA_BITBUCKET_INVITE_TO_GROUPS_KEY, inviteToGroups);
    }

    private String inviteToGroups(List<Organization> organizations)
    {
        final List<String> organizationGroupPairs = Lists.newArrayList();
        for (Organization organization : organizations)
        {
            for (Group group : organization.getDefaultGroups())
            {
                organizationGroupPairs.add(organization.getId() + ORGANIZATION_GROUP_SEPARATOR + group.getSlug());
            }
        }

        return Joiner.on(ORGANIZATION_GROUP_PAIR_SEPARATOR).join(organizationGroupPairs);
    }
}
