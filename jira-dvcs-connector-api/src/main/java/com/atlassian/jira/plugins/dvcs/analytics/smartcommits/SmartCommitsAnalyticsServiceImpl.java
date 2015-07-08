package com.atlassian.jira.plugins.dvcs.analytics.smartcommits;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.issue.status.category.StatusCategory;
import com.atlassian.jira.plugins.dvcs.analytics.event.DvcsType;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitCommandType;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitEnabledByDefaultConfigEvent;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitFailure;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitFailureEvent;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitOnMergeEvent;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitOperationFailedEvent;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitRepoConfigChangedEvent;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitReceived;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitSuccessEvent;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitTransitionStatusCategoryEvent;
import org.apache.commons.lang.StringUtils;
import com.google.common.base.Preconditions;
import org.springframework.stereotype.Component;

import java.util.Set;
import javax.inject.Inject;


@Component
public class SmartCommitsAnalyticsServiceImpl implements SmartCommitsAnalyticsService
{
    private final EventPublisher eventPublisher;

    @Inject
    public SmartCommitsAnalyticsServiceImpl(final EventPublisher eventPublisher){
        Preconditions.checkNotNull(eventPublisher);
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void fireSmartCommitSucceeded(final Set<SmartCommitCommandType> smartCommitCommandTypesPresent)
    {
        Preconditions.checkNotNull(smartCommitCommandTypesPresent);
        eventPublisher.publish(new SmartCommitSuccessEvent(smartCommitCommandTypesPresent));
    }

    @Override
    public void fireSmartCommitOperationFailed(final SmartCommitCommandType smartCommitCommandType)
    {
        fireSmartCommitOperationFailed(smartCommitCommandType, SmartCommitFailure.NO_REASON);
    }

    @Override
    public void fireSmartCommitOperationFailed(final SmartCommitCommandType smartCommitCommandType, final SmartCommitFailure failureReason)
    {
        Preconditions.checkNotNull(smartCommitCommandType);
        String failureReasonString = failureReason == null ? "" : failureReason.toString();
        eventPublisher.publish(new SmartCommitOperationFailedEvent(smartCommitCommandType, failureReason));
    }

    @Override
    public void fireSmartCommitFailed()
    {
        fireSmartCommitFailed(SmartCommitFailure.NO_REASON);
    }

    @Override
    public void fireSmartCommitFailed(final SmartCommitFailure failureReason)
    {
        Preconditions.checkNotNull(failureReason);
        eventPublisher.publish(new SmartCommitFailureEvent(failureReason));
    }

    @Override
    public void fireSmartCommitReceived(final Set<SmartCommitCommandType> smartCommitCommandTypesPresent)
    {   Preconditions.checkNotNull(smartCommitCommandTypesPresent);
        eventPublisher.publish(new SmartCommitReceived(smartCommitCommandTypesPresent));
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
        // This is not pretty, but it is the best we can do atm.
        // commitParentsData is a JSON array of parents
        //
        // e.g "[45db8d8a6f2c06adb1f7c6384eaca3df3ff5c02e,bc277b6e1c48931be0b74ced521d82d3590bf5d5]"
        //
        // but if it would be longer that 255 chars it instead is
        //
        // "<TOO_MANY_PARENTS>"
        //
        // so if it contains a < or a , we know the commit has more than one parent.
        // This is supposed to be short lived as this is moving to the jira-development-status-plugin

        if(commitParentsData != null){
            Boolean hasMoreThanOneParent = commitParentsData.contains("<") || commitParentsData.contains(",");
            if (hasMoreThanOneParent)
            {
                eventPublisher.publish(new SmartCommitOnMergeEvent());
            }
        }
    }

    @Override
    public void fireNewOrganizationAddedWithSmartCommits(final DvcsType dvcsType, final boolean smartCommitsEnabled)
    {
        if(smartCommitsEnabled){
            eventPublisher.publish(new AccountAddedWithSmartCommitsEvent(dvcsType));
        }
    }
    @Override
    public void fireSmartCommitAutoEnabledConfigChange(final int orgId, final boolean smartCommitsEnabled)
    {
        eventPublisher.publish(new SmartCommitEnabledByDefaultConfigEvent(orgId, smartCommitsEnabled));
    }

    @Override
    public void fireSmartCommitPerRepoConfigChange(final int repoId, final boolean smartCommitsEnabled)
    {
        eventPublisher.publish(new SmartCommitRepoConfigChangedEvent(repoId, smartCommitsEnabled));
    }


}
