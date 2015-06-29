package com.atlassian.jira.plugins.dvcs.analytics.smartcommits;


import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.status.category.StatusCategory;
import com.atlassian.jira.plugins.dvcs.analytics.event.DvcsType;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitCommandType;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitFailure;
import com.atlassian.jira.util.ErrorCollection;

import java.util.Set;

public interface SmartCommitsAnalyticsService
{

    void fireSmartCommitSucceeded(Set<SmartCommitCommandType> smartCommitCommandTypesPresent);

    void fireSmartCommitOperationFailed(SmartCommitCommandType smartCommitCommandType);

    void fireSmartCommitOperationFailed(SmartCommitCommandType smartCommitCommandType, SmartCommitFailure failureReason);

    void fireSmartCommitFailed();

    void fireSmartCommitFailed(SmartCommitFailure failureReason);

    void fireSmartCommitReceived(Set<SmartCommitCommandType> smartCommitCommandTypesPresent);

    void fireSmartCommitTransitionReceived(Issue issue);

    void fireMergeSmartCommitIfAppropriate(String commitParentsData);

    void fireNewOrganizationAddedWithSmartCommits(DvcsType dvcsType, boolean smartCommitsEnabled);

    void fireSmartCommitAutoEnabledConfigChange(int orgId, boolean smartCommitsEnabled);

    void fireSmartCommitPerRepoConfigChange(int repoId, boolean smartCommitsEnabled);

}
