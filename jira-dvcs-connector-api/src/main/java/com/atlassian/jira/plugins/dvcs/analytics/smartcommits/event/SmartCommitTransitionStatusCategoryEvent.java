package com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event;


import com.atlassian.analytics.api.annotations.EventName;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;


public class SmartCommitTransitionStatusCategoryEvent
{
    private final String key;
    private final String TRANSITION_EVENT_NAME = "jira.dvcsconnector.smartcommit.transition.to.";

    public SmartCommitTransitionStatusCategoryEvent(String statusCategoryKey)
    {
        this.key = statusCategoryKey;
    }

    @EventName
    public String determineEventName()
    {
        return TRANSITION_EVENT_NAME + key;
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
