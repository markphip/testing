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
public class AddUserBitbucketAccessExtensionContextProvider implements ContextProvider
{
    /**
     * This context key points to a String that looks like: 1:developers;1:administrators;2:developers.
     * The number represents Organization ID (Bitbucket team is an Organization) and the string that follows
     * the ID represents groups that we're inviting the user to. This format is chosen to conform to existing
     * code that will do the actual inviting.
     */
    @VisibleForTesting
    static final String CONTEXT_KEY_INVITE_TO_GROUPS = "inviteToGroups";

    /**
     * This context key points to JIRA's base url. We use this so we can specify a link to the 'DVCS accounts' page.
     */
    @VisibleForTesting
    static final String CONTEXT_KEY_JIRA_BASE_URL = "jiraBaseUrl";

    /**
     * When we have more than 3 Bitbucket teams, we list the first 3 teams and wrote ", and N other". This
     * context key points to the value for N.
     */
    @VisibleForTesting
    static final String CONTEXT_KEY_MORE_COUNT = "moreCount";

    /**
     * This context key points to a collection of team names that are not displayed and will be displayed in
     * the inline dialog when the user clicks on the "N other" link.
     */
    @VisibleForTesting
    static final String CONTEXT_KEY_MORE_TEAMS = "moreTeams";

    /**
     * This context key points to the full collection of team names. We take the first three and display them.
     */
    @VisibleForTesting
    static final String CONTEXT_KEY_TEAMS_WITH_DEFAULT_GROUPS = "teamsWithDefaultGroups";

    private static final String ORGANIZATION_GROUP_PAIR_SEPARATOR = ";";

    private static final String ORGANIZATION_GROUP_SEPARATOR = ":";

    @VisibleForTesting
    static final String REQUIRED_WEB_RESOURCE_COMPLETE_KEY = "com.atlassian.jira.plugins.jira-bitbucket-connector-plugin:add-user-bitbucket-access-extension-resources";

    private static final int TEAMS_DISPLAY_THRESHOLD = 3;

    private final ApplicationProperties applicationProperties;

    private final BitbucketTeamService bitbucketTeamService;

    private final PageBuilderService pageBuilderService;

    public AddUserBitbucketAccessExtensionContextProvider(@ComponentImport ApplicationProperties applicationProperties,
            BitbucketTeamService bitbucketTeamService, @ComponentImport PageBuilderService pageBuilderService)
    {
        this.applicationProperties = checkNotNull(applicationProperties);
        this.bitbucketTeamService = checkNotNull(bitbucketTeamService);
        this.pageBuilderService = checkNotNull(pageBuilderService);
    }

    @Override
    public void init(Map<String, String> params) {}

    @Override
    public Map<String, Object> getContextMap(Map<String, Object> context)
    {
        List<Organization> bitbucketTeamsWithDefaultGroups = bitbucketTeamService.getTeamsWithDefaultGroups();
        List<String> bitbucketTeamNames = transform(bitbucketTeamsWithDefaultGroups, new Function<Organization, String>()
        {
            @Override
            public String apply(Organization organization)
            {
                return organization.getName();
            }
        });

        requireResources();
        return ImmutableMap.of(
                CONTEXT_KEY_INVITE_TO_GROUPS, inviteToGroups(bitbucketTeamsWithDefaultGroups),
                CONTEXT_KEY_JIRA_BASE_URL, applicationProperties.getString(JIRA_BASEURL),
                CONTEXT_KEY_MORE_COUNT, max(0, bitbucketTeamsWithDefaultGroups.size() - TEAMS_DISPLAY_THRESHOLD),
                CONTEXT_KEY_MORE_TEAMS, inlineDialogContent(bitbucketTeamNames),
                CONTEXT_KEY_TEAMS_WITH_DEFAULT_GROUPS, bitbucketTeamNames
        );
    }

    private void requireResources()
    {
        pageBuilderService.assembler().resources().requireWebResource(REQUIRED_WEB_RESOURCE_COMPLETE_KEY);
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
