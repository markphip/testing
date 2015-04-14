package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag;
import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.EnumSet;

import static com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag.SOFT_SYNC;
import static com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag.WEBHOOK_SYNC;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Listeners (MockitoTestNgListener.class)
public class RepositorySyncHelperTest
{
    private final SyncEvent syncEvent = new TestEvent();
    private final DevSummaryChangedEvent devSummaryChangedEvent = newDevSummaryChangedEvent();

    @Mock CarefulEventService eventService;
    @Mock EventsFeature eventsFeature;
    @Mock Repository repository;

    RepositorySyncHelper repoSyncHelper;
    ThreadEvents threadEvents;

    @BeforeMethod
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        when(eventsFeature.isEnabled()).thenReturn(true);

        threadEvents = new ThreadEvents();
        repoSyncHelper = new RepositorySyncHelper(eventService, eventsFeature, threadEvents);
    }

    @Test
    public void finishingSyncShouldStopCapturingEvents() throws Exception
    {
        // setup
        threadEvents = mock(ThreadEvents.class);
        repoSyncHelper = new RepositorySyncHelper(eventService, eventsFeature, threadEvents);
        
        final ThreadEventsCaptor captor = mock(ThreadEventsCaptor.class);
        when(threadEvents.startCapturing()).thenReturn(captor);

        // execute
        repoSyncHelper.startSync(repository, EnumSet.of(SOFT_SYNC)).finish();
        
        // check
        verify(captor).stopCapturing();
    }

    @Test
    public void returnedSyncDoesNotCaptureWhenRepositoryIsNull() throws Exception
    {
        // setup
        RepositorySync sync = repoSyncHelper.startSync(null, EnumSet.of(SOFT_SYNC));
        broadcastEvents();

        // execute
        sync.finish();
        
        // check
        verify(eventService, never()).storeEvent(any(Repository.class), any(SyncEvent.class), anyBoolean());
    }

    @Test
    public void returnedSyncOnlyCapturesDevSummaryChangedEventsDuringNonSoftSync() throws Exception
    {
        // setup
        RepositorySync sync = repoSyncHelper.startSync(repository, SynchronizationFlag.NO_FLAGS);
        broadcastEvents();

        // execute
        sync.finish();
        
        // check
        verify(eventService).storeEvent(repository, devSummaryChangedEvent, true);
        verify(eventService, times(1)).storeEvent(any(Repository.class), any(SyncEvent.class), anyBoolean());
    }

    @Test
    public void returnedSyncCapturesAllSyncEventsWhenSoftSyncIsTrue() throws Exception
    {
        // setup
        RepositorySync sync = repoSyncHelper.startSync(repository, EnumSet.of(SOFT_SYNC));
        broadcastEvents();

        // execute
        sync.finish();

        // check
        verify(eventService).storeEvent(repository, syncEvent, true);
        verify(eventService).storeEvent(repository, devSummaryChangedEvent, true);
        verify(eventService, times(2)).storeEvent(any(Repository.class), any(SyncEvent.class), anyBoolean());
    }

    @Test
    public void returnedSyncDoesNotCaptureWhenEventsFeatureIsDisabled() throws Exception
    {
        // setup
        when(eventsFeature.isEnabled()).thenReturn(false);

        RepositorySync sync = repoSyncHelper.startSync(repository, EnumSet.of(SOFT_SYNC));
        broadcastEvents();

        // execute
        sync.finish();

        // check
        verify(eventService, never()).storeEvent(any(Repository.class), any(SyncEvent.class), anyBoolean());
    }

    @Test
    public void shouldTriggerScheduledSyncCaptureWhenWebhookFlagIsMissing() throws Exception
    {
        // setup
        final boolean scheduledSync = true;

        RepositorySync sync = repoSyncHelper.startSync(repository, SynchronizationFlag.NO_FLAGS);
        broadcastEvents();

        // execute
        sync.finish();

        // check
        verify(eventService).storeEvent(repository, devSummaryChangedEvent, scheduledSync);
        verify(eventService, times(1)).storeEvent(any(Repository.class), any(SyncEvent.class), anyBoolean());
    }

    @Test
    public void shouldNotTriggerScheduledSyncCaptureWhenWebhookFlagIsPresent() throws Exception
    {
        // setup
        final boolean scheduledSync = false;

        RepositorySync sync = repoSyncHelper.startSync(repository, EnumSet.of(WEBHOOK_SYNC));
        broadcastEvents();

        // execute
        sync.finish();

        // check
        verify(eventService).storeEvent(repository, devSummaryChangedEvent, scheduledSync);
        verify(eventService, times(1)).storeEvent(any(Repository.class), any(SyncEvent.class), anyBoolean());
    }

    private void broadcastEvents()
    {
        threadEvents.broadcast(syncEvent);
        threadEvents.broadcast(devSummaryChangedEvent);
    }

    private DevSummaryChangedEvent newDevSummaryChangedEvent()
    {
        return new DevSummaryChangedEvent(0, "dvcsType", Collections.<String>emptySet());
    }
}
