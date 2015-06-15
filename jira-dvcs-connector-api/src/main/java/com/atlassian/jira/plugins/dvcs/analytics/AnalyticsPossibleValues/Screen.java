package com.atlassian.jira.plugins.dvcs.analytics.AnalyticsPossibleValues;

public enum Screen
{
    SHOWN("shown");

    private String outputString;

    Screen(String outputString)
    {
        this.outputString = outputString;
    }

    @Override
    public String toString()
    {
        return outputString;
    }
}
