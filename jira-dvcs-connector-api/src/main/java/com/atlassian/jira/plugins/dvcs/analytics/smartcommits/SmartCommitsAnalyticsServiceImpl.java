package com.atlassian.jira.plugins.dvcs.analytics.smartcommits;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.issue.status.category.StatusCategory;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitCommandType;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitFailure;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitFailureEvent;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitOnMergeEvent;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitOperationFailedEvent;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitRecieved;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitSuccessEvent;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitTransitionStatusCategoryEvent;
import org.springframework.stereotype.Component;

import java.util.Set;
import javax.inject.Inject;


@Component
public class SmartCommitsAnalyticsServiceImpl implements SmartCommitsAnalyticsService
{
    @Inject
    private EventPublisher eventPublisher;

    @Override
    public void fireSmartCommitSucceeded(final Set<SmartCommitCommandType> smartCommitCommandTypesPresent)
    {
        eventPublisher.publish(new SmartCommitSuccessEvent(smartCommitCommandTypesPresent));
    }

    @Override
    public void fireSmartCommitOperationFailed(final SmartCommitCommandType smartCommitCommandType)
    {
        eventPublisher.publish(new SmartCommitOperationFailedEvent(smartCommitCommandType, ""));
    }

    @Override
    public void fireSmartCommitOperationFailed(final SmartCommitCommandType smartCommitCommandType, final SmartCommitFailure failureReason)
    {
        String failureReasonString = failureReason == null ? "" : failureReason.toString();
        eventPublisher.publish(new SmartCommitOperationFailedEvent(smartCommitCommandType, failureReasonString));
    }

    @Override
    public void fireSmartCommitFailed()
    {
        eventPublisher.publish(new SmartCommitFailureEvent());
    }

    @Override
    public void fireSmartCommitReceived(final Set<SmartCommitCommandType> smartCommitCommandTypesPresent)
    {
        eventPublisher.publish(new SmartCommitRecieved(smartCommitCommandTypesPresent));
    }

    @Override
    public void fireSmartCommitTransitionReceived(final Issue issue)
    {
        Status transitionStatus = issue.getStatusObject();
        StatusCategory transitionStatusCategory = transitionStatus.getStatusCategory();
        String statusCategoryKey = transitionStatusCategory.getKey();
        eventPublisher.publish(new SmartCommitTransitionStatusCategoryEvent(statusCategoryKey));
    }

    @Override
    public void fireMergeSmartCommitIfAppropriate(final String commitParentsData)
    {
        //This is not pretty, but it is the best we can do atm.
        //commitParentsData is a JSON array of parents which if that would be longer than 255 chars
        //is instead the string <TOO_MANY_PARENTS>
        //so if it contains a < or a , we know the commit has more than one parent.
        //This is supposed to be short lived as this is moving to the jira-development-status-plugin
        //Sorry
        Boolean hasMoreThanOneParent = commitParentsData.contains("<") || commitParentsData.contains(",");
        if (hasMoreThanOneParent)
        {
            eventPublisher.publish(new SmartCommitOnMergeEvent());
        }

    }
}
