package com.atlassian.jira.plugins.dvcs.bitbucket.access;

import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.plugin.web.ContextProvider;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.Gson;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Math.max;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public class AddUserBitbucketAccessExtensionContextProvider implements ContextProvider
{
    @VisibleForTesting
    static final String CONTEXT_KEY_INVITE_TO_GROUPS = "inviteToGroups";

    @VisibleForTesting
    static final String CONTEXT_KEY_MORE_COUNT = "moreCount";

    @VisibleForTesting
    static final String CONTEXT_KEY_TEAMS_WITH_DEFAULT_GROUPS = "teamsWithDefaultGroups";

    private static final String ORGANIZATION_GROUP_PAIR_SEPARATOR = ";";

    private static final String ORGANIZATION_GROUP_SEPARATOR = ":";

    @VisibleForTesting
    static final String REQUIRED_DATA_KEY = "bitbucket-access-inline-dialog-content";

    @VisibleForTesting
    static final String REQUIRED_WEB_RESOURCE_COMPLETE_KEY = "com.atlassian.jira.plugins.jira-bitbucket-connector-plugin:add-user-bitbucket-access-extension-resources";

    private static final int TEAMS_DISPLAY_THRESHOLD = 3;

    private final BitbucketTeamService bitbucketTeamService;

    private final PageBuilderService pageBuilderService;

    public AddUserBitbucketAccessExtensionContextProvider(BitbucketTeamService bitbucketTeamService,
            PageBuilderService pageBuilderService)
    {
        this.bitbucketTeamService = checkNotNull(bitbucketTeamService);
        this.pageBuilderService = checkNotNull(pageBuilderService);
    }

    @Override
    public void init(Map<String, String> params) {}

    @Override
    public Map<String, Object> getContextMap(Map<String, Object> context)
    {
        List<Organization> bitbucketTeamsWithDefaultGroups = bitbucketTeamService.getTeamsWithDefaultGroups();
        List<String> bitbucketTeamNames = bitbucketTeamsWithDefaultGroups.stream()
                .map(Organization::getName)
                .collect(toList());

        requireResourcesAndData(bitbucketTeamNames);
        return ImmutableMap.of(
                CONTEXT_KEY_INVITE_TO_GROUPS, inviteToGroups(bitbucketTeamsWithDefaultGroups),
                CONTEXT_KEY_MORE_COUNT, max(0, bitbucketTeamsWithDefaultGroups.size() - TEAMS_DISPLAY_THRESHOLD),
                CONTEXT_KEY_TEAMS_WITH_DEFAULT_GROUPS, bitbucketTeamNames
        );
    }

    private void requireResourcesAndData(List<String> bitbucketTeamNames)
    {
        pageBuilderService.assembler().resources().requireWebResource(REQUIRED_WEB_RESOURCE_COMPLETE_KEY);

        List<String> inlineDialogContent = inlineDialogContent(bitbucketTeamNames);
        if (!inlineDialogContent.isEmpty())
        {
            pageBuilderService.assembler().data().requireData(REQUIRED_DATA_KEY,
                    writer -> writer.write(new Gson().toJson(inlineDialogContent)));
        }
    }

    private List<String> inlineDialogContent(List<String> bitbucketTeamNames)
    {
        if(bitbucketTeamNames.size() > TEAMS_DISPLAY_THRESHOLD)
        {
            return bitbucketTeamNames.subList(TEAMS_DISPLAY_THRESHOLD, bitbucketTeamNames.size());
        }

        return emptyList();
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

        return Joiner.on(ORGANIZATION_GROUP_PAIR_SEPARATOR).join(organizationGroupPairs);
    }
}
