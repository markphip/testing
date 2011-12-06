package com.atlassian.jira.plugins.bitbucket.api.impl;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryUri;

public class DefaultSourceControlRepository implements SourceControlRepository
{
	private final int id;
	private final String username;
	private final String password;
    private final RepositoryUri repositoryUri;
    private final String projectKey;
	private final String adminUsername;
	private final String adminPassword;
    private final String repositoryType;
    private final String accessToken;

    public DefaultSourceControlRepository(int id, String repositoryType, RepositoryUri repositoryUri, String projectKey, String username, String password,
			String adminUsername, String adminPassword, String accessToken)
	{
		this.id = id;
        this.repositoryUri = repositoryUri;
        this.projectKey = projectKey;
		this.username = username;
		this.password = password;
		this.adminUsername = adminUsername;
		this.adminPassword = adminPassword;
        this.repositoryType = repositoryType;
        this.accessToken = accessToken;
    }
    
	@Override
    public int getId()
	{
		return id;
	}

	@Override
	public String getRepositoryType()
	{
	    return repositoryType;
	}

	@Override
    public RepositoryUri getRepositoryUri()
    {
        return repositoryUri;
    }

    @Override
    public String getProjectKey()
	{
		return projectKey;
	}
	

	@Override
    public String getUsername()
	{
		return username;
	}
	
	@Override
    public String getPassword()
	{
		return password;
	}

	@Override
    public String getAdminUsername()
	{
		return adminUsername;
	}

	@Override
    public String getAdminPassword()
	{
		return adminPassword;
	}

	@Override
    public String getAccessToken()
	{
	    return accessToken;
	}

    @Override
	public boolean equals(Object obj)
	{
		if (obj == null) return false;
		if (this==obj) return true;
		if (this.getClass()!=obj.getClass()) return false;
		DefaultSourceControlRepository that = (DefaultSourceControlRepository) obj;
		return new EqualsBuilder().append(id, that.id).append(repositoryUri, that.repositoryUri)
			.append(projectKey, that.projectKey).append(username, that.username)
			.append(password, that.password).append(adminUsername, that.adminUsername)
			.append(adminPassword, that.adminPassword).append(accessToken, that.accessToken).isEquals();
	}
	
	@Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37).append(id).append(repositoryUri).append(projectKey)
            .append(username).append(password).append(adminUsername).append(adminPassword).append(accessToken)
            .toHashCode();
    }
}
