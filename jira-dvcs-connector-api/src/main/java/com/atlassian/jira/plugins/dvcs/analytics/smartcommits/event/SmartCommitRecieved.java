package com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event;

import com.atlassian.analytics.api.annotations.EventName;
import org.apache.commons.lang.builder.EqualsBuilder;

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
        this.time = smartCommitType.contains(SmartCommitCommandType.TIME);
        this.comment = smartCommitType.contains(SmartCommitCommandType.COMMENT);
    }

    @Override
    public boolean equals(Object o)
    {
        return EqualsBuilder.reflectionEquals(this, o);
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
}
