package com.atlassian.jira.plugins.dvcs.model.dev;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 *
 */
@XmlRootElement
@XmlAccessorType (XmlAccessType.FIELD)
public class RestPullRequest
{
    private RestAuthor author;
    private long authorTimestamp;
    private long updateOn;
    private long id;
    private String title;
    private String url;
    private String status;

    public RestAuthor getAuthor()
    {
        return author;
    }

    public void setAuthor(final RestAuthor author)
    {
        this.author = author;
    }

    public long getAuthorTimestamp()
    {
        return authorTimestamp;
    }

    public void setAuthorTimestamp(final long authorTimestamp)
    {
        this.authorTimestamp = authorTimestamp;
    }

    public long getId()
    {
        return id;
    }

    public void setId(final long id)
    {
        this.id = id;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(final String title)
    {
        this.title = title;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(final String url)
    {
        this.url = url;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(final String status)
    {
        this.status = status;
    }

    public long getUpdateOn()
    {
        return updateOn;
    }

    public void setUpdateOn(final long updateOn)
    {
        this.updateOn = updateOn;
    }
}

