package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BaseRemoteRequestor
 * 
 * 
 * <br />
 * <br />
 * Created on 13.7.2012, 10:25:24 <br />
 * <br />
 * 
 * @author jhocman@atlassian.com
 * 
 */
public class BaseRemoteRequestor implements RemoteRequestor
{
    private static final int HTTP_STATUS_CODE_UNAUTHORIZED = 401;
    private static final int HTTP_STATUS_CODE_FORBIDDEN = 403;
    private static final int HTTP_STATUS_CODE_NOT_FOUND = 404;

    private final Logger log = LoggerFactory.getLogger(BaseRemoteRequestor.class);

    protected final String apiUrl;

    public BaseRemoteRequestor(String apiUrl)
    {
        this.apiUrl = apiUrl;
    }

    @Override
    public <T> T get(String uri, Map<String, String> parameters, ResponseCallback<T> callback)
    {
        HttpGet getMethod = new HttpGet();
        return requestWithoutPayload(getMethod, uri, parameters, callback);
    }

    @Override
    public <T> T delete(String uri, Map<String, String> parameters, ResponseCallback<T> callback)
    {
        HttpDelete method = new HttpDelete();
        return requestWithoutPayload(method, uri, parameters, callback);
    }

    @Override
    public  <T> T post(String uri, Map<String, String> parameters, ResponseCallback<T> callback)
    {
        HttpPost method = new HttpPost();
        return requestWithPayload(method, uri, parameters, callback);
    }

    @Override
    public <T> T put(String uri, Map<String, String> parameters, ResponseCallback<T> callback)
    {
        HttpPut method = new HttpPut();
        return requestWithPayload(method, uri, parameters, callback);
    }

    // --------------------------------------------------------------------------------------------------
    // extension hooks
    // --------------------------------------------------------------------------------------------------
    /**
     * E.g. append basic auth headers ...
     */
    protected void onConnectionCreated(DefaultHttpClient client, HttpRequestBase method, Map<String, String> params)
            throws IOException
    {

    }

    /**
     * E.g. append oauth params ...
     */
    protected String afterFinalUriConstructed(HttpRequestBase method, String finalUri, Map<String, String> params)
    {
        return finalUri;
    }

    // --------------------------------------------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------------------------------------------

    protected void logRequest(HttpRequestBase method, String finalUrl, Map<String, String> params)
    {
        log.debug("[Headers {}]", method.getParams());
        log.debug("[REST call {} : {} :: {}]", new Object[] { method.getMethod(), finalUrl, params });
    }

    private <T> T requestWithPayload(HttpEntityEnclosingRequestBase postOrPut, String uri, Map<String, String> params, ResponseCallback<T> callback)
    {

        DefaultHttpClient client = new DefaultHttpClient();
        RemoteResponse response = null;
       
        try
        {
            createConnection(client, postOrPut, uri, params);
            setPayloadParams(postOrPut, params);

            System.out.println("START");
            HttpResponse httpResponse = client.execute(postOrPut);
            System.out.println("END");
            response = checkAndCreateRemoteResponse(client, httpResponse);

            return callback.onResponse(response);

        } catch (BitbucketRequestException e)
        {
            throw e; // Unauthorized or NotFound exceptions will be rethrown
        } catch (IOException e)
        {
            log.debug("Failed to execute request: " + postOrPut.getURI(), e);
            throw new BitbucketRequestException("Failed to execute request " + postOrPut.getURI(), e);
        } catch (URISyntaxException e)
        {
            log.debug("Failed to execute request: " + postOrPut.getURI(), e);
            throw new BitbucketRequestException("Failed to execute request " + postOrPut.getURI(), e);
        } finally
        {
            closeResponse(response);
        }
    }

    private void closeResponse(RemoteResponse response)
    {
        if (response != null)
        {
            response.close();
        }
    }

