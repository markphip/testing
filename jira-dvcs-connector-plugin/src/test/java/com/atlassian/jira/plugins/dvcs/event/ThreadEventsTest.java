package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.beust.jcommander.internal.Lists;
import com.google.common.collect.Sets;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith (MockitoJUnitRunner.class)
public class ThreadEventsTest
{
    private static final int REPO_ID_1 = 1234;
    private static final int REPO_ID_2 = 5678;
    private static final String DVCS_TYPE_BB = "bitbucket";
    private static final String DVCS_TYPE_GH = "github";

    ThreadEvents threadEvents;
    ThreadEventsCaptor threadEventsCaptor;
    CollectEventsClosure closure;

    @Mock
    ChangesetMapping changeset;

    @Mock
    RepositoryPullRequestMapping pullRequest;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        threadEvents = new ThreadEvents();

        threadEventsCaptor = threadEvents.startCapturing();
        closure = new CollectEventsClosure();
    }

    @After
    public void tearDown() throws Exception
    {
        threadEventsCaptor.stopCapturing();
    }

    @Test
    public void processEachShouldProcessAllPublishedEvents() throws Exception
    {
        threadEvents.broadcast(changeset);
        threadEvents.broadcast(pullRequest);

        threadEventsCaptor.processEach(closure);
        assertThat(closure.events, Matchers.<Object>hasItems(changeset, pullRequest));
    }

    @Test
    public void processEachShouldFilterByClassType() throws Exception
    {
        threadEvents.broadcast(changeset);
        threadEvents.broadcast(pullRequest);

        CollectEventsClosure closure2 = new CollectEventsClosure();
        threadEventsCaptor.processEach(RepositoryPullRequestMapping.class, closure);
        threadEventsCaptor.processEach(closure2);

        assertThat("1st call to processEach should process PR event only", closure.events, Matchers.<Object>hasItems(pullRequest));
        assertThat("2nd call to processEach should process remaining events", closure2.events, Matchers.<Object>hasItems(changeset));
    }

    @Test
    public void listenersShouldNotReceiveEventsRaisedAfterTheyHaveStoppedListening() throws Exception
    {
        threadEventsCaptor.stopCapturing();
        threadEvents.broadcast(pullRequest);

        threadEventsCaptor.processEach(closure);
        assertThat(closure.events.size(), equalTo(0));
    }

    @Test
    public void listenersShouldNotGetEventsRaisedInOtherThreads() throws Exception
    {
        Thread thread = new Thread()
        {
            @Override
            public void run()
            {
                threadEvents.broadcast(changeset);
            }
        };
        thread.start();
        thread.join();

        threadEventsCaptor.processEach(closure);
        assertThat(closure.events.size(), equalTo(0));
    }
    
    @Test(expected = IllegalStateException.class)
    public void startingTwoCapturesOnTheSameThreadThrowsException() throws Exception
    {
        threadEvents.startCapturing();
    }
    
    @Test
    public void devSummaryChangedEventsShouldBeCombinedPerIssueKey()
    {
        // setup
        final String issueKey1 = "TEST-1";
        final String issueKey2 = "TEST-2";
        final Date date = new Date();
        
        threadEvents.broadcast(devSummaryChangedEvent(REPO_ID_1, DVCS_TYPE_BB, date, issueKey1));
        threadEvents.broadcast(devSummaryChangedEvent(REPO_ID_1, DVCS_TYPE_BB, date, issueKey1, issueKey2));
        threadEvents.broadcast(devSummaryChangedEvent(REPO_ID_2, DVCS_TYPE_GH, date, issueKey1, issueKey2));

        // execute
        threadEventsCaptor.processEach(closure);
        
        // check
        List<DevSummaryChangedEvent> events = castToDevSummaryChangedEvents(closure.events);
        assertThat("Deduped issue events correctly", events, Matchers.hasItems(
                equalTo(devSummaryChangedEvent(REPO_ID_1, DVCS_TYPE_BB, date, issueKey1)),
                equalTo(devSummaryChangedEvent(REPO_ID_1, DVCS_TYPE_BB, date, issueKey2)),
                equalTo(devSummaryChangedEvent(REPO_ID_2, DVCS_TYPE_GH, date, issueKey1, issueKey2))));
    }

    private List<DevSummaryChangedEvent> castToDevSummaryChangedEvents(@Nonnull final List<Object> events)
    {
        List<DevSummaryChangedEvent> devSummaryChangedEvents = Lists.newArrayList();
        for (Object e : events)
        {
            devSummaryChangedEvents.add((DevSummaryChangedEvent) e);
        }
        return devSummaryChangedEvents;
    }

    private DevSummaryChangedEvent devSummaryChangedEvent(
            int repoId,
            @Nonnull final String dvcsType,
            @Nonnull final Date date,
            @Nonnull final String... issueKeys)
    {
        Set<String> issueKeySet = Sets.newHashSet(issueKeys);
        return new DevSummaryChangedEvent(repoId, dvcsType, issueKeySet, date);
    }

    private static class CollectEventsClosure implements ThreadEventsCaptor.Closure<Object>
    {
        final List<Object> events = Lists.newArrayList();

        @Override
        public void process(@Nonnull Object event)
        {
            events.add(event);
        }
    }
}
