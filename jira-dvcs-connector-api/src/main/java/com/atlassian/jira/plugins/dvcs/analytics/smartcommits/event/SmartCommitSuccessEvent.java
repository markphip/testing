package com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event;

import com.atlassian.analytics.api.annotations.EventName;

import java.util.Set;


@EventName ("jira.dvcsconnector.smartcommit.success")
public class SmartCommitSuccessEvent
{
    private boolean transition;
    private boolean worklog;
    private boolean comment;

    public SmartCommitSuccessEvent(Set<SmartCommitCommandType> smartCommitType)
    {
        this.transition = smartCommitType.contains(SmartCommitCommandType.TRANSITION);
        this.worklog = smartCommitType.contains(SmartCommitCommandType.WORKLOG);
        this.comment = smartCommitType.contains(SmartCommitCommandType.COMMENT);
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o) { return true; }
        if (!(o instanceof SmartCommitSuccessEvent)) { return false; }

        final SmartCommitSuccessEvent that = (SmartCommitSuccessEvent) o;

        if (transition != that.transition) { return false; }
        if (worklog != that.worklog) { return false; }
        return comment == that.comment;

    }

    @Override
    public int hashCode()
    {
        int result = (transition ? 1 : 0);
        result = 31 * result + (worklog ? 1 : 0);
        result = 31 * result + (comment ? 1 : 0);
        return result;
    }

    public boolean isTransition()
    {
        return transition;
    }

    public boolean isWorklog()
    {
        return worklog;
    }

    public boolean isComment()
    {
        return comment;
    }
}
