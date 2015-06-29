package com.atlassian.jira.plugins.dvcs.analytics.smartcommits;

import com.atlassian.analytics.api.annotations.EventName;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

public class AccountAddedWithSmartCommitsEvent
{

    private com.atlassian.jira.plugins.dvcs.analytics.event.DvcsType dvcsType;

    public AccountAddedWithSmartCommitsEvent(com.atlassian.jira.plugins.dvcs.analytics.event.DvcsType dvcsType){
        this.dvcsType = dvcsType;
    }

    @EventName
    public String determineEventName()
    {
        return "jira.dvcsconnector." + dvcsType + ".smartcommit.default.enabled";
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


