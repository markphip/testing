package com.atlassian.jira.plugins.dvcs.spi.github.message;

import java.io.Serializable;
import java.util.Date;

import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.message.MessageTag;

/**
 * Message which is fired when a changeset should be synchronized.
 * 
 * @see #getRefreshAfterSynchronizedAt()
 * @author Stanislav Dvorscak
 * 
 */
public class SynchronizeChangesetMessage implements Serializable
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

    /**
     * @see #getProgress()
     */
    private Progress progress;

    /**
     * @see #getSynchronizationTag()
     */
    private final MessageTag synchronizationTag;

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
     */
    public SynchronizeChangesetMessage(Repository repository, String branch, String node, Date refreshAfterSynchronizedAt, Progress progress)
    {
        this(repository, branch, node, refreshAfterSynchronizedAt, progress, new MessageTag()
        {

            private static final long serialVersionUID = 1L;

        });
    }

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
    public SynchronizeChangesetMessage(Repository repository, String branch, String node, Date refreshAfterSynchronizedAt,
            Progress progress, MessageTag synchronizationTag)
    {
        this.repository = repository;
        this.branch = branch;
        this.node = node;
        this.refreshAfterSynchronizedAt = refreshAfterSynchronizedAt;
        this.progress = progress;

        this.synchronizationTag = synchronizationTag;
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

    /**
     * @return progress of synchronization
     */
    public Progress getProgress()
    {
        return progress;
    }

    /**
     * @return Tag of synchronization - necessary by progress tracking.
     */
    public MessageTag getSynchronizationTag()
    {
        return synchronizationTag;
    }

}
