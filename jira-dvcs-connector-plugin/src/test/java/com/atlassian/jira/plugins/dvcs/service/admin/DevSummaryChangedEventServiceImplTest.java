package com.atlassian.jira.plugins.dvcs.service.admin;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestDao;
import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao;
import com.atlassian.jira.plugins.dvcs.dao.IssueToMappingFunction;
import com.atlassian.jira.plugins.dvcs.event.EventService;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.sal.api.executor.ThreadLocalDelegateExecutorFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.Executor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DevSummaryChangedEventServiceImplTest
{
    private static final int TOTAL_NUMBER_OF_ISSUE_KEYS = 1000;
    private static final int TOTAL_NUMBER_OF_PULL_REQUEST_ISSUE_KEYS = 500;
    private static final String ISSUE_KEY = "TEST-1";
    private static final ImmutableSet<String> ISSUE_KEYS = ImmutableSet.of(ISSUE_KEY);
    private static final int PAGE_SIZE = 100;

    @Mock
    private ChangesetDao changesetDao;

    @Mock
    private RepositoryPullRequestDao repositoryPullRequestDao;

    @Mock
    private EventService eventService;

    @Mock
    private DevSummaryCachePrimingStatus status;

    @Mock
    private ThreadLocalDelegateExecutorFactory executorFactory;

    @Mock
    private Executor executor;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private Organization organization;

    @Mock
    private Repository repository;

    private DevSummaryChangedEventServiceImpl administrationService;

    @BeforeMethod
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        when(changesetDao.getNumberOfIssueKeysToChangeset()).thenReturn(TOTAL_NUMBER_OF_ISSUE_KEYS);
        when(repositoryPullRequestDao.getNumberOfIssueKeysToPullRequests()).thenReturn(TOTAL_NUMBER_OF_PULL_REQUEST_ISSUE_KEYS);
        when(executorFactory.createExecutor(any(Executor.class))).thenReturn(executor);

        administrationService = new DevSummaryChangedEventServiceImpl(executorFactory);
        ReflectionTestUtils.setField(administrationService, "changesetDao", changesetDao);
        ReflectionTestUtils.setField(administrationService, "repositoryPullRequestDao", repositoryPullRequestDao);
        ReflectionTestUtils.setField(administrationService, "status", status);
        ReflectionTestUtils.setField(administrationService, "organizationService", organizationService);

        when(repository.isLinked()).thenReturn(true);
        when(organization.getRepositories()).thenReturn(ImmutableList.of(repository));
        when(organizationService.getAll(eq(true))).thenReturn(ImmutableList.of(organization));
    }

    @Test
    public void testNotRunning()
    {
        when(status.startExclusively(anyInt(), anyInt())).thenReturn(true);

        assertThat(administrationService.generateDevSummaryEvents(PAGE_SIZE), is(true));
    }

    @Test
    public void testUnlinkedRepositoryNotUsed()
    {
        when(repository.isLinked()).thenReturn(false);
        final RuntimeException expectedException = new RuntimeException("foo");
        when(changesetDao.forEachIssueKeyMapping(eq(organization), eq(repository), anyInt(), any(IssueToMappingFunction.class)))
                .thenThrow(expectedException);

        administrationService.startPriming(PAGE_SIZE);

        verify(status).finished(any(String.class));
    }

    @Test
    public void testExceptionOnChangesetFailsStatus()
    {
        final RuntimeException expectedException = new RuntimeException("foo");
        when(changesetDao.forEachIssueKeyMapping(eq(organization), eq(repository), eq(PAGE_SIZE), any(IssueToMappingFunction.class)))
                .thenThrow(expectedException);

        administrationService.startPriming(PAGE_SIZE);
        verify(status).failed(eq(expectedException), any(String.class));
    }

    @Test
    public void testExceptionOnPullRequestFailsStatus()
    {
        final RuntimeException expectedException = new RuntimeException("foo");
        when(repositoryPullRequestDao.forEachIssueKeyMapping(eq(organization), eq(repository), eq(PAGE_SIZE), any(IssueToMappingFunction.class)))
                .thenThrow(expectedException);

        administrationService.startPriming(PAGE_SIZE);
        verify(status).failed(eq(expectedException), any(String.class));
    }

    @Test
    public void testRunning()
    {
        when(status.startExclusively(anyInt(), anyInt())).thenReturn(false);
        assertThat(administrationService.generateDevSummaryEvents(PAGE_SIZE), is(false));
    }

    @Test
    public void testClosureReturnsFalseWhenStopped()
    {
        DevSummaryChangedEventServiceImpl.PrimeCacheClosure closure =
                new DevSummaryChangedEventServiceImpl.ChangesetPrimeCacheClosure(eventService, status);
        when(status.isStopped()).thenReturn(true);

        assertThat(closure.execute("bitbucket", 1, ISSUE_KEYS), is(false));
    }

    @Test
    public void testClosureReturnsTrueWhenRunning()
    {
        DevSummaryChangedEventServiceImpl.PrimeCacheClosure closure = new DevSummaryChangedEventServiceImpl.ChangesetPrimeCacheClosure(eventService, status);
        when(status.isStopped()).thenReturn(false);

        assertThat(closure.execute("bitbucket", 1, ISSUE_KEYS), is(true));
    }
}
