package com.atlassian.jira.plugins.dvcs.analytics.event;

import com.atlassian.analytics.api.annotations.EventName;

/**
 * Analytics event to indicate that a newly created Jira user has been invited to Bitbucket.
 */
@EventName ("jira.dvcsconnector.bitbucketinvitation.adduser")
public class DvcsAddUserAnalyticsEvent
{

}
