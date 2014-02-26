package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;
import java.util.List;

/**
 * BitbucketData
 */
public class BitbucketData implements Serializable
{
    private List<String> shas;

    public List<String> getShas()
    {
        return shas;
    }

    public void setShas(final List<String> shas)
    {
        this.shas = shas;
    }
}
