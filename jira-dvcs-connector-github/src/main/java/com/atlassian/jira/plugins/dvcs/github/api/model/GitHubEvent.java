package com.atlassian.jira.plugins.dvcs.github.api.model;

import java.util.Date;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * Represents event which happened in GitHub.
 * 
 * @author Stanislav Dvorscak
 * 
 */
@XmlRootElement
@XmlType(propOrder = { "id" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubEvent
{

    /**
     * @see #getId()
     */
    private String id;

    /**
     * @see #getCreatedAt()
     */
    @XmlElement(name = "created_at")
    private Date createdAt;

    /**
     * @see #getType()
     */
    private String type;

    /**
     * @see #getPayload()
     */
    private Map<String, Object> payload;

    /**
     * Constructor.
     */
    public GitHubEvent()
    {
    }

    /**
     * @return Identity of this event (it is unique over whole system, not only according to repository).
     */
    public String getId()
    {
        return id;
    }

    /**
     * @param id
     *            {@link #getId()}
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * @return Date when this event was created.
     */
    public Date getCreatedAt()
    {
        return createdAt;
    }

    /**
     * @param createdAt
     *            {@link #getCreatedAt()}
     */
    public void setCreatedAt(Date createdAt)
    {
        this.createdAt = createdAt;
    }

    /**
     * @return type of payload
     */
    public String getType()
    {
        return type;
    }

    /**
     * @param type
     *            {@link #getType()}
     */
    public void setType(String type)
    {
        this.type = type;
    }

    /**
     * @return Payload of event.
     */
    public Map<String, Object> getPayload()
    {
        return payload;
    }

    /**
     * @param payload
     *            {@link #getPayload()}
     */
    public void setPayload(Map<String, Object> payload)
    {
        this.payload = payload;
    }

}
