package com.atlassian.jira.plugins.dvcs.dao;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.GlobalFilter;

import java.util.List;
import java.util.Set;

/**
 *
 */
public interface ChangesetDao
{
    /**
     * Removes all changesets from given repository
     * 
     * @param repositoryId
     */
    void removeAllInRepository(int repositoryId);

    /**
     * create Changeset and save to storage. If it's new object (without ID) after this operation it will have it assigned.
     * it's create alse all associations (repository- changeset, issues-changest)
     *
     * @param changeset
     * @param extractedIssues
     * @return
     */
    Changeset create(Changeset changeset, Set<String> extractedIssues);

    /**
     * update properties of changeset which is already saved in DB
     *
     * @param changeset
     * @return
     */
    Changeset update(Changeset changeset);

    /**
     * @param repositoryId
     * @param changesetNode
     * @return
     */
    Changeset getByNode(int repositoryId, String changesetNode);

    /**
     * Returns all changetsets related to given issueKey
     * 
     * @param issueKey
     * @return
     */
    List<Changeset> getByIssueKey(String issueKey);

    /**
     * Returns latest changesets. Used by activity stream.
     * 
     * @param maxResults
     * @param gf
     * @return
     */
    List<Changeset> getLatestChangesets(int maxResults, GlobalFilter gf);
    
    /**
     * Returns lists of latest commits that need to be processed by smartcommits logic.
     * 
     * @param closure
     */
    void forEachLatestChangesetsAvailableForSmartcommitDo(ForEachChangesetClosure closure);
    
    /**
     * 
     * @param id
     * @param available
     */
    void markSmartcommitAvailability(int id, boolean available);
    
    /**
     * From the changesets in database find all referenced project keys.
     *
     * @param repositoryId the repository id
     * @return the project keys by repository
     */
    Set<String> findReferencedProjects(int repositoryId);
    
    /**
     *
     */
    public interface ForEachChangesetClosure
    {
        /**
         * @param changeset
         */
        void execute(ChangesetMapping changesetMapping);
    }
}