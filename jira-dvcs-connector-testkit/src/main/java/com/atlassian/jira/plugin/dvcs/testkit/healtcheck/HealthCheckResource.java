package com.atlassian.jira.plugin.dvcs.testkit.healtcheck;

import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * REST endpoint for performing simple healthchecks
 */
@Path ("/healthcheck")
@Produces (MediaType.APPLICATION_JSON)
@AnonymousAllowed
@Scanned
public class HealthCheckResource
{

    @GET
    public HealthCheckBean doGet()
    {
        return HealthCheckBean.OK();
    }

}
