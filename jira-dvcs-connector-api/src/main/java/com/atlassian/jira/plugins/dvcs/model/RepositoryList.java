package com.atlassian.jira.plugins.dvcs.model;

import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A list of repositories
 */
@XmlRootElement(name = "repositories")
@XmlAccessorType(XmlAccessType.FIELD)
public class RepositoryList
{
    private List<Repository> repositories;
    private Set<Integer> addingOrgs;

    public RepositoryList()
    {
    }

    public RepositoryList(final List<Repository> list, final Set<Integer> addingOrgs)
    {
        this.repositories = list;
        this.addingOrgs = addingOrgs;
    }

    public List<Repository> getRepositories()
    {
        return repositories;
    }

    public void setRepositories(final List<Repository> repositories)
    {
        this.repositories = repositories;
    }

    public Set<Integer> getAddingOrgs()
    {
        return addingOrgs;
    }

    public void setAddingOrgs(final Set<Integer> addingOrgs)
    {
        this.addingOrgs = addingOrgs;
    }
}
