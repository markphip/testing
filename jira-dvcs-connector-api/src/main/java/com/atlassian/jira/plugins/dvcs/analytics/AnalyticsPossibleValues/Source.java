package com.atlassian.jira.plugins.dvcs.analytics.AnalyticsPossibleValues;

public enum Source
{
    UNKNOWN("unknown"),
    DEVTOOLS("devtools");

    private String source;

    Source(String source)
    {
        this.source = source;
    }

    @Override
    public String toString()
    {
        return this.source;
    }


}
