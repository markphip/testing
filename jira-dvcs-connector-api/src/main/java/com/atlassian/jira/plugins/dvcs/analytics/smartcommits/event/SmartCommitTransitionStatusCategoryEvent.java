package com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event;


import com.atlassian.analytics.api.annotations.EventName;
import org.apache.commons.lang.builder.EqualsBuilder;


public class SmartCommitTransitionStatusCategoryEvent
{
    private final String key;

    public SmartCommitTransitionStatusCategoryEvent(String statusCategoryKey)
    {
        this.key = statusCategoryKey;
    }

    @EventName
    public String determineEventName()
    {
        return "jira.dvcsconnector.smartcommit.transition.to." + key;
    }

    @Override
    public boolean equals(Object o)
    {
        return EqualsBuilder.reflectionEquals(this, o);
    }
}