    private <T> T requestWithoutPayload(HttpRequestBase method, String uri, Map<String, String> parameters, ResponseCallback<T> callback)
    {
        DefaultHttpClient client = new DefaultHttpClient();
        HttpURLConnection connection = null;
        RemoteResponse response = null;
       
        try
        {
            createConnection(client, method, uri + paramsToString(parameters, uri.contains("?")), parameters);
          
            HttpResponse httpResponse = client.execute(method);
            response = checkAndCreateRemoteResponse(client, httpResponse);
            
            return callback.onResponse(response);

        } catch (IOException e)
        {
            log.debug("Failed to execute request: " + connection, e);
            throw new BitbucketRequestException("Failed to execute request " + connection, e);
            
        } catch (URISyntaxException e)
        {
            log.debug("Failed to execute request: " + connection, e);
            throw new BitbucketRequestException("Failed to execute request " + connection, e);
        } finally
        {
            closeResponse(response);
        }
    }

    private RemoteResponse checkAndCreateRemoteResponse(DefaultHttpClient client, HttpResponse httpResponse) throws IOException
    {
        RemoteResponse response = new RemoteResponse();

        int statusCode = httpResponse.getStatusLine().getStatusCode();
        if (statusCode >= 300)
        {
            RuntimeException toBeThrown = new BitbucketRequestException("Error response code during the request : "
                    + statusCode);            
             
            switch (statusCode)
            {
            case HttpStatus.SC_UNAUTHORIZED:
                toBeThrown = new BitbucketRequestException.Unauthorized_401();

            case HttpStatus.SC_FORBIDDEN:
                toBeThrown = new BitbucketRequestException.Forbidden_403();

            case HttpStatus.SC_NOT_FOUND:
                toBeThrown = new BitbucketRequestException.NotFound_404();
            }
            
            // log.error("Failed to properly execute request [" + connection.getRequestMethod() + "] : " + connection, toBeThrown);
            throw toBeThrown;
        }

        response.setHttpStatusCode(statusCode);
        response.setResponse(httpResponse.getEntity().getContent());
        response.setHttpClient(client);

        return response;
    }

    protected String paramsToString(Map<String, String> parameters, boolean urlAlreadyHasParams)
    {
        StringBuilder queryStringBuilder = new StringBuilder();

        if (parameters != null && !parameters.isEmpty())
        {
            if (!urlAlreadyHasParams)
            {
                queryStringBuilder.append("?");
            } else
            {
                queryStringBuilder.append("&");
            }

            paramsMapToString(parameters, queryStringBuilder);
        }
        return queryStringBuilder.toString();
    }

    private void paramsMapToString(Map<String, String> parameters, StringBuilder builder)
    {
        for (Iterator<Map.Entry<String, String>> iterator = parameters.entrySet().iterator(); iterator.hasNext();)
        {
            Map.Entry<String, String> entry = iterator.next();
            builder.append(encode(entry.getKey()));
            builder.append("=");
            builder.append(encode(entry.getValue()));
            if (iterator.hasNext())
            {
                builder.append("&");
            }
        }
    }

    private static String encode(String str)
    {
        if (str == null)
        {
            return null;
        }

        try
        {
            return URLEncoder.encode(str, "UTF-8");
        } catch (UnsupportedEncodingException e)
        {
            throw new BitbucketRequestException("Required encoding not found", e);
        }
    }

    private void createConnection(DefaultHttpClient client, HttpRequestBase method, String uri, Map<String, String> params)
            throws IOException, URISyntaxException
    {
        String finalUrl = afterFinalUriConstructed(method, apiUrl + uri, params);
        method.setURI(new URI(finalUrl)); 

        

        //
        logRequest(method, finalUrl, params);
        //
        //
        // something to extend
        //
        onConnectionCreated(client, method, params);

    }

    private void setPayloadParams(HttpEntityEnclosingRequestBase method, Map<String, String> params) throws IOException
    {
        
        if (params != null)
        {
            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            formparams.add(new BasicNameValuePair("param2", "value2"));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
            
            for (Entry<String, String> entry : params.entrySet())
            {
                formparams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
            method.setEntity(entity);
        }

    }
}
