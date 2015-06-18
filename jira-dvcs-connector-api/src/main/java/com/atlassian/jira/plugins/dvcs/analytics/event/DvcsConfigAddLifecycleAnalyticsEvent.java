package com.atlassian.jira.plugins.dvcs.analytics.event;

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
