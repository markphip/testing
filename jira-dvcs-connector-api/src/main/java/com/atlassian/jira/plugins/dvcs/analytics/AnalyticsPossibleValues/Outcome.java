package com.atlassian.jira.plugins.dvcs.analytics.AnalyticsPossibleValues;

/**
 * Created by gtaylor on 29/05/15.
 */
public enum Outcome implements AnalyticsEventNameExtras
{
    FAILED ("failed"),
    SUCCEEDED ("succeeded");

    private String analyticsName;

    Outcome(String analyticsName){
        this.analyticsName = analyticsName;
    }

    @Override
    public String toString(){
        return this.analyticsName;
    }

}
