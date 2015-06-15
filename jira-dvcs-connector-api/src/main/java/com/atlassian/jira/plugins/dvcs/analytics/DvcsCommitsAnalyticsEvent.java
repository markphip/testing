package com.atlassian.jira.plugins.dvcs.analytics;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.jira.plugins.dvcs.analytics.AnalyticsPossibleValues.DvcsCommitsAnalyticsEventSource;
import com.atlassian.jira.plugins.dvcs.analytics.AnalyticsPossibleValues.DvcsCommitsAnalyticsEventname;

/**
 * Analytics event class to indicate actions on the issue.
 * <p>
 * Possible events are 'tabclick' when the 'Commit' tab is clicked and 'tabshowing' when the 'Commit' tab is rendered
 * and showing. Possible sources are 'issue' for tab in issue and 'agile' for Jira Agile tab.
 */
public class DvcsCommitsAnalyticsEvent
{

    private final String eventName;
    private final boolean authenticated;
    private final String source;

    public DvcsCommitsAnalyticsEvent(final DvcsCommitsAnalyticsEventSource source, final DvcsCommitsAnalyticsEventname eventName, final boolean authenticated)
    {
        this.eventName = eventName.toString();
        this.authenticated = authenticated;
        this.source = source.toString();
    }

    @EventName
    public String determineEventName()
    {
        return "jira.dvcsconnector.commit." + eventName;
    }

    public boolean isAuthenticated()
    {
        return authenticated;
    }

    public String getSource()
    {
        return source;
    }
}
