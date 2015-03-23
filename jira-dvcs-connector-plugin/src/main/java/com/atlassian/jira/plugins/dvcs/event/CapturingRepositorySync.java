package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.google.common.collect.ImmutableSet;

import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Helper class for capturing/storing events produced during repository synchronisation.
 */
class CapturingRepositorySync implements RepositorySync
{
    private final CarefulEventService eventService;
    private final Collection<Class> eventsToCapture;
    private final Repository repository;
    private final boolean scheduledSync;
    private final ThreadEventsCaptor threadEventCaptor;

    CapturingRepositorySync(
            @Nonnull CarefulEventService eventService,
            @Nonnull Collection<Class> eventsToCapture,
            @Nullable Repository repository,
            boolean scheduledSync,
            @Nonnull ThreadEventsCaptor threadEventCaptor)
    {
        checkCaptureEventsAreOnlySyncEvents(eventsToCapture);
        
        this.eventService = checkNotNull(eventService, "eventService");
        this.eventsToCapture = checkNotNull(ImmutableSet.copyOf(eventsToCapture), "eventsToCapture");
        this.repository = checkNotNull(repository, "repository");
        this.scheduledSync = scheduledSync;
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
    
    private void checkCaptureEventsAreOnlySyncEvents(final Collection<Class> eventsToCapture) 
    {
        for (Class eventClass : eventsToCapture)
        {
            checkState(SyncEvent.class.isInstance(eventClass));
        }
    }

    private void storeEvents()
    {
        for (Class eventType: eventsToCapture)
        {
            storeEventsOfType(eventType);
        }
    }

    private void storeEventsOfType(@Nonnull final Class eventType)
    {
        checkNotNull(eventType);
        threadEventCaptor.processEach(eventType, new ThreadEventsCaptor.Closure<SyncEvent>()
        {
            @Override
            public void process(@Nonnull SyncEvent event)
            {
                eventService.storeEvent(repository, event, scheduledSync);
            }
        });
    }
}
