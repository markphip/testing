package com.atlassian.jira.plugins.dvcs.model;

import com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag;

import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Information about the current synchronisation progress
 */
public interface Progress
{

    /**
     * Call this method to update the current status of the progress.
     *
     * @param changesetCount count of changesets processed so far
     * @param issues set of issues that has been affected, it doesn't have to be all affected issues in synchronisation,
     *        but only issues since the previous {@link #inProgress(int, java.util.Set, int)} invocation
     * @param synchroErrorCount synchronization errors count
     */
    void inProgress(int changesetCount, Set<String> issues, int synchroErrorCount);

    int getAuditLogId();
    void setAuditLogId(int id);

    /**
     * Marks progress as finished.
     */
    void finish();

    /**
     * @return true if the progress is Finished
     */
    boolean isFinished();

    /**
     * @return number of JIRA issues found in commit messages
     */
    int getJiraCount();

    /**
     * @return number of changesets synchronised
     */
    int getChangesetCount();

    /**
     * @return number of changesets which are not fully synchronised.
     */
    int getSynchroErrorCount();

    int getPullRequestActivityCount();

	/**
	 * @return error messages
	 */
	String getError();

	/**
	 * Indication that the synchronisation should stop.
	 * Used when repository is unlinked or organisation and its repositories are deleted.
	 *
	 * @param shouldStop
	 */
	void setShouldStop(boolean shouldStop);

	/**
	 * Indication that the synchronisation should pause
	 * Used when repository is unlinked or organisation and its repositories are deleted.
	 *
	 * @return returns true if synchronisation should stop, false otherwise
	 */
	boolean isShouldStop();

    /**
     * Call this method to update the current status of the pull request progress.
     *
     * @param pullRequestActivityCount
     * @param issues set of issues that has been affected, it doesn't have to be all affected issues in synchronisation,
     *        but only issues since the previous {@link #inPullRequestProgress(int, java.util.Set)} invocation
     */
    void inPullRequestProgress(int pullRequestActivityCount, Set<String> issues);

    /**
     * Indication whether the synchronisation has been finished
     *
     * @param finished
     */
    void setFinished(boolean finished);

    /**
     * Indication that the repository has administration permission
     *
     * @return true if the repository has administration permission, false otherwise
     */
    boolean hasAdminPermission();

    /**
     * Sets the administration permission state
     *
     * @param hasAdminPermission
     */
    void setAdminPermission(boolean hasAdminPermission);

    /**
     * get smart commit errors
     *
     * @return smart commit errors
     */
    List<SmartCommitError> getSmartCommitErrors();

    /**
     * set smart commit errors
     *
     * @param smartCommitErrors
     */
    void setSmartCommitErrors(List<SmartCommitError> smartCommitErrors);

    void setError(String error);

    EnumSet<SynchronizationFlag> getRunAgainFlags();

    void setRunAgainFlags(EnumSet<SynchronizationFlag> flags);

    Long getStartTime();

    Long getFinishTime();

    Set<String> getAffectedIssueKeys();

    Date getFirstMessageTime();

    void incrementRequestCount(Date messageTime);

    void addFlightTimeMs(int timeMs);

    int getNumRequests();

    int getFlightTimeMs();

    boolean isSoftsync();

    void setSoftsync(boolean softsync);
}