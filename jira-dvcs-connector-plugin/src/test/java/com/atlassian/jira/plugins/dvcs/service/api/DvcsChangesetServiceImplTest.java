package com.atlassian.jira.plugins.dvcs.service.api;

import com.atlassian.beehive.compat.ClusterLockServiceFactory;
import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao;
import com.atlassian.jira.plugins.dvcs.dao.RepositoryDao;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetServiceImpl;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * Test for {@link com.atlassian.jira.plugins.dvcs.service.api.DvcsChangesetServiceImpl} class
 */
public class DvcsChangesetServiceImplTest
{
    private static final int REPO_ID = 1;
    private static final String ISSUE_KEY = "TST-1";
    private static final String NODE = "node";

    private ChangesetServiceImpl changesetService;

    private DvcsChangesetServiceImpl dvcsChangesetService;

    @Mock
    private Repository repository;

    @Mock
    private Changeset changeset;

    @Mock
    private ChangesetDao changesetDao;

    @Mock
    private RepositoryDao repositoryDao;

    @Mock
    private DvcsCommunicatorProvider communicatorProvider;

    @Mock
    private DvcsCommunicator communicator;

    @Mock
    private ClusterLockServiceFactory clusterLockServiceFactory;

    @BeforeMethod
    public void initializeMocks()
    {

        MockitoAnnotations.initMocks(this);
        changesetService = new ChangesetServiceImpl(changesetDao, clusterLockServiceFactory);
        ReflectionTestUtils.setField(changesetService, "repositoryDao", repositoryDao);
        ReflectionTestUtils.setField(changesetService, "dvcsCommunicatorProvider", communicatorProvider);

        dvcsChangesetService = new DvcsChangesetServiceImpl(changesetService);

        when(repositoryDao.get(REPO_ID)).thenReturn(repository);

        when(communicatorProvider.getCommunicator(repository.getDvcsType())).thenReturn(communicator);
        when(repository.getId()).thenReturn(REPO_ID);
        when(changeset.getRepositoryId()).thenReturn(REPO_ID);
        when(changeset.getVersion()).thenReturn(0);
        when(changeset.getNode()).thenReturn(NODE);
    }

    @Test
    public void getChangesetsWithFileDetails_shouldNotThrowExceptionWhenSyncDisabled()
    {
        mockSyncDisabled();
        dvcsChangesetService.getChangesetsWithFileDetails(Collections.singletonList(changeset));
    }

    @Test
    public void getChangesets_shouldNotThrowExceptionWhenSyncDisabled()
    {
        mockSyncDisabled();
        when(changesetDao.getByIssueKey(any(Iterable.class), anyBoolean())).thenReturn(Collections.singletonList(changeset));
        when(changesetDao.getByIssueKey(any(Iterable.class), eq(BitbucketCommunicator.BITBUCKET), anyBoolean())).thenReturn(Collections.singletonList(changeset));

        assertThat(dvcsChangesetService.getChangesets(Collections.singletonList(ISSUE_KEY)), allOf(notNullValue(), not(empty())));
        assertThat(dvcsChangesetService.getChangesets(Collections.singletonList(ISSUE_KEY), BitbucketCommunicator.BITBUCKET), allOf(notNullValue(), not(empty())));
    }

    private void mockSyncDisabled()
    {
        when(communicator.isSyncDisabled()).thenReturn(true);
        when(communicatorProvider.getCommunicatorAndCheckSyncDisabled(repository.getDvcsType())).thenThrow(new SourceControlException.SynchronizationDisabled("Synchronization disabled"));
        Mockito.doThrow(new SourceControlException.SynchronizationDisabled("Synchronization disabled")).when(communicator).checkSyncDisabled();
    }
}
