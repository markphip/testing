package com.atlassian.jira.plugins.dvcs.rest;


import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.plugins.dvcs.service.admin.DevSummaryCachePrimingStatus;
import com.atlassian.jira.plugins.dvcs.service.admin.DevSummaryChangedEventServiceImpl;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import static com.atlassian.jira.permission.GlobalPermissionKey.SYSTEM_ADMIN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

public class DevSummaryChangedEventResourceTest
{
    private static final int PAGE_SIZE = 100;

    @Mock
    private DevSummaryChangedEventServiceImpl devSummaryChangedEventService;

    @Mock
    private JiraAuthenticationContext authenticationContext;

    @Mock
    private GlobalPermissionManager globalPermissionManager;

    @Mock
    private FeatureManager featureManager;

    @Mock
    private UnifiedUser user;

    private DevSummaryChangedEventResource devSummaryChangedEventResource;

    @BeforeMethod
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        devSummaryChangedEventResource = new DevSummaryChangedEventResource(devSummaryChangedEventService, featureManager,
                globalPermissionManager, authenticationContext);
        ReflectionTestUtils.setField(devSummaryChangedEventResource, "devSummaryChangedEventService", devSummaryChangedEventService);
        when(authenticationContext.getUser()).thenReturn(user);
        when(globalPermissionManager.hasPermission(SYSTEM_ADMIN, user)).thenReturn(true);
        when(featureManager.isOnDemand()).thenReturn(true);
    }

    @Test
    public void testStartPrimingSuccess()
    {
        when(devSummaryChangedEventService.generateDevSummaryEvents(PAGE_SIZE)).thenReturn(true);
        Response response = devSummaryChangedEventResource.startGeneration(PAGE_SIZE);
        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
    }

    @Test
    public void testStartPrimingFailure()
    {
        when(devSummaryChangedEventService.generateDevSummaryEvents(PAGE_SIZE)).thenReturn(false);
        Response response = devSummaryChangedEventResource.startGeneration(PAGE_SIZE);
        assertThat(response.getStatus(), is(Status.CONFLICT.getStatusCode()));
    }

    @Test
    public void testStartPrimingNonAdmin()
    {
        when(globalPermissionManager.hasPermission(SYSTEM_ADMIN, user)).thenReturn(false);
        Response response = devSummaryChangedEventResource.startGeneration(PAGE_SIZE);
        assertThat(response.getStatus(), is(Status.UNAUTHORIZED.getStatusCode()));
    }

    @Test
    public void testStartPrimingNonOD()
    {
        when(featureManager.isOnDemand()).thenReturn(false);
        Response response = devSummaryChangedEventResource.startGeneration(PAGE_SIZE);
        assertThat(response.getStatus(), is(Status.FORBIDDEN.getStatusCode()));
    }

    @Test
    public void testStatus()
    {
        when(devSummaryChangedEventService.getEventGenerationStatus()).thenReturn(new DevSummaryCachePrimingStatus());
        Response response = devSummaryChangedEventResource.generationStatus();
        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
    }

    @Test
    public void testStatusNonAdmin()
    {
        when(globalPermissionManager.hasPermission(SYSTEM_ADMIN, user)).thenReturn(false);
        Response response = devSummaryChangedEventResource.generationStatus();
        assertThat(response.getStatus(), is(Status.UNAUTHORIZED.getStatusCode()));
    }

    @Test
    public void testStop()
    {

        Response response = devSummaryChangedEventResource.stopGeneration();
        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
    }

    @Test
    public void testStopNonAdmin()
    {
        when(globalPermissionManager.hasPermission(SYSTEM_ADMIN, user)).thenReturn(false);
        Response response = devSummaryChangedEventResource.stopGeneration();
        assertThat(response.getStatus(), is(Status.UNAUTHORIZED.getStatusCode()));
    }
}
