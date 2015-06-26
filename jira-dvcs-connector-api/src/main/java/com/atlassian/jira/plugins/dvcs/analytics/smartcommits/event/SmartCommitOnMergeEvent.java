package com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event;

import com.atlassian.analytics.api.annotations.EventName;
import org.apache.commons.lang.builder.EqualsBuilder;

@EventName ("jira.dvcsconnector.smartcommit.merge")
public class SmartCommitOnMergeEvent
{
    @Override
    public boolean equals(Object o){
        return EqualsBuilder.reflectionEquals(this, o);
    }
}
