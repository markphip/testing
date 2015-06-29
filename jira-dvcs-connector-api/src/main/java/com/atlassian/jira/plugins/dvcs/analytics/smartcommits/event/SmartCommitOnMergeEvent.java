package com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event;

import com.atlassian.analytics.api.annotations.EventName;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

@EventName ("jira.dvcsconnector.smartcommit.merge")
public class SmartCommitOnMergeEvent
{
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
