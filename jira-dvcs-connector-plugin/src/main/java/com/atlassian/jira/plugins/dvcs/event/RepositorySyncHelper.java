package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag;
import com.google.common.collect.ImmutableSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag.SOFT_SYNC;
import static com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag.WEBHOOK_SYNC;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Helper class for managing events during repository synchronisation.
 */
@Component
public class RepositorySyncHelper
{
    private static final RepositorySync NULL_REPO_SYNC = new NullRepositorySync();
    
    private static final ImmutableSet<Class> EVENTS_FOR_SOFT_SYNC = ImmutableSet.<Class>of(SyncEvent.class);
    private static final ImmutableSet<Class> EVENTS_TO_TRIGGER_DEV_SUMMARY_REINDEX = 
            ImmutableSet.<Class>of(DevSummaryChangedEvent.class);
    
    private final CarefulEventService eventService;
    private final EventsFeature eventsFeature;
    private final ThreadEvents threadEvents;

    @Autowired
    public RepositorySyncHelper(
            @Nonnull CarefulEventService eventService,
            @Nonnull EventsFeature eventsFeature,
            @Nonnull ThreadEvents threadEvents)
    {
        this.eventService = checkNotNull(eventService, "eventService");
        this.eventsFeature = checkNotNull(eventsFeature, "eventsFeature");
        this.threadEvents = checkNotNull(threadEvents, "threadEvents");
    }

    /**
     * Returns a new RepositorySync object for the given repository, which by default captures and stores all
     * {@link SyncEvent}s.
     *  
     * If {@code repository} is null or the {@code eventsFeature} is disabled
     * then the returned RepositorySync will not capture (or store) events.
     * Otherwise, if {@code syncFlags} does not contain the SOFT_SYNC {@link SynchronizationFlag}, then we
     * only capture events that will trigger a reindex of DevSummary, and we avoid capturing other events such as 
     * those that trigger Automatic Issue Transitions.
     *
     * @param repository the Repository being synchronised
     * @param syncFlags synchronisation flags
     * @return a RepositorySync
     */
    @Nonnull
    public RepositorySync startSync(@Nullable Repository repository, @Nonnull EnumSet<SynchronizationFlag> syncFlags)
    {
        checkNotNull(syncFlags, "syncFlags");

        if (eventsFeature.isEnabled() && repository != null)
        {
            return createRepositorySync(repository, syncFlags);
        }
        return NULL_REPO_SYNC;
    }

    private RepositorySync createRepositorySync(
            @Nullable Repository repository, 
            @Nonnull EnumSet<SynchronizationFlag> syncFlags)
    {
        final boolean scheduledSync = !syncFlags.contains(WEBHOOK_SYNC);
        final boolean isSoftSync = syncFlags.contains(SOFT_SYNC);
        final ThreadEventsCaptor threadEventsCaptor = threadEvents.startCapturing();
        final ImmutableSet<Class> eventsToCapture;
        
        if (isSoftSync)
        {
            eventsToCapture = EVENTS_FOR_SOFT_SYNC;
        }
        else
        {
            eventsToCapture = EVENTS_TO_TRIGGER_DEV_SUMMARY_REINDEX;
        }

        return new CapturingRepositorySync(
                eventService, 
                eventsToCapture, 
                repository, 
                scheduledSync,
                threadEventsCaptor);
    }
}
