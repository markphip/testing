package com.atlassian.jira.plugins.dvcs.bitbucket.access;

import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.plugin.web.ContextProvider;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;

import static com.atlassian.jira.config.properties.APKeys.JIRA_BASEURL;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.transform;
import static java.lang.Math.max;
import static java.util.Collections.emptyList;

/**
 * Base class for bitbucket access extension context providers
 */
public abstract class BaseBitbucketAccessExtensionContextProvider implements ContextProvider
{
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

    private static final int TEAMS_DISPLAY_THRESHOLD = 3;

    private final ApplicationProperties applicationProperties;

    private final BitbucketTeamService bitbucketTeamService;

    protected final PageBuilderService pageBuilderService;

    public BaseBitbucketAccessExtensionContextProvider(final ApplicationProperties applicationProperties,
            final BitbucketTeamService bitbucketTeamService, final PageBuilderService pageBuilderService)
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
        requireResourcesAndData(bitbucketTeamsWithDefaultGroups);

        return ImmutableMap.of(
                CONTEXT_KEY_JIRA_BASE_URL, applicationProperties.getString(JIRA_BASEURL),
                CONTEXT_KEY_MORE_COUNT, max(0, bitbucketTeamsWithDefaultGroups.size() - TEAMS_DISPLAY_THRESHOLD),
                CONTEXT_KEY_MORE_TEAMS, inlineDialogContent(bitbucketTeamNames),
                CONTEXT_KEY_TEAMS_WITH_DEFAULT_GROUPS, bitbucketTeamNames
        );
    }

    protected abstract void requireResourcesAndData(final List<Organization> bitbucketTeamsWithDefaultGroups);

    private List<String> inlineDialogContent(final List<String> bitbucketTeamNames)
    {
        if (bitbucketTeamNames.size() > TEAMS_DISPLAY_THRESHOLD)
        {
            return bitbucketTeamNames.subList(TEAMS_DISPLAY_THRESHOLD, bitbucketTeamNames.size());
        }

        return emptyList();
    }
}
