package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;

/**
 * BitbucketHttpError
 *
 * <pre>
 * {
 *   "error": {
 *     "message": "Not found"
 *   },
 *   "data": {
 *     "shas": ["a1d1f2dbcfb8000c584c84a7e424acf5359eb7ae",
 *              "4e28ec75de4186e76863a9e338ce665987c03a87"]
 *     }
 * }
 * </pre>
 */
public class BitbucketHttpError implements Serializable
{
    private BitbucketError error;
    private BitbucketData data;

    public BitbucketError getError()
    {
        return error;
    }

    public void setError(final BitbucketError error)
    {
        this.error = error;
    }

    public BitbucketData getData()
    {
        return data;
    }

    public void setData(final BitbucketData data)
    {
        this.data = data;
    }
}
