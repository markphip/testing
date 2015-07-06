package com.atlassian.jira.plugins.dvcs.bitbucket.access;

import com.atlassian.plugin.web.Condition;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Checks whether there is at least one Bitbucket team with default groups.
 *
 * When this condition is satisfied, we should show the Bitbucket access extension in the add user, application role defaults and JIM pages.
 */
public class BitbucketAccessExtensionCondition implements Condition
{
    private final BitbucketTeamService bitbucketTeamService;

    public BitbucketAccessExtensionCondition(BitbucketTeamService bitbucketTeamService)
    {
        this.bitbucketTeamService = checkNotNull(bitbucketTeamService);
    }

    @Override
    public void init(Map<String, String> params) {}

    @Override
    public boolean shouldDisplay(Map<String, Object> context)
    {
        return !bitbucketTeamService.getTeamsWithDefaultGroups().isEmpty();
    }
}
