package com.atlassian.jira.plugins.dvcs.rest;

import java.io.StringWriter;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.atlassian.velocity.VelocityContextUtils;
import com.google.common.collect.Maps;

@Path("/html")
@Produces({ MediaType.TEXT_HTML })
public class HtmlFragmentsResource
{
    private static final Logger log = LoggerFactory.getLogger(HtmlFragmentsResource.class);

    private final OrganizationService organizationService;
    private final TemplateRenderer templateRenderer;

    public HtmlFragmentsResource(final OrganizationService organizationService, final TemplateRenderer templateRenderer)
    {
        this.organizationService = organizationService;
        this.templateRenderer = templateRenderer;
    }

    @GET
    @Path("/repositories")
    @Produces({ MediaType.TEXT_HTML })
    public Response getCommits(@QueryParam("oid") final int orgId)
    {
        final StringWriter out = new StringWriter();
        final Map<String, Object> model = Maps.newHashMap();
        VelocityContextUtils.getContextParamsBody(model);
        model.put("org", organizationService.get(orgId, true));
        try
        {
            templateRenderer.render("/templates/dvcs/add-organization.vm", model, out);
        }
        catch (final Exception e)
        {
            log.warn(e + " : failed to render repositories fragment - " + e.getMessage());
            return Response.serverError().build();
        }

        return Response.ok(out.toString()).build();
    }
}
