package com.atlassian.jira.plugins.dvcs.analytics.AnalyticsPossibleValues;

/**
 * Created by gtaylor on 1/06/15.
 */
public enum Source
{
    UNKNOWN("unknown"),
    DEVTOOLS("devtools");

    private String source;

    Source(String source){
        this.source=source;
    }


}
