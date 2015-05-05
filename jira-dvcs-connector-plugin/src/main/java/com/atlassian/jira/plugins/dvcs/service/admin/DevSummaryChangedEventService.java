package com.atlassian.jira.plugins.dvcs.service.admin;

import com.atlassian.annotations.PublicApi;

@PublicApi
public interface DevSummaryChangedEventService
{
    boolean generateDevSummaryEvents(int pageSize);

    DevSummaryCachePrimingStatus getEventGenerationStatus();

    void stopGeneration();
}
