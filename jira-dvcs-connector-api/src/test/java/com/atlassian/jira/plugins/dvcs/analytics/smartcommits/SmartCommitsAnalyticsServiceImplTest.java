package com.atlassian.jira.plugins.dvcs.analytics.smartcommits;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.issue.status.category.StatusCategory;
import com.atlassian.jira.plugins.dvcs.analytics.event.DvcsType;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitCommandType;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitEnabledByDefaultConfigEvent;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitFailure;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitFailureEvent;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitOnMergeEvent;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitOperationFailedEvent;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitRecieved;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitRepoConfigChangedEvent;
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
    EventPublisher eventPublisher;

    @Mock
    MutableIssue issue;

    @Mock
    Status transitionStatus;

    @Mock
    StatusCategory transitionStatusCategory;

    @InjectMocks
    SmartCommitsAnalyticsServiceImpl classUnderTest;

    @Test
    public void testFireSmartCommitSucceeded() throws Exception
    {
        Set<SmartCommitCommandType> smartCommitTypes = ImmutableSet.of(SmartCommitCommandType.COMMENT);
        classUnderTest.fireSmartCommitSucceeded(smartCommitTypes);

        verify(eventPublisher).publish(new SmartCommitSuccessEvent(smartCommitTypes));
    }

    @Test
    public void testFireSmartCommitOperationFailed() throws Exception
    {
        SmartCommitCommandType operationType = SmartCommitCommandType.COMMENT;
        classUnderTest.fireSmartCommitOperationFailed(operationType);
        verify(eventPublisher).publish(new SmartCommitOperationFailedEvent(operationType, ""));

    }

    @Test
    public void testFireSmartCommitOperationFailedWithArg() throws Exception
    {
        SmartCommitCommandType operationType = SmartCommitCommandType.TRANSITION;
        SmartCommitFailure failure = SmartCommitFailure.AMBIGIOUS_TRANSITION;

        classUnderTest.fireSmartCommitOperationFailed(operationType, failure);

        verify(eventPublisher).publish(new SmartCommitOperationFailedEvent(operationType, failure.toString()));

    }

    @Test
    public void testFireSmartCommitFailed() throws Exception
    {
        classUnderTest.fireSmartCommitFailed();

        verify(eventPublisher).publish(new SmartCommitFailureEvent(""));
    }

    @Test
    public void testFireSmartCommitFailedWithReason() throws Exception
    {
        classUnderTest.fireSmartCommitFailed(SmartCommitFailure.NO_EMAIL);

        verify(eventPublisher).publish(new SmartCommitFailureEvent(SmartCommitFailure.NO_EMAIL.toString()));
    }


    @Test
    public void testFireSmartCommitReceived() throws Exception
    {
        Set<SmartCommitCommandType> smartCommitTypes = ImmutableSet.of(SmartCommitCommandType.COMMENT);
        classUnderTest.fireSmartCommitReceived(smartCommitTypes);

        verify(eventPublisher).publish(new SmartCommitRecieved(smartCommitTypes));
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
        String parents = "[5750922bd07b525630f05e9fadc24b87c2015e7d,5750922bd07b525630f05e9fadc24b87c2015e7d]";
        classUnderTest.fireMergeSmartCommitIfAppropriate(parents);
        verify(eventPublisher).publish(new SmartCommitOnMergeEvent());
    }

    @Test
    public void testFireMergeSmartCommitOneParent() throws Exception
    {
        String parents = "[5750922bd07b525630f05e9fadc24b87c2015e7d]";
        classUnderTest.fireMergeSmartCommitIfAppropriate(parents);
        verifyNoMoreInteractions(eventPublisher);
    }

    @Test
    public void testFireMergeSmartCommitTooMantParents() throws Exception
    {
        String parents = "<TOO_MANY_PARENTS>";
        classUnderTest.fireMergeSmartCommitIfAppropriate(parents);
        verify(eventPublisher).publish(new SmartCommitOnMergeEvent());
    }

    @Test
    public void testFireNewOrganizationAddedWithSmartCommitsEnabled(){
        classUnderTest.fireNewOrganizationAddedWithSmartCommits(DvcsType.BITBUCKET, true);
        verify(eventPublisher).publish(new AccountAddedWithSmartCommitsEvent(DvcsType.BITBUCKET));
    }

    @Test
    public void testFireNewOrganizationAddedWithSmartCommitsDisabled(){
        classUnderTest.fireNewOrganizationAddedWithSmartCommits(DvcsType.BITBUCKET, false);
        verifyNoMoreInteractions(eventPublisher);
    }

    @Test
    public void testFireSmartCommitAutoEnabledConfigChange(){
        classUnderTest.fireSmartCommitAutoEnabledConfigChange(1, false);
        verify(eventPublisher).publish(new SmartCommitEnabledByDefaultConfigEvent(1, false));
    }

    @Test
    public void testFireSmartCommitPerRepoConfigChange(){
        classUnderTest.fireSmartCommitPerRepoConfigChange(1, false);
        verify(eventPublisher).publish(new SmartCommitRepoConfigChangedEvent(1, false));
    }

}