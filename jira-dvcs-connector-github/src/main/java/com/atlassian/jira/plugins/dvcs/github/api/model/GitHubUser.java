package com.atlassian.jira.plugins.dvcs.github.api.model;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * GitHub user.
 * 
 * @author Stanislav Dvorscak
 * 
 */
@XmlRootElement
@XmlType(propOrder = { "login" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubUser
{

    /**
     * @see #getLogin()
     */
    private String login;

    /**
     * Constructor.
     */
    public GitHubUser()
    {
    }

    /**
     * @return Login (username) of this user.
     */
    public String getLogin()
    {
        return login;
    }

    /**
     * @param login
     *            {@link #getLogin()}
     */
    public void setLogin(String login)
    {
        this.login = login;
    }

}
