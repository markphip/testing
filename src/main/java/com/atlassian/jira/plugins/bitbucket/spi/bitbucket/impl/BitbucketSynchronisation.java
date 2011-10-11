package com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.bitbucket.DefaultSynchronizer;
import com.atlassian.jira.plugins.bitbucket.Progress;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.api.SynchronizationKey;
import com.atlassian.jira.plugins.bitbucket.spi.AbstractSynchronisationOperation;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryManager;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.BitbucketCommunicator;
import com.google.common.base.Function;

public class BitbucketSynchronisation extends AbstractSynchronisationOperation
{
    private final Logger logger = LoggerFactory.getLogger(DefaultSynchronizer.class);

	private BitbucketCommunicator bitbucketCommunicator;

    public BitbucketSynchronisation(SynchronizationKey key, RepositoryManager repositoryManager,
			BitbucketCommunicator bitbucketCommunicator, Function<SynchronizationKey, Progress> progressProvider)
	{
    	super(key, repositoryManager, progressProvider);
		this.bitbucketCommunicator = bitbucketCommunicator;
	}

	public Iterable<Changeset> getChangsetsIterator()
	{
		logger.debug("synchronize [ {} ] with [ {} ]", key.getProjectKey(), key.getRepositoryUrl());

        SourceControlRepository repository = repositoryManager.getRepository(key.getProjectKey(), key.getRepositoryUrl());
        Iterable<Changeset> changesets = key.getChangesets() == null ? bitbucketCommunicator.getChangesets(repository) : key.getChangesets();
		return changesets;
	}

}
