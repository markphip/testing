package com.atlassian.jira.plugins.dvcs.analytics;

import com.atlassian.jira.plugins.dvcs.analytics.AnalyticsPossibleValues.DvcsType;
import com.atlassian.jira.plugins.dvcs.analytics.AnalyticsPossibleValues.Outcome;
import com.atlassian.jira.plugins.dvcs.analytics.AnalyticsPossibleValues.FailureReason;
import com.atlassian.jira.plugins.dvcs.analytics.AnalyticsPossibleValues.Source;
import com.atlassian.jira.plugins.dvcs.analytics.AnalyticsPossibleValues.Stage;

/**
 * Analytics event to indicate that an add organization process has ended.
 */
public class DvcsConfigAddEndedAnalyticsEvent extends DvcsConfigAddLifecycleAnalyticsEvent
{

    private String reason;

    public DvcsConfigAddEndedAnalyticsEvent(Source source, DvcsType type, Outcome outcome, FailureReason reason)
    {
        super(source, Stage.ENDED, type, outcome);
        if(reason != null){
            this.reason = reason.toString();
        }
    }

    public String getReason()
    {
        return reason;
    }
}
