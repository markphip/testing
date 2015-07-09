package com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event;

import com.atlassian.analytics.api.annotations.EventName;
import com.google.common.base.Preconditions;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

public class SmartCommitOperationFailedEvent
{
    private final SmartCommitCommandType commandType;
    private final String failureReason;

    public SmartCommitOperationFailedEvent(SmartCommitCommandType commandType, SmartCommitFailure failureReason)
    {
        Preconditions.checkNotNull(commandType);
        Preconditions.checkNotNull(failureReason);
        this.commandType = commandType;
        this.failureReason = failureReason.toString();
    }

    @EventName
    public String determineEventName()
    {
        return  String.format("jira.dvcsconnector.smartcommit.%s.failed", commandType);
    }

    public String getfailureReason()
    {
        return failureReason;
    }

    @Override
    public boolean equals(Object o)
    {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode()
    {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this);
    }

}
