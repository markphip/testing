package com.atlassian.jira.plugins.bitbucket.spi.bitbucket;

import java.text.MessageFormat;

import com.atlassian.jira.plugins.bitbucket.api.impl.DefaultRepositoryUri;
import com.atlassian.jira.plugins.bitbucket.api.util.CustomStringUtils;

/**
 * Used to identify a repository, contains an owner, and a slug 
 */
public class BitbucketRepositoryUri extends DefaultRepositoryUri
{
    public BitbucketRepositoryUri(String protocol, String hostname, String owner, String slug)
    {
        super(protocol, hostname, owner, slug);
    }

    @Override
    public String getApiUrl()
    {
    	return MessageFormat.format("{0}://{1}/!api/1.0", getProtocol(), getHostname());
    }
    
    @Override
    public String getCommitUrl(String node)
    {
    	return MessageFormat.format("{0}://{1}/{2}/{3}/changeset/{4}", getProtocol(), getHostname(), getOwner(), getSlug(), node);
    }

    @Override
    public String getUserUrl(String username)
    {
    	return MessageFormat.format("{0}://{1}/{2}", getProtocol(), getHostname(), username);
    }

    @Override
    public String getRepositoryInfoUrl()
    {
        return MessageFormat.format("/repositories/{0}/{1}", CustomStringUtils.encode(getOwner()), CustomStringUtils.encode(getSlug()));  
    }

    @Override
    public String getFileCommitUrl(String node, String file, int counter)
    {
        return MessageFormat.format("{0}#chg-{1}", getCommitUrl(node), file);
    }

    @Override
    public String getParentUrl(String parentNode)
    {
        return MessageFormat.format("{0}://{1}/{2}/{3}/changeset/{4}", getProtocol(), getHostname(), getOwner(), getSlug(), parentNode);
    }
}
