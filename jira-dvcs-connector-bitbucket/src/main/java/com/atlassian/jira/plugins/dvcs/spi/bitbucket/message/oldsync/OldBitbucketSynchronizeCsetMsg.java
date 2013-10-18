package com.atlassian.jira.plugins.dvcs.spi.bitbucket.message.oldsync;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.atlassian.jira.plugins.dvcs.model.BranchHead;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.message.BaseProgressEnabledMessage;

/**
 * Message which is fired when a changeset should be synchronized.
 *
 * @see #getRefreshAfterSynchronizedAt()
 * @author Stanislav Dvorscak
 *
 */
public class OldBitbucketSynchronizeCsetMsg extends BaseProgressEnabledMessage implements Serializable
{

    /**
     * Serial version id.
     */
    private static final long serialVersionUID = 1L;

    /**
     * @see #getRepository()
     */
    private final Repository repository;

    /**
     * @see #getBranch()
     */
    private final String branch;

    /**
     * @see #getNode()
     */
    private final String node;

    /**
     * @see #getRefreshAfterSynchronizedAt()
     */
    private Date refreshAfterSynchronizedAt;

    private List<BranchHead> newHeads;

    private boolean softSync;

    /**
     * Constructor.
     *
     * @param repository
     *            {@link #getRepository()}
     * @param branch
     * @param node
     *            {@link #getNode()}
     * @param refreshAfterSynchronizedAt
     *            {@link #getRefreshAfterSynchronizedAt()}
     * @param progress
     *            {@link #getProgress()}
     * @param synchronizationTag
     *            {@link #getSynchronizationTag()}
     */
    public OldBitbucketSynchronizeCsetMsg(Repository repository, String branch, String node, Date refreshAfterSynchronizedAt,
            Progress progress, List<BranchHead> newHeads, boolean softSync, int syncAuditId)
    {
        super(progress, syncAuditId);
        this.repository = repository;
        this.branch = branch;
        this.node = node;
        this.refreshAfterSynchronizedAt = refreshAfterSynchronizedAt;
        this.newHeads = newHeads;
        this.softSync = softSync;
    }

    /**
     * @return Repository owner of changeset.
     */
    public Repository getRepository()
    {
        return repository;
    }

    /**
     * @return Branch of node.
     */
    public String getBranch()
    {
        return branch;
    }

    /**
     * @return Changeset identity.
     */
    public String getNode()
    {
        return node;
    }

    /**
     * @return Date when changeset should be resynchronized if last synchronization is after this date.
     */
    public Date getRefreshAfterSynchronizedAt()
    {
        return refreshAfterSynchronizedAt;
    }

    public List<BranchHead> getNewHeads()
    {
        return newHeads;
    }

    public boolean isSoftSync()
    {
        return softSync;
    }
}
