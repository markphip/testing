package com.atlassian.jira.plugins.dvcs;

import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Map.Entry;

public class RestUrlBuilder
{
    private final String path;
    private final Map<String, String> params = Maps.newHashMap();
    private final String baseUrl;
    private String username = "admin";
    private String password = "admin";

    public RestUrlBuilder(String baseUrl, String path)
    {
        this.path = path;
        this.baseUrl = baseUrl;
    }

    public RestUrlBuilder add(String name, String value)
    {
        params.put(name, value);
        return this;
    }

    public String build()
    {
        StringBuilder url = new StringBuilder();
        url.append(baseUrl);
        url.append(path);
        url.append(url.indexOf("?") != -1 ? "&" : "?");
        url.append("os_username=" + username + "&os_password=" + password);
        for (Entry<String, String> entry : params.entrySet())
        {
            url.append("&");
            url.append(entry.getKey());
            url.append("=");
            url.append(entry.getValue());
        }
        return url.toString();
    }

    @Override
    public String toString()
    {
        return build();
    }

    public RestUrlBuilder username(final String username)
    {
        this.username = username;
        return this;
    }

    public RestUrlBuilder password(final String password)
    {
        this.password = password;
        return this;
    }
}
