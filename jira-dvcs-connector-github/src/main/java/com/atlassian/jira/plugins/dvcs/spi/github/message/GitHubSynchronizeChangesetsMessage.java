package com.atlassian.jira.plugins.dvcs.spi.github.message;

import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.message.BaseProgressEnabledMessage;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * Message which is fired when a changeset should be synchronized.
 *
 * @see #getRefreshAfterSynchronizedAt()
 * @author Miroslav Stencel
 *
 */
public class GitHubSynchronizeChangesetsMessage extends BaseProgressEnabledMessage implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String firstSha;
    private Date refreshAfterSynchronizedAt;
    private Map<String, String> nodesToBranches;
    private Date lastCommitDate;
    private int pagelen;

    public GitHubSynchronizeChangesetsMessage(Repository repository, Date refreshAfterSynchronizedAt,
            Progress progress, String firstSha, Date lastCommitDate, int pagelen,
            Map<String, String> nodesToBranches, boolean softSync, int syncAuditId)
    {
        super(progress, syncAuditId, softSync, repository);
        this.refreshAfterSynchronizedAt = refreshAfterSynchronizedAt;
        this.nodesToBranches = nodesToBranches;
        this.firstSha = firstSha;
        this.lastCommitDate = lastCommitDate;
        this.pagelen = pagelen;
    }

    public Date getRefreshAfterSynchronizedAt()
    {
        return refreshAfterSynchronizedAt;
    }

    public Map<String, String> getNodesToBranches()
    {
        return nodesToBranches;
    }

    public String getFirstSha()
    {
        return firstSha;
    }

    public Date getLastCommitDate()
    {
        return lastCommitDate;
    }

    public int getPagelen()
    {
        return pagelen;
    }
}
