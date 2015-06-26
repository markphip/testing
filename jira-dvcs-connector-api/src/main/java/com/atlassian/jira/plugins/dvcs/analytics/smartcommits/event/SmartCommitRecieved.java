package com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event;

import com.atlassian.analytics.api.annotations.EventName;
import org.apache.commons.lang.builder.EqualsBuilder;

import java.util.Set;

@EventName ("jira.dvcsconnector.smartcommit.received")
public class SmartCommitRecieved
{
    private boolean transition;
    private boolean worklog;
    private boolean comment;

    public SmartCommitRecieved(Set<SmartCommitCommandType> smartCommitType)
    {
        this.transition = smartCommitType.contains(SmartCommitCommandType.TRANSITION);
        worklog = smartCommitType.contains(SmartCommitCommandType.WORKLOG);
        comment = smartCommitType.contains(SmartCommitCommandType.COMMENT);
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

    public boolean isWorklog()
    {
        return worklog;
    }

    public boolean isTransition()
    {
        return transition;
    }
}
