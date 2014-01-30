package com.atlassian.jira.plugins.dvcs.activity;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.model.Participant;

public interface RepositoryPullRequestDao
{
    // C-U-D
    RepositoryPullRequestMapping savePullRequest(Repository domain, Map<String, Object> activity);

    RepositoryPullRequestMapping updatePullRequestInfo(int localId, String name, String sourceBranch, String dstBranch, RepositoryPullRequestMapping.Status status,
            Date updatedOn, String sourceRepo, final int commentCount);

    /**
     * Updates issue keys related to commits of provided repository.
     *
     * @param domain
     */
    void updateCommitIssueKeys(Repository domain);

    /**
     * Updates issue keys related to the provided pull request to reflect current state.
     *
     * @param pullRequestId
     *
     * @return Set of found issues keys
     */
    Set<String> updatePullRequestIssueKeys(Repository domain, int pullRequestId);

    void removeAll(Repository domain);

    RepositoryCommitMapping saveCommit(Repository domain, Map<String, Object> commit);

    void linkCommit(Repository domain, RepositoryPullRequestMapping request, RepositoryCommitMapping commit);

    void unlinkCommit(Repository domain, RepositoryPullRequestMapping request, RepositoryCommitMapping commit);

    // R

    List<RepositoryPullRequestMapping> getPullRequestsForIssue(final Iterable<String> issueKeys);

    RepositoryPullRequestMapping findRequestById(int localId);

    RepositoryPullRequestMapping findRequestByRemoteId(Repository domain, long remoteId);

    Set<String> getExistingIssueKeysMapping(Repository domain, Integer pullRequestId);

    RepositoryCommitMapping getCommit(Repository domain, int pullRequesCommitId);

    RepositoryCommitMapping getCommitByNode(Repository domain, int pullRequestId, String node);

    RepositoryCommitMapping getCommitByNode(Repository domain, String node);

    PullRequestParticipantMapping[] getParticipants(int pullRequestId);

    void removeParticipant(PullRequestParticipantMapping participantMapping);

    void saveParticipant(PullRequestParticipantMapping participantMapping);

    void createParticipant(int pullRequestId, int repositoryId, Participant participant);

    List<RepositoryPullRequestMapping> getPullRequestsForIssue(Iterable<String> issueKeys, String dvcsType);
}
