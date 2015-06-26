package com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event;

import com.atlassian.analytics.api.annotations.EventName;

public class SmartCommitOperationFailedEvent
{
    private SmartCommitCommandType commandType;
    private String failureReason;

    public SmartCommitOperationFailedEvent(SmartCommitCommandType commandType, String failureReason)
    {
        this.commandType = commandType;
        this.failureReason = failureReason;
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o) { return true; }
        if (!(o instanceof SmartCommitOperationFailedEvent)) { return false; }

        final SmartCommitOperationFailedEvent that = (SmartCommitOperationFailedEvent) o;

        if (commandType != that.commandType) { return false; }

        return failureReason.equals(that.failureReason);

    }

    @Override
    public int hashCode()
    {
        int result = commandType.hashCode();
        result = 31 * result + failureReason.hashCode();
        return result;
    }

    @EventName
    public String determineEventName()
    {
        return "jira.dvcsconnector.smartcommit." + commandType + ".failed";
    }

    public String getfailureReason()
    {
        return failureReason;
    }

}
