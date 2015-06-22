package com.atlassian.jira.plugins.dvcs.util;

import com.atlassian.jira.plugins.dvcs.activity.PullRequestParticipantMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryCommitMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import net.java.ao.EntityManager;

import java.beans.PropertyChangeListener;
import java.util.Date;
import javax.annotation.Nullable;

public class RepositoryPullRequestMappingMock implements RepositoryPullRequestMapping
{
    private Date createdOn;
    private Date updatedOn;
    private int commentCount;
    private long remoteId;
    private int domainId;
    private int repoId;
    private PullRequestParticipantMapping[] participants = new PullRequestParticipantMapping[0];
    private RepositoryCommitMapping[] commits = new RepositoryCommitMapping[0];
    private String author;
    private String destinationBranch;
    private String executedBy;
    private String lastStatus;
    private String name;
    private String sourceBranch;
    private String sourceRepo;
    private String url;

    @Override
    public Long getRemoteId()
    {
        return remoteId;
    }

    @Override
    public int getToRepositoryId()
    {
        return repoId;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String getUrl()
    {
        return url;
    }

    @Override
    public String getSourceBranch()
    {
        return sourceBranch;
    }

    @Override
    public String getDestinationBranch()
    {
        return destinationBranch;
    }

    @Override
    public String getLastStatus()
    {
        return lastStatus;
    }

    @Override
    public Date getCreatedOn()
    {
        return createdOn;
    }

    @Override
    public Date getUpdatedOn()
    {
        return updatedOn;
    }

    @Override
    public String getAuthor()
    {
        return author;
    }

    @Override
    public RepositoryCommitMapping[] getCommits()
    {
        return commits;
    }

    @Override
    public String getSourceRepo()
    {
        return sourceRepo;
    }

    @Override
    public PullRequestParticipantMapping[] getParticipants()
    {
        return participants;
    }

    @Override
    public int getCommentCount()
    {
        return commentCount;
    }

    @Override
    public String getExecutedBy()
    {
        return executedBy;
    }

    @Override
    public void setRemoteId(final Long remoteId)
    {
        this.remoteId = remoteId;
    }

    @Override
    public void setToRepositoryId(final int repoId)
    {
        this.repoId = repoId;
    }

    @Override
    public void setName(final String name)
    {
        this.name = name;
    }

    @Override
    public void setUrl(final String url)
    {
        this.url = url;
    }

    @Override
    public void setSourceBranch(final String branch)
    {
        this.sourceBranch = branch;
    }

    @Override
    public void setDestinationBranch(final String branch)
    {
        this.destinationBranch = branch;
    }

    @Override
    public void setLastStatus(final String status)
    {
        this.lastStatus = status;
    }

    @Override
    public void setCreatedOn(final Date date)
    {
        this.createdOn = date;
    }

    @Override
    public void setUpdatedOn(final Date date)
    {
        this.updatedOn = date;
    }

    @Override
    public void setAuthor(final String author)
    {
        this.author = author;
    }

    @Override
    public void setSourceRepo(final String sourceRepo)
    {
        this.sourceRepo = sourceRepo;
    }

    @Override
    public void setCommentCount(final int commentCount)
    {
        this.commentCount = commentCount;
    }

    @Override
    public void setExecutedBy(final String user)
    {
        this.executedBy = user;
    }

    @Override
    public int getDomainId()
    {
        return domainId;
    }

    @Override
    public void setDomainId(final int domainId)
    {
        this.domainId = domainId;
    }

    @Override
    public int getID()
    {
        return 0;
    }

    @Override
    public void init()
    {

    }

    @Override
    public void save()
    {

    }

    @Override
    public EntityManager getEntityManager()
    {
        return null;
    }

    @Override
    public void addPropertyChangeListener(final PropertyChangeListener propertyChangeListener)
    {

    }

    @Override
    public void removePropertyChangeListener(final PropertyChangeListener propertyChangeListener)
    {

    }

    @Override
    public Class getEntityType()
    {
        return RepositoryPullRequestMapping.class;
    }

    public void setCommits(final RepositoryCommitMapping[] commits)
    {
        this.commits = commits;
    }
    
    public void setParticipants(@Nullable final PullRequestParticipantMapping[] participants)
    {
        this.participants = participants;
    }
}
