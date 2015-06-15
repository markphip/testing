package com.atlassian.jira.plugins.dvcs.analytics.AnalyticsPossibleValues;

public enum Outcome
{
    FAILED("failed"),
    SUCCEEDED("succeeded");

    private String analyticsName;

    Outcome(String analyticsName)
    {
        this.analyticsName = analyticsName;
    }

    @Override
    public String toString()
    {
        return this.analyticsName;
    }

}
