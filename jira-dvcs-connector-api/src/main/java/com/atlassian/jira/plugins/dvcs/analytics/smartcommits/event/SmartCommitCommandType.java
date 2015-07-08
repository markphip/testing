package com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event;

public enum SmartCommitCommandType
{

    TRANSITION("transition"),
    LOG_WORK("time"),
    COMMENT("comment");

    private String smartCommitCommandType;

    SmartCommitCommandType(String smartCommitCommandType)
    {
        this.smartCommitCommandType = smartCommitCommandType;
    }

    @Override
    public String toString()
    {
        return smartCommitCommandType;
    }
}
