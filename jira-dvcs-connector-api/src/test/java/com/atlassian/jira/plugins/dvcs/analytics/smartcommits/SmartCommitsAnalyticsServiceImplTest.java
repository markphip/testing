package com.atlassian.jira.plugins.dvcs.analytics.smartcommits;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.issue.status.category.StatusCategory;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitCommandType;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitFailure;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitFailureEvent;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitOnMergeEvent;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitOperationFailedEvent;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitReceived;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitSuccessEvent;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitTransitionStatusCategoryEvent;
import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import com.google.common.collect.ImmutableSet;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Set;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@Listeners (MockitoTestNgListener.class)
public class SmartCommitsAnalyticsServiceImplTest
{
    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private MutableIssue issue;

    @Mock
    private Status transitionStatus;

    @Mock
    private StatusCategory transitionStatusCategory;

    @InjectMocks
    private SmartCommitsAnalyticsServiceImpl classUnderTest;

    @Test
    public void testFireSmartCommitSucceeded() throws Exception
    {
        final Set<SmartCommitCommandType> smartCommitTypes = ImmutableSet.of(SmartCommitCommandType.COMMENT);
        classUnderTest.fireSmartCommitSucceeded(smartCommitTypes);
        verify(eventPublisher).publish(new SmartCommitSuccessEvent(smartCommitTypes));
    }

    @Test
    public void testFireSmartCommitOperationFailed() throws Exception
    {
        final SmartCommitCommandType operationType = SmartCommitCommandType.COMMENT;
        classUnderTest.fireSmartCommitOperationFailed(operationType);
        verify(eventPublisher).publish(new SmartCommitOperationFailedEvent(operationType, SmartCommitFailure.NO_REASON));
    }

    @Test
    public void testFireSmartCommitOperationFailedWithArg() throws Exception
    {
        SmartCommitCommandType operationType = SmartCommitCommandType.TRANSITION;
        SmartCommitFailure failure = SmartCommitFailure.AMBIGIOUS_TRANSITION;

        classUnderTest.fireSmartCommitOperationFailed(operationType, failure);

        verify(eventPublisher).publish(new SmartCommitOperationFailedEvent(operationType, failure));
    }

    @Test
    public void testFireSmartCommitFailed() throws Exception
    {
        classUnderTest.fireSmartCommitFailed();
        verify(eventPublisher).publish(new SmartCommitFailureEvent(SmartCommitFailure.NO_REASON));
    }

    @Test
    public void testFireSmartCommitFailedWithReason() throws Exception
    {
        classUnderTest.fireSmartCommitFailed(SmartCommitFailure.NO_EMAIL);
        verify(eventPublisher).publish(new SmartCommitFailureEvent(SmartCommitFailure.NO_EMAIL));
    }

    @Test
    public void testFireSmartCommitReceived() throws Exception
    {
        final Set<SmartCommitCommandType> smartCommitTypes = ImmutableSet.of(SmartCommitCommandType.COMMENT);
        classUnderTest.fireSmartCommitReceived(smartCommitTypes);
        verify(eventPublisher).publish(new SmartCommitReceived(smartCommitTypes));
    }

    @Test
    public void testFireSmartCommitTransitionReceived() throws Exception
    {
        when(issue.getStatusObject()).thenReturn(transitionStatus);
        when(transitionStatus.getStatusCategory()).thenReturn(transitionStatusCategory);
        when(transitionStatusCategory.getKey()).thenReturn(StatusCategory.TO_DO);

        classUnderTest.fireSmartCommitTransitionReceived(issue);

        verify(eventPublisher).publish(new SmartCommitTransitionStatusCategoryEvent(StatusCategory.TO_DO));
    }

    @Test
    public void testFireMergeSmartCommitMergeTwoParents() throws Exception
    {
        final String parents = "[5750922bd07b525630f05e9fadc24b87c2015e7d,5750922bd07b525630f05e9fadc24b87c2015e7d]";
        classUnderTest.fireMergeSmartCommitIfAppropriate(parents);
        verify(eventPublisher).publish(new SmartCommitOnMergeEvent());
    }

    @Test
    public void testFireMergeSmartCommitOneParent() throws Exception
    {
        final String parents = "[5750922bd07b525630f05e9fadc24b87c2015e7d]";
        classUnderTest.fireMergeSmartCommitIfAppropriate(parents);
        verifyNoMoreInteractions(eventPublisher);
    }

    @Test
    public void testFireMergeSmartCommitTooMantParents() throws Exception
    {
        //Should be using ChangesetMapping.TOO_MANY_PARENTS in this test
        //but that would require adding a dependcy to the the plugin modue

        final String parents = "<TOO_MANY_PARENTS>";
        classUnderTest.fireMergeSmartCommitIfAppropriate(parents);
        verify(eventPublisher).publish(new SmartCommitOnMergeEvent());
    }

}