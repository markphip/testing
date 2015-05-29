package com.atlassian.jira.plugins.dvcs.analytics.AnalyticsPossibleValues;

/**
 * Created by gtaylor on 29/05/15.
 */
public enum DvcsCommitsAnalyticsEventSource
{
    ISSUE("issue"),
    AGILE("agile");

    final String eventSource;
    DvcsCommitsAnalyticsEventSource(String eventSource){
        this.eventSource = eventSource;
    }

    @Override
    public String toString(){
        return eventSource;
    }

}
