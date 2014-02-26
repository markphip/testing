package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;

/**
 * BitbucketError
 */
public class BitbucketError implements Serializable
{
    private String message;

    public String getMessage()
    {
        return message;
    }

    public void setMessage(final String message)
    {
        this.message = message;
    }
}
