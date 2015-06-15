package com.atlassian.jira.plugins.dvcs.analytics;

        import com.atlassian.analytics.api.annotations.EventName;

/**
 * Analytics event to indicate that the number of invite groups enable has changed.
 */
@EventName ("jira.dvcsconnector.bitbucketinvitation.group.updated")
public class DvcsInviteGroupChanged
{
    private int groupsEnabled;

    public DvcsInviteGroupChanged(int groupsEnabled)
    {
        this.groupsEnabled = groupsEnabled;
    }

    public int getGroupsEnabled()
    {
        return groupsEnabled;
    }
}
