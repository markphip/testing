package com.atlassian.jira.plugins.dvcs.rest;

import com.atlassian.jira.compatibility.util.ApplicationUserUtil;
import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.plugins.dvcs.service.admin.DevSummaryCachePrimingStatus;
import com.atlassian.jira.plugins.dvcs.service.admin.DevSummaryChangedEventServiceImpl;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import static com.atlassian.jira.permission.GlobalPermissionKey.SYSTEM_ADMIN;
import static com.google.common.base.Preconditions.checkNotNull;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;

/**
 * REST resource for generating dev summary changed events
 */
@Path ("/event/dev-summary-changed")
public class DevSummaryChangedEventResource
{
    private final DevSummaryChangedEventServiceImpl devSummaryChangedEventService;
    private final FeatureManager featureManager;
    private final GlobalPermissionManager globalPermissionManager;
    private final JiraAuthenticationContext authenticationContext;

    public DevSummaryChangedEventResource(
            @ComponentImport final FeatureManager featureManager,
            final GlobalPermissionManager globalPermissionManager,
            final JiraAuthenticationContext authenticationContext,
            final DevSummaryChangedEventServiceImpl devSummaryChangedEventService)
    {
        this.featureManager = checkNotNull(featureManager);
        this.globalPermissionManager = checkNotNull(globalPermissionManager);
        this.authenticationContext = checkNotNull(authenticationContext);
        this.devSummaryChangedEventService = checkNotNull(devSummaryChangedEventService);
    }

    @Produces (MediaType.TEXT_PLAIN)
    @POST
    public Response startGeneration(@FormParam ("pageSize") @DefaultValue ("100") int pageSize)
    {
        ApplicationUser user = ApplicationUserUtil.from(authenticationContext.getLoggedInUser());
        if (!globalPermissionManager.hasPermission(SYSTEM_ADMIN, user))
        {
            return response(Status.UNAUTHORIZED, null);
        }

        if (!featureManager.isOnDemand())
        {
            return response(FORBIDDEN, "Only available on Cloud instances");
        }

        if (devSummaryChangedEventService.generateDevSummaryEvents(pageSize))
        {
            return Response.status(Status.OK).entity("event generation is scheduled").build();
        }
        else
        {
            return Response.status(Status.CONFLICT).entity("event generation is already scheduled, either wait for completion or stop it").build();
        }
    }

    @Produces (MediaType.TEXT_PLAIN)
    @DELETE
    public Response stopGeneration()
    {
        ApplicationUser user = ApplicationUserUtil.from(authenticationContext.getLoggedInUser());
        if (!globalPermissionManager.hasPermission(SYSTEM_ADMIN, user))
        {
            return response(Status.UNAUTHORIZED, null);
        }

        devSummaryChangedEventService.stopGeneration();
        return Response.status(Status.OK).entity("Stopped Generation").build();
    }

    @Produces (MediaType.APPLICATION_JSON)
    @GET
    public Response generationStatus()
    {
        ApplicationUser user = ApplicationUserUtil.from(authenticationContext.getLoggedInUser());
        if (!globalPermissionManager.hasPermission(SYSTEM_ADMIN, user))
        {
            return response(Status.UNAUTHORIZED, null);
        }

        DevSummaryCachePrimingStatus status = devSummaryChangedEventService.getEventGenerationStatus();
        return Response.status(Status.OK).entity(status).build();
    }

    private Response response(@Nonnull final Status status, @Nullable final Object body)
    {
        final CacheControl cacheControl = new CacheControl();
        cacheControl.setNoCache(true);
        cacheControl.setNoStore(true);
        return Response
                .status(status)
                .entity(body)
                .cacheControl(cacheControl)
                .build();
    }
}
