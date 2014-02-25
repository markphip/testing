package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestActivityInfo;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestApprovalActivity;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestApprovalsIterator;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestCommit;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestCommitIterator;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestPage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestReviewer;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestReviewerIterator;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteResponse;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.ResponseCallback;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * PullRequestRemoteRestpoint
 *
 *
 * <br /><br />
 * Created on 11.12.2012, 13:14:31
 * <br /><br />
 * @author jhocman@atlassian.com
 *
 */
public class PullRequestRemoteRestpoint
{

    public static final int REPO_ACTIVITY_PAGESIZE = 30;

    private final RemoteRequestor requestor;

    public PullRequestRemoteRestpoint(RemoteRequestor requestor)
    {
        this.requestor = requestor;
    }

    public BitbucketPullRequestPage<BitbucketPullRequestActivityInfo> getRepositoryActivityPage(int page, String owner, String repoSlug, final Date upToDate) {

        String activityUrl = String.format("/repositories/%s/%s/pullrequests/activity?pagelen=%s&page=%s", owner, repoSlug, REPO_ACTIVITY_PAGESIZE, page);
        ResponseCallback<BitbucketPullRequestPage<BitbucketPullRequestActivityInfo>> callback = new ResponseCallback<BitbucketPullRequestPage<BitbucketPullRequestActivityInfo>>()
        {
            @Override
            public BitbucketPullRequestPage<BitbucketPullRequestActivityInfo> onResponse(RemoteResponse response)
            {
                BitbucketPullRequestPage<BitbucketPullRequestActivityInfo> remote =
                        ClientUtils.fromJson(response.getResponse(),new TypeToken<BitbucketPullRequestPage<BitbucketPullRequestActivityInfo>>(){}.getType() );

                if (remote != null && remote.getValues() != null && !remote.getValues().isEmpty())
                {
                    // filter by date here
                    remote.setValues(filterByDate(upToDate, remote.getValues()));
                }

                return remote;
            }

            private List<BitbucketPullRequestActivityInfo> filterByDate(Date upToDate, List<BitbucketPullRequestActivityInfo> values)
            {
                List<BitbucketPullRequestActivityInfo> fine = new ArrayList<BitbucketPullRequestActivityInfo>();
                for (BitbucketPullRequestActivityInfo info : values)
                {
                    Date activityDate = ClientUtils.extractActivityDate(info.getActivity());
                    if (upToDate == null || activityDate != null && activityDate.after(upToDate))
                    {
                        fine.add(info);
                    }
                }
                return fine;
            }
        };
        return requestor.get(activityUrl, null, callback);

    }

    public Iterable<BitbucketPullRequestCommit> getPullRequestCommits(String owner, String repoSlug, String localId)
    {
        String url = String.format("/repositories/%s/%s/pullrequests/%s/commits", owner, repoSlug, localId);

        return new BitbucketPullRequestCommitIterator(requestor, url);
    }

    public Iterable<BitbucketPullRequestCommit> getPullRequestCommits(String urlIncludingApi)
    {
        return new BitbucketPullRequestCommitIterator(requestor, urlIncludingApi);
    }

    public BitbucketPullRequest getPullRequestDetail(String owner, String repoSlug, String localId)
    {

        String url = String.format("/repositories/%s/%s/pullrequests/%s", owner, repoSlug, localId);

        return getPullRequestDetail(url);
    }

    public BitbucketPullRequest getPullRequestDetail(String urlIncludingApi)
    {
        return requestor.get(urlIncludingApi, null, new ResponseCallback<BitbucketPullRequest>()
        {

            @Override
            public BitbucketPullRequest onResponse(RemoteResponse response)
            {
                return ClientUtils.fromJson(response.getResponse(), new TypeToken<BitbucketPullRequest>()
                {
                }.getType());
            }

        });
    }

    public Iterable<BitbucketPullRequestReviewer> getPullRequestReviewers(String urlIncludingApi)
    {
        return new BitbucketPullRequestReviewerIterator(requestor, urlIncludingApi);
    }

    public Iterable<BitbucketPullRequestApprovalActivity> getPullRequestApprovals(String urlIncludingApi)
    {
        return new BitbucketPullRequestApprovalsIterator(requestor, urlIncludingApi);
    }

    public int getCount(String urlIncludingApi)
    {
        String url = urlIncludingApi + "?pagelen=0";

        return requestor.get(url, null, new ResponseCallback<Integer>()
        {
            @Override
            public Integer onResponse(RemoteResponse response)
            {

                BitbucketPullRequestPage<?> remote = transformFromJson(response);
                if (remote.getSize() == null)
                {
                    return 0;
                }
                return remote.getSize();
            }
        });
    }

    private BitbucketPullRequestPage<?> transformFromJson(final RemoteResponse response)
    {
        return ClientUtils.fromJson(response.getResponse(),
                new TypeToken<BitbucketPullRequestPage<?>>()
                {
                }.getType());
    }
}

