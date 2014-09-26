package it.restart.com.atlassian.jira.plugins.dvcs.testClient;

import com.atlassian.jira.plugins.dvcs.base.resource.GitHubTestSupport;
import it.restart.com.atlassian.jira.plugins.dvcs.test.IntegrationTestUserDetails;
import org.eclipse.egit.github.core.PullRequest;

public class GitHubClient implements DvcsHostClient<PullRequest>
{
    private final GitHubTestSupport gitHubTestSupport;

    public GitHubClient(final GitHubTestSupport gitHubTestSupport)
    {
        this.gitHubTestSupport = gitHubTestSupport;
    }

    @Override
    public PullRequestDetails<PullRequest> openPullRequest(final String owner, final String repositoryName, final String password,
            final String title, final String description, final String head, final String base, final String... reviewers)
    {
        PullRequest pullRequest = gitHubTestSupport.openPullRequest(owner, repositoryName, title, description, head, base);

        return new PullRequestDetails(pullRequest.getHtmlUrl(), new Long(pullRequest.getNumber()), pullRequest);
    }

    @Override
    public PullRequestDetails<PullRequest> updatePullRequest(final String owner, final String repositoryName,
            final String password, final PullRequest pullRequest, final String title, final String description, final String base)
    {

        PullRequest updatedPullRequest = gitHubTestSupport.updatePullRequest(pullRequest, IntegrationTestUserDetails.ACCOUNT_NAME, repositoryName, title,
                description, base);

        return new PullRequestDetails(updatedPullRequest.getHtmlUrl(), new Long(updatedPullRequest.getNumber()), updatedPullRequest);
    }

    @Override
    public PullRequestDetails<PullRequest> openForkPullRequest(final String owner, final String repositoryName, final String title, final String description, final String head, final String base, final String forkOwner, final String forkPassword)
    {
        PullRequest pullRequest = gitHubTestSupport.openPullRequest(owner, repositoryName, title, title, forkOwner + ":" + head, base);

        return new PullRequestDetails(pullRequest.getHtmlUrl(), new Long(pullRequest.getNumber()), pullRequest);
    }

    @Override
    public void declinePullRequest(final String owner, final String repositoryName, final String password, final PullRequest pullRequest)
    {
        gitHubTestSupport.closePullRequest(owner, repositoryName, pullRequest);
    }

    @Override
    public void approvePullRequest(final String owner, final String repositoryName, final String password, final Long pullRequestId)
    {
        throw new UnsupportedOperationException("Not Supported, use merge");
    }

    @Override
    public void mergePullRequest(final String owner, final String repositoryName, final String password, final Long pullRequestId)
    {
        final int pullRequestNumber = pullRequestId.intValue();
        gitHubTestSupport.mergePullRequest(owner, repositoryName, pullRequestNumber, "Merge Message");
    }

    @Override
    public void commentPullRequest(final String owner, final String repositoryName, final String password, final PullRequest pullRequest, final String comment)
    {
        gitHubTestSupport.commentPullRequest(owner, repositoryName, pullRequest, comment);
    }

    @Override
    public void createRepository(final String accountName, final String repositoryName, final String password, final String scm)
    {
        gitHubTestSupport.addRepositoryByName(accountName, repositoryName, GitHubTestSupport.Lifetime.DURING_TEST_METHOD);
    }

    @Override
    public void removeRepositories()
    {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void fork(final String owner, final String repositoryName, final String forkOwner, final String forkPassword)
    {
        gitHubTestSupport.fork(owner, forkOwner, repositoryName, GitHubTestSupport.Lifetime.DURING_TEST_METHOD);
    }

    @Override
    public boolean isRepositoryExists(final String owner, final String repositoryName, final String password)
    {
        return gitHubTestSupport.repositoryExists(owner, repositoryName);
    }
}
