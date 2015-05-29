package com.atlassian.jira.plugins.dvcs.analytics.AnalyticsPossibleValues;

/**
 * Created by gtaylor on 29/05/15.
 */
public enum Stage
{
    ENDED("ended"),
    STARTED("started");

    String stage;

    Stage(String stage){
        this.stage = stage;
    }
}
