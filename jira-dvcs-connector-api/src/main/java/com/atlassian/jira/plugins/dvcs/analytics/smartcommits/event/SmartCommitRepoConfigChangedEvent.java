package com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event;

import com.atlassian.analytics.api.annotations.EventName;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

public class SmartCommitRepoConfigChangedEvent
{
    private int id;
    private boolean smartCommitEnabledByDefault;

    public SmartCommitRepoConfigChangedEvent(int id, boolean smartCommitEnabledByDefault)
    {
        this.id = id;
        this.smartCommitEnabledByDefault = smartCommitEnabledByDefault;
    }

    @EventName
    public String determineEventName()
    {
        String baseEventName = "jira.dvcsconnector.smartcommit.repo.";

        if (smartCommitEnabledByDefault)
        {
            return baseEventName + "enabled";
        }
        else
        {
            return baseEventName + "disabled";
        }
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

