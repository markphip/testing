package com.atlassian.jira.plugins.dvcs.analytics.event;

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
