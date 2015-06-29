package com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event;

import com.atlassian.analytics.api.annotations.EventName;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

public class SmartCommitOperationFailedEvent
{
    private SmartCommitCommandType commandType;
    private String failureReason;

    public SmartCommitOperationFailedEvent(SmartCommitCommandType commandType, String failureReason)
    {
        this.commandType = commandType;
        this.failureReason = failureReason;
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
