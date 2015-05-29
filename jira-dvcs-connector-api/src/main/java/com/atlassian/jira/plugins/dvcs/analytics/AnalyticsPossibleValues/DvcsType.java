package com.atlassian.jira.plugins.dvcs.analytics.AnalyticsPossibleValues;

/**
 * Created by gtaylor on 29/05/15.
 */
public enum DvcsType
{
    BITBUCKET("bitbucket"),
    GITHUB("github"),
    GITHUB_ENTERPRISE("githubenterprise");

    final String type;

    DvcsType(String type){
        this.type = type;
    }

    @Override
    public String toString(){
        return(type);
    }

}
