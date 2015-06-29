package com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event;

import com.atlassian.analytics.api.annotations.EventName;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

public class SmartCommitEnabledByDefaultConfigEvent
{
    private int organizationID;
    private boolean smartCommitEnabledByDefault;

    public SmartCommitEnabledByDefaultConfigEvent(int organizationID, boolean smartCommitEnabledByDefault)
    {
        this.organizationID = organizationID;
        this.smartCommitEnabledByDefault = smartCommitEnabledByDefault;
    }

    @EventName
    public String determineEventName()
    {
        String baseEventName = "jira.dvcsconnector.smartcommit.config.default.";

        if (smartCommitEnabledByDefault)
        {
            return baseEventName + "enabled";
        }
        else
        {
            return baseEventName + "disabled";
        }
    }

    public int getOrganizationID(){
        return this.organizationID;
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
