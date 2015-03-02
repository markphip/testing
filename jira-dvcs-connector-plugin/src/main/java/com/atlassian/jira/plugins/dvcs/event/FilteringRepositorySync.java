package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.jira.plugins.dvcs.model.Repository;

import java.util.Collection;
import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 *  A {@link RepositorySync} that only captures and stores certain events.
 */
public class FilteringRepositorySync implements RepositorySync
{
    private final boolean scheduledSync;
    private final CarefulEventService eventService;
    private final Collection<Class> eventsToCapture;
    private final Repository repository;
    private final ThreadEventsCaptor threadEventCaptor;

    FilteringRepositorySync(
            boolean scheduledSync,
            @Nonnull CarefulEventService eventService,
            @Nonnull Collection<Class> eventsToCapture,
            @Nonnull Repository repository, 
            @Nonnull ThreadEventsCaptor threadEventCaptor)
    {
        this.scheduledSync = scheduledSync;
        this.eventService = checkNotNull(eventService, "eventService");
        this.eventsToCapture = checkNotNull(eventsToCapture, "eventsToCapture");
        this.repository = checkNotNull(repository, "repository");
        this.threadEventCaptor = checkNotNull(threadEventCaptor, "threadEventsCaptor");
    }

    @Override
    public void finish()
    {
        try
        {
            storeEvents();
        }
        finally
        {
            // do this in a finally block to ensure we stop capturing on this thread
            threadEventCaptor.stopCapturing();
        }
    }

    private void storeEvents()
    {
        for (Class eventType: eventsToCapture)
        {
            storeEventsOfType(eventType);
        }
    }
    
    private void storeEventsOfType(Class eventType)
    {
        threadEventCaptor.processEach(eventType, new ThreadEventsCaptor.Closure<SyncEvent>()
        {
            @Override
            public void process(@Nonnull SyncEvent event)
            {
                storeEvent(event);
            }
        });
    }
    
    private void storeEvent(@Nonnull SyncEvent event)
    {
        eventService.storeEvent(repository, event, scheduledSync);
    }
}
