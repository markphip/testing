package com.atlassian.jira.plugins.dvcs.service.api;

import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.RepositoryServiceImpl;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for {@link com.atlassian.jira.plugins.dvcs.service.api.DvcsRepositoryServiceImpl} class
 */
public class DvcsRepositoryServiceImplTest
{
    private static final String AUTHOR = "jano";

    @InjectMocks
    private RepositoryServiceImpl repositoryService;

    @Mock
    private DvcsCommunicatorProvider communicatorProvider;

    @Mock
    private DvcsCommunicator communicator;

    private DvcsRepositoryServiceImpl dvcsRepositoryService;

    @Mock
    private Repository repository;

    @BeforeMethod
    public void initializeMocks()
    {
        MockitoAnnotations.initMocks(this);
        dvcsRepositoryService = new DvcsRepositoryServiceImpl(repositoryService);

        Mockito.when(communicatorProvider.getCommunicator(repository.getDvcsType())).thenReturn(communicator);
    }

    @Test
    public void getDvcsUser_shouldNotThrowExceptionWhenSyncDisabled()
    {
        Mockito.doThrow(new SourceControlException.SynchronizationDisabled("Synchronization disabled")).when(communicator).checkSyncDisabled();
        Assert.assertNotNull(dvcsRepositoryService.getDvcsUser(repository, AUTHOR, AUTHOR));
    }
}
