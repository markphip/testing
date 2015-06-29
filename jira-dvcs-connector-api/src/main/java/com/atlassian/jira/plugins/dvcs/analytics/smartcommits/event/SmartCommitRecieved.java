package com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event;

import com.atlassian.analytics.api.annotations.EventName;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.Set;

@EventName ("jira.dvcsconnector.smartcommit.received")
public class SmartCommitRecieved
{
    private boolean transition;
    private boolean time;
    private boolean comment;

    public SmartCommitRecieved(Set<SmartCommitCommandType> smartCommitType)
    {
        this.transition = smartCommitType.contains(SmartCommitCommandType.TRANSITION);
        time = smartCommitType.contains(SmartCommitCommandType.TIME);
        comment = smartCommitType.contains(SmartCommitCommandType.COMMENT);
    }

    public boolean isComment()
    {
        return comment;
    }

    public boolean isTime()
    {
        return time;
    }

    public boolean isTransition()
    {
        return transition;
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
