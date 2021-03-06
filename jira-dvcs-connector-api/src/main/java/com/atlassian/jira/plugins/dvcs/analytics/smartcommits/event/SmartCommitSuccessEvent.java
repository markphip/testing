package com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event;

import com.atlassian.analytics.api.annotations.EventName;
import com.google.common.base.Preconditions;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.Set;


@EventName ("jira.dvcsconnector.smartcommit.success")
public class SmartCommitSuccessEvent
{
    final private boolean transition;
    final private boolean time;
    final private boolean comment;

    public SmartCommitSuccessEvent(Set<SmartCommitCommandType> smartCommitType)
    {
        Preconditions.checkNotNull(smartCommitType);
        this.transition = smartCommitType.contains(SmartCommitCommandType.TRANSITION);
        this.time = smartCommitType.contains(SmartCommitCommandType.LOG_WORK);
        this.comment = smartCommitType.contains(SmartCommitCommandType.COMMENT);
    }

    public boolean isTransition()
    {
        return transition;
    }

    public boolean isTime()
    {
        return time;
    }

    public boolean isComment()
    {
        return comment;
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
