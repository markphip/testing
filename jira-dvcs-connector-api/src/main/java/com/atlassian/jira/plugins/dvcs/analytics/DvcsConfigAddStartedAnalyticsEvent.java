package com.atlassian.jira.plugins.dvcs.analytics;

import com.atlassian.jira.plugins.dvcs.analytics.AnalyticsPossibleValues.DvcsType;
import com.atlassian.jira.plugins.dvcs.analytics.AnalyticsPossibleValues.Source;
import com.atlassian.jira.plugins.dvcs.analytics.AnalyticsPossibleValues.Stage;

/**
 * Analytics event to indicate that an add organization process has started.
 */
public class DvcsConfigAddStartedAnalyticsEvent extends DvcsConfigAddLifecycleAnalyticsEvent
{
    public DvcsConfigAddStartedAnalyticsEvent(Source source, DvcsType type)
    {
        super(source, Stage.STARTED, type);
    }
}
