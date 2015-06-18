package com.atlassian.jira.plugins.dvcs.analytics.event;

/**
 * An event to indicate that the ConfigureDvcsOrganizations page is shown.
 */
public class DvcsConfigPageShownAnalyticsEvent extends DvcsConfigAnalyticsEvent
{
    private static final String SHOWN = "shown";

    public DvcsConfigPageShownAnalyticsEvent(Source source)
    {
        super(source, SHOWN);
    }
}
