package com.atlassian.jira.plugins.dvcs.analytics;

import com.atlassian.jira.plugins.dvcs.analytics.AnalyticsPossibleValues.Source;

/**
 * An event to indicate that the ConfigureDvcsOrganizations page is shown.
 */
public class DvcsConfigPageShownAnalyticsEvent extends DvcsConfigAnalyticsEvent
{
    public static final String SHOWN = "shown";

    public DvcsConfigPageShownAnalyticsEvent(Source source)
    {
        super(source, SHOWN);
    }
}
