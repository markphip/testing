package com.atlassian.jira.plugins.dvcs.analytics;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.jira.plugins.dvcs.analytics.AnalyticsPossibleValues.EventSource;
import com.atlassian.jira.plugins.dvcs.analytics.AnalyticsPossibleValues.DvcsCommitsAnalyticsEventname;

/**
 * Analytics event class to indicate actions on the issue.
 * <p>
 * Possible events are 'tabclick' when the 'Commit' tab is clicked and 'tabshowing' when the 'Commit' tab is rendered
 * and showing. Possible sources are 'issue' for tab in issue and 'agile' for Jira Agile tab.
 */

@EventName ("jira.dvcsconnector.commit.tabshowing")
public class DvcsCommitsAnalyticsEvent
{
    private final boolean authenticated;
    private final String source;

    public DvcsCommitsAnalyticsEvent(final EventSource source, final boolean authenticated)
    {
        this.authenticated = authenticated;
        this.source = source.toString();
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
