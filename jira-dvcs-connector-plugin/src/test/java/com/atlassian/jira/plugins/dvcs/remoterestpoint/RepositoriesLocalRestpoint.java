package com.atlassian.jira.plugins.dvcs.remoterestpoint;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.pageobjects.UserCredentials;
import com.atlassian.jira.plugins.dvcs.RestUrlBuilder;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.model.RepositoryList;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;

import javax.ws.rs.core.MediaType;

/**
 * {@link Repository} related resit point.
 *
 * @author Stanislav Dvorscak
 *
 */
public class RepositoriesLocalRestpoint
{
    private final JiraTestedProduct jira;

    public RepositoriesLocalRestpoint(JiraTestedProduct jira)
    {
        this.jira = jira;
    }

    /**
     * REST point for "/rest/bitbucket/1.0/repositories"
     *
     * @return {@link RepositoryList}
     */
    public RepositoryList getRepositories()
    {
        final String baseUrl = jira.environmentData().getBaseUrl().toString();
        UserCredentials credentials = jira.getAdminCredentials();
        RestUrlBuilder url = new RestUrlBuilder(baseUrl, "/rest/bitbucket/1.0/repositories")
            .username(credentials.getUsername()).password(credentials.getPassword());

        ClientConfig clientConfig = new DefaultClientConfig(JacksonJsonProvider.class);
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        Client client = Client.create(clientConfig);
        return client.resource(url.toString()).accept(MediaType.APPLICATION_JSON_TYPE).get(RepositoryList.class);
    }

}
