package it.restart.com.atlassian.jira.plugins.dvcs.testClient;

import com.atlassian.jira.plugins.dvcs.crypto.Encryptor;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketClientBuilderFactory;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.DefaultBitbucketClientBuilderFactory;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.AuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BasicAuthAuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BitbucketRequestException;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.HttpClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.PullRequestRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.RepositoryRemoteRestpoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class BitbucketClient implements DvcsHostClient<BitbucketPullRequest>
{
    private Collection<RepositoryInfo> testRepositories = new ArrayList<RepositoryInfo>();

    @Override
    public PullRequestDetails<BitbucketPullRequest> openPullRequest(String owner, String repositoryName,
            String password, String title, String description, String head, String base, String... reviewers)
    {
        PullRequestRemoteRestpoint pullRequestRemoteRestpoint = getPullRequestRemoteRestpoint(owner, password);
        List<String> reviewersList = reviewers == null ? null : Arrays.asList(reviewers);

        BitbucketPullRequest pullRequest = pullRequestRemoteRestpoint.createPullRequest(owner, repositoryName, title, description, head, base, reviewersList);

        return new PullRequestDetails(pullRequest.getLinks().getHtml().getHref(), pullRequest.getId(), pullRequest);
    }

    @Override
    public PullRequestDetails<BitbucketPullRequest> updatePullRequest(String owner, String repositoryName,
            String password, BitbucketPullRequest pullRequest, String title, String description, String base)
    {
        PullRequestRemoteRestpoint pullRequestRemoteRestpoint = getPullRequestRemoteRestpoint(owner, password);

        BitbucketPullRequest updatedPullRequest = pullRequestRemoteRestpoint.updatePullRequest(owner, repositoryName, pullRequest, title, description, base);

        return new PullRequestDetails(updatedPullRequest.getLinks().getHtml().getHref(), updatedPullRequest.getId(), updatedPullRequest);
    }

    @Override
    public PullRequestDetails<BitbucketPullRequest> openForkPullRequest(String owner, String repositoryName, String title, String description, String head, String base, String forkOwner, String forkPassword)
    {
        PullRequestRemoteRestpoint pullRequestRemoteRestpoint = getPullRequestRemoteRestpoint(forkOwner, forkPassword);

        BitbucketPullRequest pullRequest = pullRequestRemoteRestpoint.createPullRequest(owner, repositoryName, title, description, forkOwner, repositoryName, head, base);

        return new PullRequestDetails(pullRequest.getLinks().getHtml().getHref(), pullRequest.getId(), pullRequest);
    }

    @Override
    public void declinePullRequest(String owner, String repositoryName, String password, BitbucketPullRequest pullRequest)
    {
        PullRequestRemoteRestpoint pullRequestRemoteRestpoint = getPullRequestRemoteRestpoint(owner, password);

        pullRequestRemoteRestpoint.declinePullRequest(owner, repositoryName, pullRequest.getId(), null);
    }

    @Override
    public void approvePullRequest(String owner, String repositoryName, String password, Long pullRequestId)
    {
        PullRequestRemoteRestpoint pullRequestRemoteRestpoint = getPullRequestRemoteRestpoint(owner, password);

        pullRequestRemoteRestpoint.approvePullRequest(owner, repositoryName, pullRequestId);
    }

    @Override
    public void mergePullRequest(String owner, String repositoryName, String password, Long pullRequestId)
    {
        PullRequestRemoteRestpoint pullRequestRemoteRestpoint = getPullRequestRemoteRestpoint(owner, password);

        pullRequestRemoteRestpoint.mergePullRequest(owner, repositoryName, pullRequestId, "Merge message", true);
    }

    @Override
    public void commentPullRequest(String owner, String repositoryName, String password, BitbucketPullRequest pullRequest, String comment)
    {
        PullRequestRemoteRestpoint pullRequestRemoteRestpoint = getPullRequestRemoteRestpoint(owner, password);
        pullRequestRemoteRestpoint.commentPullRequest(owner, repositoryName, pullRequest.getId(), comment);
    }

    @Override
    public void createRepository(final String accountName, final String repositoryName, final String password, String scm)
    {
        BitbucketRemoteClient bbRemoteClient = new BitbucketRemoteClient(accountName, password);
        RepositoryRemoteRestpoint repositoryService = bbRemoteClient.getRepositoriesRest();

        BitbucketRepository remoteRepository = repositoryService.createRepository(repositoryName, scm, false);
        testRepositories.add(new RepositoryInfo(remoteRepository, repositoryService));
    }

    @Override
    public void removeRepositories()
    {
        for (RepositoryInfo repositoryInfo : testRepositories)
        {
            RepositoryRemoteRestpoint repositoryService = repositoryInfo.getRepositoryService();
            BitbucketRepository testRepository = repositoryInfo.getRepository();
            try
            {
                repositoryService.removeRepository(testRepository.getOwner(), testRepository.getSlug());
            }
            catch (Exception e)
            {

            }
        }
        testRepositories.clear();
    }

    @Override
    public void fork(String owner, String repositoryName, String forkOwner, String forkPassword)
    {
        final RepositoryRemoteRestpoint forkRepositoryService = getRepositoryRemoteRestpoint(forkOwner, forkPassword);

        BitbucketRepository remoteRepository = forkRepositoryService.forkRepository(owner, repositoryName, repositoryName, true);

        testRepositories.add(new RepositoryInfo(remoteRepository, forkRepositoryService));
    }

    /**
     * @param owner
     * @param repositoryName
     * @return True when test repository exists.
     */
    @Override
    public boolean isRepositoryExists(String owner, String repositoryName, String password)
    {
        final RepositoryRemoteRestpoint repositoryService = getRepositoryRemoteRestpoint(owner, password);

        try
        {
            return repositoryService.getRepository(owner, repositoryName) != null;

        } catch (BitbucketRequestException.NotFound_404 e)
        {
            return false;
        }
    }

    private RepositoryRemoteRestpoint getRepositoryRemoteRestpoint(String owner, String password)
    {
        HttpClientProvider httpClientProvider = new HttpClientProvider();
        httpClientProvider.setUserAgent("jirabitbucketconnectortest");

        // Bitbucket client setup
        AuthProvider authProvider = new BasicAuthAuthProvider(BitbucketRemoteClient.BITBUCKET_URL,
                owner,
                password,
                httpClientProvider);

        BitbucketRemoteClient bitbucketClient = new BitbucketRemoteClient(authProvider);
        return bitbucketClient.getRepositoriesRest();
    }

    private PullRequestRemoteRestpoint getPullRequestRemoteRestpoint(String owner, String password)
    {
        BitbucketClientBuilderFactory bitbucketClientBuilderFactory = new DefaultBitbucketClientBuilderFactory(new Encryptor()
        {

            @Override
            public String encrypt(final String input, final String organizationName, final String hostUrl)
            {
                return input;
            }

            @Override
            public String decrypt(final String input, final String organizationName, final String hostUrl)
            {
                return input;
            }
        }, "DVCS Connector Tests", new HttpClientProvider());
        Credential credential = new Credential();
        credential.setAdminUsername(owner);
        credential.setAdminPassword(password);
        BitbucketRemoteClient bitbucketClient = bitbucketClientBuilderFactory.authClient("https://bitbucket.org", null, credential).apiVersion(2).build();
        return bitbucketClient.getPullRequestAndCommentsRemoteRestpoint();
    }

    public static class RepositoryInfo
    {
        private BitbucketRepository repository;
        private RepositoryRemoteRestpoint repositoryService;

        public RepositoryInfo(BitbucketRepository repository, RepositoryRemoteRestpoint repositoryService)
        {
            this.repository = repository;
            this.repositoryService = repositoryService;
        }

        public BitbucketRepository getRepository()
        {
            return repository;
        }

        public void setRepository(BitbucketRepository repository)
        {
            this.repository = repository;
        }

        public RepositoryRemoteRestpoint getRepositoryService()
        {
            return repositoryService;
        }

        public void setRepositoryService(RepositoryRemoteRestpoint repositoryService)
        {
            this.repositoryService = repositoryService;
        }
    }
}
