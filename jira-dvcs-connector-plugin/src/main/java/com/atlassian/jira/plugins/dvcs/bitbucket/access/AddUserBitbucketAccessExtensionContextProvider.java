package com.atlassian.jira.plugins.dvcs.bitbucket.access;

import com.atlassian.jira.compatibility.bridge.application.ApplicationRoleManagerBridge;
import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.plugin.web.ContextProvider;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Map;

import static com.atlassian.jira.plugins.dvcs.ApplicationKey.SOFTWARE;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.Math.max;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public class AddUserBitbucketAccessExtensionContextProvider implements ContextProvider
{
    @VisibleForTesting
    static final String CONTEXT_KEY_INVITE_TO_GROUPS = "inviteToGroups";

    @VisibleForTesting
    static final String CONTEXT_KEY_INVITE_USER_BY_DEFAULT = "inviteUserByDefault";

    @VisibleForTesting
    static final String CONTEXT_KEY_MORE_COUNT = "moreCount";

    @VisibleForTesting
    static final String CONTEXT_KEY_MORE_TEAMS = "moreTeams";

    static final String CONTEXT_KEY_TEAMS_WITH_DEFAULT_GROUPS = "teamsWithDefaultGroups";

    private static final String ORGANIZATION_GROUP_PAIR_SEPARATOR = ";";

    private static final String ORGANIZATION_GROUP_SEPARATOR = ":";

    private static final int TEAMS_DISPLAY_THRESHOLD = 3;

    @VisibleForTesting
    static final String REQUIRED_WEB_RESOURCE_COMPLETE_KEY = "com.atlassian.jira.plugins.jira-bitbucket-connector-plugin:add-user-bitbucket-access-extension-resources";

    private final ApplicationRoleManagerBridge applicationRoleManagerBridge;

    private final BitbucketTeamService bitbucketTeamService;

    private final PageBuilderService pageBuilderService;

    public AddUserBitbucketAccessExtensionContextProvider(ApplicationRoleManagerBridge applicationRoleManagerBridge,
            BitbucketTeamService bitbucketTeamService, PageBuilderService pageBuilderService)
    {
        this.applicationRoleManagerBridge = checkNotNull(applicationRoleManagerBridge);
        this.bitbucketTeamService = checkNotNull(bitbucketTeamService);
        this.pageBuilderService = checkNotNull(pageBuilderService);
    }

    @Override
    public void init(Map<String, String> params) {}

    @Override
    public Map<String, Object> getContextMap(Map<String, Object> context)
    {
        requireResources();

        List<Organization> bitbucketTeamsWithDefaultGroups = bitbucketTeamService.getTeamsWithDefaultGroups();
        List<String> bitbucketTeamNames = bitbucketTeamsWithDefaultGroups.stream()
                .map(o -> o.getName())
                .collect(toList());

        return ImmutableMap.of(
                CONTEXT_KEY_INVITE_TO_GROUPS, inviteToGroups(bitbucketTeamsWithDefaultGroups),
                CONTEXT_KEY_INVITE_USER_BY_DEFAULT, inviteUserByDefault(),
                CONTEXT_KEY_MORE_COUNT, max(0, bitbucketTeamsWithDefaultGroups.size() - TEAMS_DISPLAY_THRESHOLD),
                CONTEXT_KEY_MORE_TEAMS, inlineDialogContent(bitbucketTeamNames),
                CONTEXT_KEY_TEAMS_WITH_DEFAULT_GROUPS, bitbucketTeamNames
        );
    }

    private void requireResources()
    {
        pageBuilderService.assembler().resources().requireWebResource(REQUIRED_WEB_RESOURCE_COMPLETE_KEY);
    }

    private String inviteToGroups(List<Organization> organizations)
    {
        List<String> organizationGroupPairs = Lists.newArrayList();
        for (Organization organization : organizations)
        {
            for (Group group : organization.getDefaultGroups())
            {
                organizationGroupPairs.add(organization.getId() + ORGANIZATION_GROUP_SEPARATOR + group.getSlug());
            }
        }

        String result = Joiner.on(ORGANIZATION_GROUP_PAIR_SEPARATOR).join(organizationGroupPairs);
        if (result.isEmpty())
        {
            // Setting to a blank String that is not empty so that Crowd will not store this as null.
            // See: https://sdog.jira.com/browse/BBC-432
            result = " ";
        }

        return result;
    }

    private boolean inviteUserByDefault()
    {
        checkState(applicationRoleManagerBridge.isBridgeActive(), "Expecting ApplicationRoleManagerBridge to be active.");
        checkState(applicationRoleManagerBridge.rolesEnabled(), "Expecting application roles to be enabled.");

        return applicationRoleManagerBridge.getDefaultApplicationKeys().contains(SOFTWARE);
    }

    private List<String> inlineDialogContent(List<String> bitbucketTeamNames)
    {
        if(bitbucketTeamNames.size() > TEAMS_DISPLAY_THRESHOLD)
        {
            return bitbucketTeamNames.subList(TEAMS_DISPLAY_THRESHOLD, bitbucketTeamNames.size());
        }

        return emptyList();
    }
}
