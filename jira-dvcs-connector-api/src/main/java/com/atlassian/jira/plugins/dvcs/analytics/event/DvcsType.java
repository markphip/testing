package com.atlassian.jira.plugins.dvcs.analytics.event;

public enum DvcsType
{
    BITBUCKET("bitbucket"),
    GITHUB("github"),
    GITHUB_ENTERPRISE("githubenterprise");

    final String dvcsType;

    DvcsType(String type)
    {
        this.dvcsType = type;
    }

    @Override
    public String toString()
    {
        return dvcsType;
    }

}
