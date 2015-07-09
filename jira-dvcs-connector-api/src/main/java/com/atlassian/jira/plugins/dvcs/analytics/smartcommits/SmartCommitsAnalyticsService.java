package com.atlassian.jira.plugins.dvcs.analytics.smartcommits;


import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.status.category.StatusCategory;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitCommandType;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitFailure;
import com.atlassian.jira.util.ErrorCollection;

import java.util.Set;

/**
 * A service to provide analytics for smart commits
 */
public interface SmartCommitsAnalyticsService
{
    /**
     * Fires an event to record a smart commit was successful
     * A single smart commit may consist of multiple smart commit commands.
     *
     *
     * @param smartCommitCommandTypesPresent The smart commit types present in the successful smart commit
     */
    void fireSmartCommitSucceeded(Set<SmartCommitCommandType> smartCommitCommandTypesPresent);

    /**
     * Fires an event to record a smart commit operation failed
     *
     * @param smartCommitCommandType the type of smart commit that failed
     */
    void fireSmartCommitOperationFailed(SmartCommitCommandType smartCommitCommandType);

    /**
     * Fires an event to record that a smart commit operation was unsuccessful.
     *
     * @param smartCommitCommandType the type of the smart commit that failed
     * @param failureReason the reason for the operation failure, may be NO_REASON if the reason is unknown
     */
    void fireSmartCommitOperationFailed(SmartCommitCommandType smartCommitCommandType, SmartCommitFailure failureReason);

    /**
     * Fires an event to record that a smart commit failed
     */
    void fireSmartCommitFailed();

    /**
     * Fires an event to record that a smart commit failed
     *
     * @param failureReason the reason for the operation failure, may be NO_REASON if the reason is unknown
     */
    void fireSmartCommitFailed(SmartCommitFailure failureReason);

    /**
     * Fires an event to record that a smart commit was received
     *
     * @param smartCommitCommandTypesPresent The smart commit types present in the successful smart commit
     */
    void fireSmartCommitReceived(Set<SmartCommitCommandType> smartCommitCommandTypesPresent);


    /**
     * Fires an event to record that a smart commit with an issue state transition command was recieved,
     * records what status category the issue transition was to.
     *
     * @param issue The issues state after the transitions has been applied to it
     */
    void fireSmartCommitTransitionReceived(Issue issue);

    /**
     * Fires a event to record that the smart commit came from a merge commit. Only fires the event if the arg indicates
     * that the commit had multiple parents.
     *
     * @param commitParentsData A string representation of a JSON array of the parent commit information
     */
    void fireMergeSmartCommitIfAppropriate(String commitParentsData);

}
