package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag;
import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.Listeners;

import java.util.EnumSet;

import static com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag.SOFT_SYNC;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Listeners (MockitoTestNgListener.class)
public class RepositorySyncHelperTest
{
    ThreadEvents threadEvents;

    @Mock
    EventService eventService;

    @Mock
    Repository repository;

    RepositorySyncHelper repoSyncHelper;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        threadEvents = new ThreadEvents();
        repoSyncHelper = new RepositorySyncHelper(threadEvents, eventService);
    }

    @Test
    public void finishingSyncShouldStopCapturingEvents() throws Exception
    {
        threadEvents = mock(ThreadEvents.class);
        repoSyncHelper = new RepositorySyncHelper(threadEvents, eventService);

        final ThreadEventsCaptor captor = mock(ThreadEventsCaptor.class);
        when(threadEvents.startCapturing()).thenReturn(captor);

        repoSyncHelper.startSync(repository, EnumSet.of(SOFT_SYNC)).finish();
        verify(captor).stopCapturing();
    }

    @Test
    public void returnedSyncDoesNotCaptureWhenRepositoryIsNull() throws Exception
    {
        RepositorySync sync = repoSyncHelper.startSync(null, EnumSet.of(SOFT_SYNC));
        threadEvents.broadcast(new Object());

        sync.finish();
        verify(eventService, never()).storeEvent(any(Repository.class), any(SyncEvent.class), false);
    }

    @Test
    public void returnedSyncDoesNotCaptureDuringNonSoftSync() throws Exception
    {
        RepositorySync sync = repoSyncHelper.startSync(repository, SynchronizationFlag.NO_FLAGS);
        threadEvents.broadcast(new Object());

        sync.finish();
        verify(eventService, never()).storeEvent(any(Repository.class), any(SyncEvent.class), false);
    }

    @Test
    public void returnedSyncCapturesEventsWhenSoftSyncIsTrue() throws Exception
    {
        final SyncEvent event = new TestEvent();

        RepositorySync sync = repoSyncHelper.startSync(repository, EnumSet.of(SOFT_SYNC));
        threadEvents.broadcast(event);

        sync.finish();
        verify(eventService).storeEvent(repository, event, false);
    }

}