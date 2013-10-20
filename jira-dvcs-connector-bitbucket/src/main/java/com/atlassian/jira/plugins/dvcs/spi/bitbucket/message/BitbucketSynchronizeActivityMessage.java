package com.atlassian.jira.plugins.dvcs.spi.bitbucket.message;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.message.BaseProgressEnabledMessage;

public class BitbucketSynchronizeActivityMessage extends BaseProgressEnabledMessage implements Serializable
{

    private static final long serialVersionUID = -4361088769277502144L;

    private Set<Integer> processedPullRequests;
    private Set<Integer> processedPullRequestsLocal;
    private Date lastSyncDate;

    private int pageNum;

    public BitbucketSynchronizeActivityMessage(Repository repository,
                                               Progress progress,
                                               boolean softSync,
                                               int pageNum,
                                               Set<Integer> processedPullRequests,
                                               Set<Integer> processedPullRequestsLocal,
                                               Date lastSyncDate,
                                               int syncAuditId)
    {
        super(progress, syncAuditId, softSync, repository);
        this.pageNum = pageNum;
        this.processedPullRequests = processedPullRequests;
        this.processedPullRequestsLocal = processedPullRequestsLocal;
        this.lastSyncDate = lastSyncDate;
    }

    public BitbucketSynchronizeActivityMessage(Repository repository, boolean softSync, Date lastSyncDate, int syncAuditId)
    {
        this(repository, null, softSync, 1, new HashSet<Integer>(), new HashSet<Integer>(), lastSyncDate, syncAuditId);
    }

    public Set<Integer> getProcessedPullRequests()
    {
        return processedPullRequests;
    }

    public int getPageNum()
    {
        return pageNum;
    }

    public Set<Integer> getProcessedPullRequestsLocal()
    {
        return processedPullRequestsLocal;
    }

    public Date getLastSyncDate()
    {
        return lastSyncDate;
    }

}