package com.atlassian.jira.plugins.dvcs.analytics.event;

public enum EventSource
{
    ISSUE("issue"),
    AGILE("agile");

    final String eventSource;

    EventSource(String eventSource)
    {
        this.eventSource = eventSource;
    }

    @Override
    public String toString()
    {
        return eventSource;
    }

}
