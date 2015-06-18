package com.atlassian.jira.plugins.dvcs.analytics.event;

import com.atlassian.analytics.api.annotations.EventName;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Base class for all dvcs analytics events.
 */
public abstract class DvcsConfigAnalyticsEvent
{

    protected final String source;
    protected final String prefix;

    protected DvcsConfigAnalyticsEvent(Source source, String prefix)
    {
        this.source = source.toString();
        this.prefix = prefix;
    }

    @EventName
    public String determineEventName()
    {
        return "jira.dvcsconnector.config." + prefix;
    }

    // using EqualsBuilder and HashCodeBuilder here as they are used in tests only
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

    public String getSource()
    {
        return source;
    }

}
