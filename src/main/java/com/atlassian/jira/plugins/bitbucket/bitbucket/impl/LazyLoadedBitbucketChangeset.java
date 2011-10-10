package com.atlassian.jira.plugins.bitbucket.bitbucket.impl;

import java.text.MessageFormat;
import java.util.List;

import com.atlassian.jira.plugins.bitbucket.bitbucket.Bitbucket;
import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketChangesetFile;
import com.atlassian.jira.plugins.bitbucket.bitbucket.RepositoryUri;
import com.atlassian.jira.plugins.bitbucket.common.Changeset;
import com.atlassian.jira.plugins.bitbucket.common.SourceControlRepository;
import com.atlassian.util.concurrent.LazyReference;

/**
 * A lazy loaded remote bitbucket changeset.  Will only load the changeset details if the
 * details that aren't stored locally are required.
 */
public class LazyLoadedBitbucketChangeset implements Changeset
{

    private final LazyReference<Changeset> lazyReference;
    private final String nodeId;
	private final SourceControlRepository repository;

    public LazyLoadedBitbucketChangeset(final Bitbucket bitbucket, final SourceControlRepository repository, final String nodeId)
    {
		this.repository = repository;
		this.lazyReference = new LazyReference<Changeset>()
        {
            protected Changeset create() throws Exception
            {
                return bitbucket.getChangeset(repository, nodeId);
            }
        };
        this.nodeId = nodeId;
    }

    private Changeset getBitbucketChangeset()
    {
        return lazyReference.get();
    }

    public String getNode()
    {
        return nodeId;
    }

    public String getRawAuthor()
    {
        return getBitbucketChangeset().getRawAuthor();
    }

    public String getAuthor()
    {
        return getBitbucketChangeset().getAuthor();
    }

    public String getTimestamp()
    {
        return getBitbucketChangeset().getTimestamp();
    }

    public String getRawNode()
    {
        return getBitbucketChangeset().getRawNode();
    }

    public String getBranch()
    {
        return getBitbucketChangeset().getBranch();
    }

    public String getMessage()
    {
        return getBitbucketChangeset().getMessage();
    }

    public List<String> getParents()
    {
        return getBitbucketChangeset().getParents();
    }

    public List<BitbucketChangesetFile> getFiles()
    {
        return getBitbucketChangeset().getFiles();
    }

    public String getRevision()
    {
        return getBitbucketChangeset().getRevision();
    }
    
	public String getRepositoryUrl()
	{
		return repository.getUrl();
	}

    public String getCommitURL()
    {
    	RepositoryUri uri = RepositoryUri.parse(repository.getUrl());
        return MessageFormat.format(DefaultBitbucketChangeset.COMMIT_URL_PATTERN, uri.getOwner(), uri.getSlug(), nodeId);
    }
}
