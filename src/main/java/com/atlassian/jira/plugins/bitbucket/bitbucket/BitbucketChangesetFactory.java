package com.atlassian.jira.plugins.bitbucket.bitbucket;

import java.util.ArrayList;
import java.util.List;

import com.atlassian.jira.plugins.bitbucket.bitbucket.impl.DefaultBitbucketChangeset;
import com.atlassian.jira.plugins.bitbucket.bitbucket.impl.LazyLoadedBitbucketChangeset;
import com.atlassian.jira.plugins.bitbucket.common.Changeset;
import com.atlassian.jira.plugins.bitbucket.common.SourceControlRepository;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;

/**
 * Factory for {@link Changeset} implementations
 */
public class BitbucketChangesetFactory
{
    /**
     * Load the changeset details based on the authentication method, the repository owner, repository
     * slug, and changeset node id
     *
     * @param bitbucket the remote bitbucket service
     * @param auth      the authentication method
     * @param owner     the owner of the repository
     * @param slug      the slug of the repository
     * @param node      the changeset node id
     * @param repository 
     * @return the parsed {@link BitbucketRepository}
     */
    public static Changeset load(Bitbucket bitbucket, SourceControlRepository repository, String node)
    {
        return new LazyLoadedBitbucketChangeset(bitbucket, repository, node);
    }

    /**
     * Parse the json object as a bitbucket changeset
     *
     * @param owner the owner of the repository this changeset belongs to
     * @param slug  the slug of the repository this changeset belons to
     * @param json  the json object describing the change
     * @return the parsed {@link Changeset}
     */
    public static Changeset parse(String repositoryUrl, JSONObject json)
    {
        try
        {
			return new DefaultBitbucketChangeset(
                    repositoryUrl, 
                    json.getString("node"),
                    json.getString("raw_author"),
                    json.getString("author"),
                    json.getString("timestamp"),
                    json.getString("raw_node"),
                    json.getString("branch"),
                    json.getString("message"),
                    json.getString("revision"),
                    stringList(json.getJSONArray("parents")),
                    fileList(json.getJSONArray("files"))
            );
        }
        catch (JSONException e)
        {
            throw new BitbucketException("invalid json object", e);
        }
    }

    private static List<String> stringList(JSONArray parents) throws JSONException
    {
        List<String> list = new ArrayList<String>();
        for (int i = 0; i < parents.length(); i++)
            list.add((String) parents.get(i));
        return list;
    }

    private static List<BitbucketChangesetFile> fileList(JSONArray parents) throws JSONException
    {
        List<BitbucketChangesetFile> list = new ArrayList<BitbucketChangesetFile>();
        for (int i = 0; i < parents.length(); i++)
            list.add(BitbucketChangesetFileFactory.parse((JSONObject) parents.get(i)));
        return list;
    }

    private BitbucketChangesetFactory()
    {
    }
}
