package com.atlassian.jira.plugins.dvcs.github.impl;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Resource;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.eclipse.egit.github.core.client.IGitHubConstants;

import com.atlassian.cache.CacheFactory;
import com.atlassian.jira.plugins.dvcs.github.api.model.GitHubPage;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.core.header.LinkHeader;
import com.sun.jersey.core.header.LinkHeaders;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * Support for {@link GitHubRESTClientImpl}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class AbstractGitHubRESTClientImpl
{

    /**
     * Jersey client.
     */
    private final Client client;

    /**
     * @see #setCacheFactory(CacheFactory)
     */
    @Resource
    private CacheFactory cacheFactory;

    /**
     * @see #setRepositoryService(RepositoryService)
     */
    @Resource
    private RepositoryService repositoryService;

    /**
     * Constructor.
     */
    public AbstractGitHubRESTClientImpl()
    {
        ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        client = Client.create(clientConfig);

    }

    /**
     * @param cacheFactory
     *            injected {@link CacheFactory} dependency.
     */
    public void setCacheFactory(CacheFactory cacheFactory)
    {
        this.cacheFactory = cacheFactory;
    }

    /**
     * @param repositoryService
     *            injected {@link RepositoryService} dependency
     */
    public void setRepositoryService(RepositoryService repositoryService)
    {
        this.repositoryService = repositoryService;
    }

    /**
     * Corrects {@link Repository#getOrgHostUrl()} to point to correct repository API URL.
     * 
     * @param repository
     *            for which repository
     * @return resolved REST API URL for provided repository
     */
    private String getRepositoryAPIUrl(Repository repository)
    {
        URL url;

        try
        {
            url = new URL(repository.getOrgHostUrl());
        } catch (MalformedURLException e)
        {
            throw new RuntimeException(e);
        }

        UriBuilder result;
        try
        {
            result = UriBuilder.fromUri(url.toURI());
        } catch (IllegalArgumentException e)
        {
            throw new RuntimeException(e);
        } catch (URISyntaxException e)
        {
            throw new RuntimeException(e);
        }

        String host = url.getHost();

        // corrects default GitHub URL and GIST url to default github host
        if (IGitHubConstants.HOST_DEFAULT.equals(host) || IGitHubConstants.HOST_GISTS.equals(host))
        {
            result.host(IGitHubConstants.HOST_API);
        }

        result = result.path("/repos").path(repository.getOrgName()).path(repository.getSlug());

        // decorates URI with access token
        result.queryParam("access_token", "{arg1}");
        return result.build(repository.getCredential().getAccessToken()).toString();
    }

    /**
     * @return access to configured jersey client
     */
    protected Client getClient()
    {
        return client;
    }

    /**
     * Goes over all GitHub pages and return all pages union.
     * 
     * @param responseEntityType
     *            type of entities
     * @param uri
     *            of resource
     * @return union
     */
    protected <T> List<T> getAll(Class<T[]> responseEntityType, URI uri)
    {
        List<T> result = new LinkedList<T>();

        URI cursor = uri;
        while (cursor != null)
        {
            GitHubPage<T> page = getPage(responseEntityType, uri);
            result.addAll(Arrays.asList(page.getValues()));
            cursor = page.getNextPage();
        }

        return result;
    }

    /**
     * 
     * @param responseEntityType
     *            type of entities
     * @param uri
     *            of web resource
     * @return loaded page
     */
    protected <E> GitHubPage<E> getPage(Class<E[]> responseEntityType, URI uri)
    {
        ClientResponse response = client.resource(uri).accept(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
        E[] pageValues = response.getEntity(responseEntityType);

        LinkHeaders linkHeaders = getLinks(response);
        LinkHeader nextLink = linkHeaders.getLink("next");
        URI nextPage = nextLink != null ? nextLink.getUri() : null;

        return new GitHubPage<E>(pageValues, nextPage);
    }

    /**
     * TODO: workaround for bug - {@link ClientResponse} of jersey does not support comma separated multiple values headers
     * 
     * @param clientResponse
     *            for processing
     * @return proceed links
     */
    private LinkHeaders getLinks(ClientResponse clientResponse)
    {
        // raw 'Link' headers values
        List<String> linksRaw = clientResponse.getHeaders().get("Link");
        if (linksRaw == null)
        {
            linksRaw = new LinkedList<String>();
        }

        // proceed 'Link' values according to multiple values header policy
        List<String> links = new LinkedList<String>();

        for (String linkRaw : linksRaw)
        {
            // header can be comma separated - which means, that it contains multiple values
            for (String link : linkRaw.split(","))
            {
                links.add(link.trim());
            }
        }

        MultivaluedMapImpl headers = new MultivaluedMapImpl();
        headers.put("Link", links);
        return new LinkHeaders(headers);
    }

    /**
     * Builds new {@link URI} - for provided repository and appropriate path part.
     * 
     * @param repository
     *            over which repository
     * @param path
     *            to resource
     * @return created uri
     */
    protected URI uri(Repository repository, String path)
    {
        return UriBuilder.fromUri(getRepositoryAPIUrl(repository)).path(path).build();
    }

}
