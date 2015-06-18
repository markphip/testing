package com.atlassian.jira.plugins.dvcs.analytics;

/**
 * A service for handling the creation and publication of analytics events
 */
public interface AnalyticsService
{
    /**
     * Publishes an analytics event signalling that the number of enabled invite groups has changed
     *
     * @param inviteGroupsEnabled the number of invite groups now enabled, 0 if all are disabled
     */
    public void publishInviteGroupChange(int inviteGroupsEnabled);

    /**
     * Publishes an analytics event signalling that a user has been created with a bitbucket invite
     */
    public void publishUserCreatedThatHasInvite();

    /**
     * Publishes an analytics event signalling that a user invite has been sent
     */
    public void publishInviteSent();

}
