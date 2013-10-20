package com.atlassian.jira.plugins.dvcs.service.api;

import com.atlassian.annotations.PublicApi;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.model.GlobalFilter;
import com.atlassian.jira.plugins.dvcs.model.PullRequest;
import com.atlassian.jira.plugins.dvcs.model.Repository;

import java.util.List;
import java.util.Map;

/**
 * Gets the pull requests for one or more issue keys or repository from connected dvcs account
 *
 */
@PublicApi
public interface DvcsPullRequestService
{
    /**
     * Find all pullRequests by one or more issue keys
     *
     * @param issueKeys the list of issue keys to find
     * @return list of (@link PullRequest}
     */
    List<PullRequest> getPullRequests(Iterable<String> issueKeys);
}