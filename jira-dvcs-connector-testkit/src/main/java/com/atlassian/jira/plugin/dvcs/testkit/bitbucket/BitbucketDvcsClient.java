package com.atlassian.jira.plugin.dvcs.testkit.bitbucket;

import com.atlassian.jira.plugin.dvcs.testkit.healtcheck.HealthCheckBean;
import com.atlassian.jira.testkit.client.JIRAEnvironmentData;
import com.atlassian.jira.testkit.client.RestApiClient;
import com.atlassian.jira.testkit.client.restclient.Response;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;


public class BitbucketDvcsClient extends RestApiClient<BitbucketDvcsClient>
{
    private static final Logger log = LoggerFactory.getLogger(BitbucketDvcsClient.class);
    private final String rootPath;
    private final ObjectMapper mapper;

    public BitbucketDvcsClient(final JIRAEnvironmentData environmentData){
        super(environmentData);
        rootPath = environmentData.getBaseUrl().toExternalForm();

        mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector(new JacksonAnnotationIntrospector());
    }

    public HealthCheckBean healthCheck(){
        try
        {
            Response<String> response = get(healthCheckResource());
            if (response.statusCode != 200)
            {
                log.warn("Got HTTP response {} from healthcheck", response.statusCode);
                return HealthCheckBean.FAIL(String.format("Got HTTP {} from healthcheck", response.statusCode));
            }

            final HealthCheckBean result = mapper.readValue(response.body, HealthCheckBean.class);
            return result;
        }
        catch (Exception e)
        {
            log.error("Exception while retrieving status", e);
            return HealthCheckBean.FAIL(e.getMessage());
        }
    }

    private Response<String> post(final WebResource resource, final String content)
    {
        return toResponse(() -> {
            return resource.accept(MediaType.APPLICATION_JSON_TYPE)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .post(ClientResponse.class, content);
        }, String.class);
    }

    private Response<String> get(final WebResource resource)
    {
        return toResponse(() -> {
            return resource.get(ClientResponse.class);
        }, String.class);
    }

    private WebResource healthCheckResource()
    {
        return basePath().path("healthcheck");
    }

    private WebResource submitResource()
    {
        return basePath().path("submit");
    }

    private WebResource basePath()
    {
        return resourceRoot(rootPath).path("rest").path("dev-status-testkit").path("1.0").path("smartcommits");
    }

}
