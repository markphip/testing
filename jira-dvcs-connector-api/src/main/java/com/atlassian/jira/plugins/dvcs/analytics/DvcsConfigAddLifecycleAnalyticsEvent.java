package com.atlassian.jira.plugins.dvcs.analytics;

import com.atlassian.jira.plugins.dvcs.analytics.AnalyticsPossibleValues.DvcsType;
import com.atlassian.jira.plugins.dvcs.analytics.AnalyticsPossibleValues.Outcome;
import com.atlassian.jira.plugins.dvcs.analytics.AnalyticsPossibleValues.Source;
import com.atlassian.jira.plugins.dvcs.analytics.AnalyticsPossibleValues.Stage;

/**
 * Analytics event to indicate that an add organization process has entered into a stage in the lifecycle.
 */
public class DvcsConfigAddLifecycleAnalyticsEvent extends DvcsConfigAnalyticsEvent
{
    public DvcsConfigAddLifecycleAnalyticsEvent(Source source, Stage stage, DvcsType type)
    {
        super(source, "add." + type + "." + stage);
    }

    public DvcsConfigAddLifecycleAnalyticsEvent(Source source, Stage stage, DvcsType type, Outcome outcome)
    {
        super(source, "add." + type + "." + stage + "." + outcome);
    }
}
