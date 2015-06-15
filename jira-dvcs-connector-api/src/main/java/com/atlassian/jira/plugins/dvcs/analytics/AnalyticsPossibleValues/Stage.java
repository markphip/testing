package com.atlassian.jira.plugins.dvcs.analytics.AnalyticsPossibleValues;

public enum Stage
{
    ENDED("ended"),
    STARTED("started");

    String stage;

    Stage(String stage)
    {
        this.stage = stage;
    }

    @Override
    public String toString()
    {
        return this.stage;
    }
}
