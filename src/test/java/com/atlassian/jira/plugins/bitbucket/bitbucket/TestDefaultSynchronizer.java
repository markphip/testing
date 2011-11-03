package com.atlassian.jira.plugins.bitbucket.bitbucket;

import com.atlassian.jira.plugins.bitbucket.DefaultSynchronizer;
import com.atlassian.jira.plugins.bitbucket.api.*;
import com.atlassian.jira.plugins.bitbucket.spi.Communicator;
import com.atlassian.jira.plugins.bitbucket.spi.DefaultSynchronisationOperation;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryManager;
import com.atlassian.jira.plugins.bitbucket.spi.SynchronisationOperation;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DefaultSynchronizer}
 */
public class TestDefaultSynchronizer
{
	@Mock
	private Communicator bitbucket;
	@Mock
	private Changeset changeset;
	@Mock
	private RepositoryManager repositoryManager;
	@Mock
	private SourceControlRepository repository;
	@Mock
	private ProgressWriter progressProvider;

	@Before
	public void setup()
	{
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testSynchronizeAddsSingleMapping() throws InterruptedException
	{
		when(repository.getUrl()).thenReturn("https://bitbucket.org/owner/slug");
		when(repository.getProjectKey()).thenReturn("PRJ");
		SynchronizationKey key = new SynchronizationKey(repository);
		SynchronisationOperation synchronisation = new DefaultSynchronisationOperation(key, repositoryManager, bitbucket, progressProvider);
		when(repositoryManager.getSynchronisationOperation(any(SynchronizationKey.class), any(ProgressWriter.class))).thenReturn(synchronisation);
		when(bitbucket.getChangesets(repository)).thenReturn(Arrays.asList(changeset));
		when(changeset.getMessage()).thenReturn("PRJ-1 Message");

		DefaultSynchronizer synchronizer = new DefaultSynchronizer(Executors.newSingleThreadExecutor(), repositoryManager);
		assertNull(synchronizer.getProgress(repository));
		synchronizer.synchronize(repository);
		assertNotNull(synchronizer.getProgress(repository));
		
		Progress progress = synchronizer.getProgress(repository);
		while (!progress.isFinished())
		{
			Thread.sleep(10);
		}
		verify(repositoryManager, times(1)).addChangeset(repository, "PRJ-1", changeset);
	}

}
