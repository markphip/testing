package com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event;

import com.atlassian.analytics.api.annotations.EventName;

import java.util.Set;


@EventName ("jira.dvcsconnector.smartcommit.success")
public class SmartCommitSuccessEvent
{
    private boolean transition;
    private boolean time;
    private boolean comment;

    public SmartCommitSuccessEvent(Set<SmartCommitCommandType> smartCommitType)
    {
        this.transition = smartCommitType.contains(SmartCommitCommandType.TRANSITION);
        this.time = smartCommitType.contains(SmartCommitCommandType.TIME);
        this.comment = smartCommitType.contains(SmartCommitCommandType.COMMENT);
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o) { return true; }
        if (!(o instanceof SmartCommitSuccessEvent)) { return false; }

        final SmartCommitSuccessEvent that = (SmartCommitSuccessEvent) o;

        if (transition != that.transition) { return false; }
        if (time != that.time) { return false; }
        return comment == that.comment;

    }

    @Override
    public int hashCode()
    {
        int result = (transition ? 1 : 0);
        result = 31 * result + (time ? 1 : 0);
        result = 31 * result + (comment ? 1 : 0);
        return result;
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
}
