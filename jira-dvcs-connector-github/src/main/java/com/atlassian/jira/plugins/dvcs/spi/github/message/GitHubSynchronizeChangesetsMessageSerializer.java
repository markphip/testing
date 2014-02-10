package com.atlassian.jira.plugins.dvcs.spi.github.message;

import com.atlassian.jira.plugins.dvcs.service.message.AbstractMessagePayloadSerializer;
import com.atlassian.jira.util.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.eclipse.egit.github.core.client.PagedRequest;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * An implementation of {@link com.atlassian.jira.plugins.dvcs.service.message.MessagePayloadSerializer} over {@link com.atlassian.jira.plugins.dvcs.spi.github.message.GitHubSynchronizeChangesetsMessage}.
 *
 * @author Miroslav Stencel
 *
 */
public class GitHubSynchronizeChangesetsMessageSerializer extends AbstractMessagePayloadSerializer<GitHubSynchronizeChangesetsMessage>
{
    @Override
    protected void serializeInternal(JSONObject json, GitHubSynchronizeChangesetsMessage payload) throws Exception
    {
        json.put("refreshAfterSynchronizedAt", payload.getRefreshAfterSynchronizedAt().getTime());
        if (StringUtils.isNotEmpty(payload.getFirstSha()))
        {
            json.put("sha", payload.getFirstSha());
        }
        if (payload.getLastCommitDate() != null)
        {
            json.put("lastCommitDate", payload.getLastCommitDate().getTime());
        }
        json.put("nodesToBranches", payload.getNodesToBranches());
        json.put("pagelen", payload.getPagelen());
    }

    @Override
    protected GitHubSynchronizeChangesetsMessage deserializeInternal(JSONObject json, final int version) throws Exception
    {
        Date refreshAfterSynchronizedAt = parseDate(json, "refreshAfterSynchronizedAt", version);
        String firstSha = json.optString("sha", null);
        Map<String, String> nodesToBranches = asMap(json.optJSONObject("nodesToBranches"));
        Date lastCommitDate = parseDate(json, "lastCommitDate", version);
        int pagelen = json.optInt("pagelen", PagedRequest.PAGE_SIZE);
        return new GitHubSynchronizeChangesetsMessage(null, refreshAfterSynchronizedAt, null, firstSha, lastCommitDate, pagelen, nodesToBranches, false, 0);
    }

    protected Map<String, String> asMap(JSONObject object)
    {
        String[] names = JSONObject.getNames(object);
        Map<String, String> ret = new HashMap<String, String>();
        if (names != null)
        {
            for (String keyName : names)
            {
                ret.put(keyName, object.optString(keyName));
            }
        }
        return ret;
    }

    @Override
    public Class<GitHubSynchronizeChangesetsMessage> getPayloadType()
    {
        return GitHubSynchronizeChangesetsMessage.class;
    }
}
