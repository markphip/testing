package com.atlassian.jira.plugins.dvcs.base.resource;

import com.atlassian.fugue.Either;
import com.atlassian.jira.plugins.dvcs.base.AbstractTestListener;
import com.atlassian.jira.plugins.dvcs.base.TestListenerDelegate;
import com.atlassian.jira.plugins.dvcs.base.resource.github.EGitPullRequestServiceWrapper;
import com.atlassian.jira.plugins.dvcs.model.PullRequestStatus;
import it.restart.com.atlassian.jira.plugins.dvcs.common.MagicVisitor;
import it.restart.com.atlassian.jira.plugins.dvcs.common.OAuth;
import it.restart.com.atlassian.jira.plugins.dvcs.github.GithubLoginPage;
import it.restart.com.atlassian.jira.plugins.dvcs.github.GithubOAuthPage;
import org.apache.commons.httpclient.HttpStatus;
import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.PullRequestMarker;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

/**
 * Provides GitHub test resource related functionality.
 *
 * @author Stanislav Dvorscak
 */
public class GitHubTestSupport
{

    /**
     * Logger for this class.
     */
    private static Logger logger = LoggerFactory.getLogger(GitHubTestSupport.class);

    /**
     * Base GitHub url.
     */
    public static final String URL = "https://github.com";

    /**
     * Organization for GitHub related tests.
     */
    public static final String ORGANIZATION = "jira-dvcs-connector-org";

    /**
     * Lifetime of generated repository.
     *
     * @author Stanislav Dvorscak
     */
    public enum Lifetime
    {
        DURING_CLASS, DURING_TEST_METHOD,
    }

    /**
     * Context infomration related to generated {@link OAuth}.
     *
     * @author Stanislav Dvorscak
     */
    private static class OAuthContext
    {

        private final String gitHubURL;
        private final OAuth oAuth;

        public OAuthContext(String gitHubURL, OAuth oAuth)
        {
            this.gitHubURL = gitHubURL;
            this.oAuth = oAuth;
        }

    }

    /**
     * Context information related to generated repositories.
     *
     * @author Stanislav Dvorscak
     */
    public static class RepositoryContext
    {

        /**
         * Created GitHub repository.
         */
        private final Repository repository;

        /**
         * Owner under whom was created this repository.
         */
        private final String owner;

        /**
         * Constructor.
         *
         * @param owner {@link #owner}
         * @param repository {@link #repository}
         */
        public RepositoryContext(String owner, Repository repository)
        {
            this.owner = owner;
            this.repository = repository;
        }

        public Repository getRepository()
        {
            return repository;
        }

        public String getOwner()
        {
            return owner;
        }
    }

    /**
     * {@link MagicVisitor} dependency injected via constructor.
     */
    private final MagicVisitor magicVisitor;

    /**
     * Used by repository name generation.
     */
    private TimestampNameTestResource timestampNameTestResource = new TimestampNameTestResource();

    /**
     * Registered owners.
     *
     * @see #addOwner(String, GitHubClient)
     */
    private Map<String, GitHubClient> gitHubClientByOwner = new HashMap<String, GitHubClient>();

    /**
     * Created OAuths.
     */
    private Map<Lifetime, List<OAuthContext>> oAuthByLifetime = new HashMap<GitHubTestSupport.Lifetime, List<OAuthContext>>();

    /**
     * Created repositories.
     *
     * @see #addRepository(String, String, Lifetime, int)
     */
    private Map<Lifetime, List<RepositoryContext>> repositoryByLifetime = new HashMap<Lifetime, List<RepositoryContext>>();

    /**
     * Created repositories by slug.
     *
     * @see #addRepository(String, String, Lifetime, int)
     */
    private Map<String, RepositoryContext> repositoryBySlug = new HashMap<String, RepositoryContext>();

    public GitHubTestSupport(final MagicVisitor magicVisitor)
    {
        this.magicVisitor = magicVisitor;
    }

    /**
     * Constructor.
     */
    public GitHubTestSupport(TestListenerDelegate testListenerDelegate, MagicVisitor magicVisitor)
    {
        testListenerDelegate.register(new AbstractTestListener()
        {

            @Override
            public void beforeClass()
            {
                super.beforeClass();
                GitHubTestSupport.this.beforeClass();
            }

            @Override
            public void beforeMethod()
            {
                super.beforeMethod();
                GitHubTestSupport.this.beforeMethod();
            }

            @Override
            public void afterMethod()
            {
                super.afterMethod();
                GitHubTestSupport.this.afterMethod();
            }

            @Override
            public void afterClass()
            {
                super.afterClass();
                GitHubTestSupport.this.afterClass();
            }

        });
        this.magicVisitor = magicVisitor;
    }

    // Listeners for test lifecycle.

    public void beforeClass()
    {
        repositoryByLifetime.put(Lifetime.DURING_CLASS, new LinkedList<RepositoryContext>());
        oAuthByLifetime.put(Lifetime.DURING_CLASS, new LinkedList<OAuthContext>());
    }

    /**
     * Prepares stuff related to single test method.
     */
    public void beforeMethod()
    {
        repositoryByLifetime.put(Lifetime.DURING_TEST_METHOD, new LinkedList<RepositoryContext>());
        oAuthByLifetime.put(Lifetime.DURING_TEST_METHOD, new LinkedList<OAuthContext>());
    }

    /**
     * Cleans up stuff related to single test method.
     */
    public void afterMethod()
    {
        for (OAuthContext oAuthContext : oAuthByLifetime.get(Lifetime.DURING_TEST_METHOD))
        {
            removeOAuth(oAuthContext.gitHubURL, oAuthContext.oAuth);
        }
        for (RepositoryContext testMethodRepository : repositoryByLifetime.remove(Lifetime.DURING_TEST_METHOD))
        {
            removeRepository(testMethodRepository.owner, testMethodRepository.repository.getName());
        }
    }

    /**
     * Cleaning stuff related to this resource.
     */
    public void afterClass()
    {
        for (OAuthContext oAuthContext : oAuthByLifetime.get(Lifetime.DURING_CLASS))
        {
            removeOAuth(oAuthContext.gitHubURL, oAuthContext.oAuth);
        }
        for (RepositoryContext testMethodRepository : repositoryByLifetime.remove(Lifetime.DURING_CLASS))
        {
            removeRepository(testMethodRepository.owner, testMethodRepository.repository.getName());
        }
        for (String owner : gitHubClientByOwner.keySet())
        {
            removeExpiredRepository(owner);
        }
    }

    /**
     * Adds {@link OAuth} for provided GitHub information.
     *
     * @param gitHubURL URL for GitHub
     * @return OAuth
     */
    public OAuth addOAuth(String gitHubURL, String callbackURL, Lifetime lifetime)
    {
        magicVisitor.visit(GithubLoginPage.class, gitHubURL).doLogin();
        GithubOAuthPage gitHubOAuthPage = magicVisitor.visit(GithubOAuthPage.class, gitHubURL);
        OAuth result = gitHubOAuthPage.addConsumer(callbackURL);
        oAuthByLifetime.get(lifetime).add(new OAuthContext(gitHubURL, result));
        magicVisitor.visit(GithubLoginPage.class, gitHubURL).doLogout();
        return result;
    }

    /**
     * Removes provided {@link OAuth}.
     *
     * @param gitHubURL url of GitHub
     * @param oAuth to remove
     * @see #addOAuth(String, String, Lifetime)
     */
    private void removeOAuth(String gitHubURL, OAuth oAuth)
    {
        magicVisitor.visit(GithubLoginPage.class, gitHubURL).doLogin();
        GithubOAuthPage gitHubOAuthPage = magicVisitor.visit(oAuth.applicationId, GithubOAuthPage.class);
        gitHubOAuthPage.removeConsumer();
        magicVisitor.visit(GithubLoginPage.class, gitHubURL).doLogout();
    }

    /**
     * Registers owner, which will be used by GitHub communication.
     *
     * @param owner name of owner
     * @param gitHubClient appropriate GitHub client
     */
    public void addOwner(String owner, GitHubClient gitHubClient)
    {
        gitHubClientByOwner.put(owner, gitHubClient);
    }

    /**
     * Adds repository under provided owner, with random generated name based on provided name prefix.
     *
     * @param owner of created repository
     * @param namePrefix of repository
     * @param lifetime validity of repository (when it should be clean up)
     * @return name of created repository
     */
    public String addRepository(String owner, String namePrefix, Lifetime lifetime, int expirationDuration)
    {
        String repositoryName = timestampNameTestResource.randomName(namePrefix, expirationDuration);
        return addRepositoryByName(owner, repositoryName, lifetime);
    }

    public String addRepositoryByName(String owner, String repositoryName, Lifetime lifetime)
    {
        Repository repository = createRepository(owner, repositoryName);

        RepositoryContext repositoryContext = new RepositoryContext(owner, repository);
        repositoryByLifetime.get(lifetime).add(repositoryContext);
        repositoryBySlug.put(getSlug(owner, repositoryName), repositoryContext);

        return repository.getName();
    }

    /**
     * Forks provided repository into the {@link #ORGANIZATION}. The forked repository will be automatically destroyed
     * after test finished.
     *
     * @param lifetime validity of repository (when it should be clean up)
     */
    public void fork(String owner, String repositoryOwner, String repositoryName, Lifetime lifetime)
    {
        GitHubClient gitHubClient = getGitHubClient(owner);
        RepositoryService repositoryService = new RepositoryService(gitHubClient);

        try
        {
            Repository repository = repositoryService.forkRepository(RepositoryId.create(repositoryOwner, repositoryName),
                    gitHubClient.getUser().equals(owner) ? null : owner);

            // wait until forked repository is prepared
            do
            {
                sleep(1000);
            }
            while (repositoryService.getRepository(repository.getOwner().getLogin(), repository.getName()) == null);

            RepositoryContext repositoryContext = new RepositoryContext(owner, repository);
            repositoryByLifetime.get(lifetime).add(repositoryContext);
            repositoryBySlug.put(getSlug(owner, repositoryName), repositoryContext);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return true if repository already exists on the host, false otherwise
     *
     * @param owner
     * @param repositoryName
     * @return true if repository already exists on the host, false otherwise
     */
    public boolean repositoryExists(String owner, String repositoryName)
    {
        GitHubClient gitHubClient = getGitHubClient(owner);
        RepositoryService repositoryService = new RepositoryService(gitHubClient);

        try
        {
            return repositoryService.getRepository(RepositoryId.create(owner, repositoryName)) != null;
        }
        catch (IOException e)
        {
            return false;
        }
    }

    /**
     * Returns repository for provided name.
     *
     * @param owner of repository
     * @param name of repository
     * @return resolved repository
     */
    public Repository getRepository(String owner, String name)
    {
        RepositoryContext bySlug = repositoryBySlug.get(getSlug(owner, name));
        return bySlug.repository;
    }

    /**
     * Open pull request over provided repository, head and base information.
     *
     * @param owner of repository
     * @param repositoryName on which repository
     * @param title title of Pull request
     * @param description description of Pull request
     * @param head from which head e.g.: master or organization:master
     * @param base to which base
     * @return created EGit pull request
     */
    public PullRequest openPullRequest(String owner, String repositoryName, String title, String description, String head, String base)
    {
        final EGitPullRequestServiceWrapper pullRequestService = buildPullRequestServiceWrapper(owner, repositoryName);

        PullRequest request = new PullRequest();
        request.setTitle(title);
        request.setBody(description);

        request.setHead(new PullRequestMarker().setLabel(head));
        request.setBase(new PullRequestMarker().setLabel(base));

        PullRequest result = null;
        try
        {
            result = pullRequestService.create(request);
        }
        catch (RuntimeException e)
        {
            // let's try once more after while
            sleep(5000);
            result = pullRequestService.create(request);
        }

        // pull request creation is asynchronous process - it is necessary to wait a little bit
        // otherwise unexpected behavior can happened - like next push will be part as open pull request
        final int pullrequestId = result.getNumber();
        waitUntil(new PullRequestCallBackPredicate(pullRequestService, pullrequestId, new PullRequestCallBackFunction()
        {
            @Override
            public boolean testPullRequest(final PullRequest pullRequest)
            {
                return true;
            }
        }));
        return result;
    }

    private PullRequestService getPullRequestService(final RepositoryContext bySlug) {return new PullRequestService(getGitHubClient(bySlug.owner));}

    private void waitUntil(PullRequestCallBackPredicate predicate)
    {
        for (int i = 0; i <=5; i++)
        {
            if (predicate.test())
            {
                break;
            }
            sleep(1000);
        }
    }

    private interface PullRequestCallBackFunction
    {
        boolean testPullRequest(@Nonnull PullRequest pullRequest);
    }

    private static final class PullRequestCallBackPredicate
    {
        private final EGitPullRequestServiceWrapper pullRequestService;
        private final int pullRequestId;
        private final PullRequestCallBackFunction callback;

        private PullRequestCallBackPredicate(final EGitPullRequestServiceWrapper pullRequestService,
                final int pullRequestId, final PullRequestCallBackFunction callback)
        {
            this.pullRequestService = pullRequestService;
            this.pullRequestId = pullRequestId;
            this.callback = callback;
        }

        public boolean test()
        {
            Either<IOException, PullRequest> pullRequestEither = pullRequestService.getAsEither(pullRequestId);
            if (pullRequestEither.isRight())
            {
                return callback.testPullRequest(pullRequestEither.right().get());
            }
            else
            {
                return false;
            }
        }
    }

    /**
     * Update pull request over provided repository, head and base information.
     *
     * @param owner of repository
     * @param repositoryName on which repository
     * @param title title of Pull request
     * @param description description of Pull request
     * @param base to which base
     * @return created EGit pull request
     */
    public PullRequest updatePullRequest(PullRequest pullRequest, String owner, String repositoryName, final String title, final String description, String base)
    {
        final EGitPullRequestServiceWrapper pullRequestService = buildPullRequestServiceWrapper(owner, repositoryName);

        pullRequest.setTitle(title);
        pullRequest.setBody(description);

        PullRequest result = pullRequestService.edit(pullRequest);

        final int pullRequestId = result.getNumber();

        waitUntil(new PullRequestCallBackPredicate(pullRequestService, pullRequestId, new PullRequestCallBackFunction()
        {
            @Override
            public boolean testPullRequest(final PullRequest pullRequest)
            {
                return title.equals(pullRequest.getTitle()) && description.equals(pullRequest.getBody());
            }
        }));

        return result;
    }

    /**
     * Returns slug for provided representation.
     */
    public String getSlug(String owner, String repositoryName)
    {
        return owner + "/" + repositoryName;
    }

    public void mergePullRequest(String owner, String repositoryName, final int pullRequestNumber, String commitMessage)
    {
        final EGitPullRequestServiceWrapper pullRequestService = buildPullRequestServiceWrapper(owner, repositoryName);
        pullRequestService.merge(pullRequestNumber, commitMessage);

        waitUntil(new PullRequestCallBackPredicate(pullRequestService, pullRequestNumber, new PullRequestCallBackFunction()
        {
            @Override
            public boolean testPullRequest(final PullRequest pullRequest)
            {
                return PullRequestStatus.fromGithubStatus(pullRequest.getState(), pullRequest.getMergedAt()) == PullRequestStatus.MERGED;
            }
        }));
    }

    /**
     * Closes provided pull request.
     *
     * @param owner of repository
     * @param repositoryName pull request owner
     * @param pullRequest to close
     */
    public void closePullRequest(String owner, String repositoryName, final PullRequest pullRequest)
    {
        final EGitPullRequestServiceWrapper pullRequestService = buildPullRequestServiceWrapper(owner, repositoryName);
        pullRequest.setState("CLOSED");
        pullRequestService.edit(pullRequest);

        waitUntil(new PullRequestCallBackPredicate(pullRequestService, pullRequest.getNumber(), new PullRequestCallBackFunction()
        {
            @Override
            public boolean testPullRequest(final PullRequest pullRequest)
            {
                return "closed".equals(pullRequest.getState());
            }
        }));
    }

    /**
     * Gets pull request commits
     *
     * @param owner of repository
     * @param repositoryName repository name
     * @param pullRequestId pull request id
     * @return pull request commits
     */
    public List<RepositoryCommit> getPullRequestCommits(String owner, String repositoryName, int pullRequestId)
    {
        final EGitPullRequestServiceWrapper pullRequestService = buildPullRequestServiceWrapper(owner, repositoryName);
        return pullRequestService.getCommits(pullRequestId);
    }

    /**
     * Adds comment to provided pull request.
     *
     * @param owner of repository
     * @param pullRequest pull request owner
     * @param comment message
     * @return created remote comment
     */
    public Comment commentPullRequest(String owner, String repositoryName, PullRequest pullRequest, String comment)
    {
        RepositoryContext bySlug = repositoryBySlug.get(getSlug(owner, repositoryName));

        IssueService issueService = new IssueService(getGitHubClient(owner));
        try
        {
            return issueService.createComment(bySlug.repository,
                    pullRequest.getIssueUrl().substring(pullRequest.getIssueUrl().lastIndexOf('/') + 1), comment);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns comment with a given id
     *
     * @param owner of repository
     * @param repositoryName repository name
     * @param commentId comment id
     * @return remote comment
     */
    public Comment getPullRequestComment(String owner, String repositoryName, long commentId)
    {
        RepositoryContext bySlug = repositoryBySlug.get(getSlug(owner, repositoryName));

        IssueService issueService = new IssueService(getGitHubClient(owner));
        try
        {
            return issueService.getComment(owner, bySlug.repository.getName(), commentId);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
    /**
     * Resolves GitHub client.
     *
     * @param owner of repository
     * @return gitHubClient if exists or {@link RuntimeException}
     */
    private GitHubClient getGitHubClient(String owner)
    {
        GitHubClient gitHubClient = gitHubClientByOwner.get(owner);
        if (gitHubClient == null)
        {
            throw new RuntimeException("Owner must be added, before it can be used, @see #addBeforeOwner(String, GitHubClient)");
        }
        return gitHubClient;
    }

    /**
     * Creates repository for provided name and appropriate owner.
     *
     * @param owner of repository
     * @param name of repository
     * @return created repository
     */
    private Repository createRepository(String owner, String name)
    {
        GitHubClient gitHubClient = getGitHubClient(owner);
        RepositoryService repositoryService = new RepositoryService(gitHubClient);

        Repository repository = new Repository();
        repository.setName(name);

        try
        {
            if (gitHubClient.getUser().equals(owner))
            {
                return repositoryService.createRepository(repository);
            }
            else
            {
                return repositoryService.createRepository(owner, repository);
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Removes repository for provided name by provided owner.
     *
     * @param owner of repository
     * @param name of repository
     */
    private void removeRepository(String owner, String name)
    {
        try
        {
            // removes repository itself
            getGitHubClient(owner).delete("/repos/" + owner + "/" + name);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Removes all expired repository for provided owner.
     *
     * @param owner of repositories
     * @see #addRepository(String, String, Lifetime, int)
     */
    private void removeExpiredRepository(String owner)
    {
        GitHubClient gitHubClient = getGitHubClient(owner);
        RepositoryService repositoryService = new RepositoryService(gitHubClient);

        List<Repository> repositories;
        try
        {
            repositories = repositoryService.getRepositories(owner);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        for (Repository repository : repositories)
        {
            if (timestampNameTestResource.isExpired(repository.getName()))
            {
                try
                {
                    gitHubClient.delete("/repos/" + owner + "/" + repository.getName());
                }
                catch (RequestException e)
                {
                    if (e.getStatus() == HttpStatus.SC_NOT_FOUND)
                    {
                        // Old GitHub Enterprise caches list of repositories and if this repository was already removed, it can be still
                        // presented in this list
                        logger.warn("Can not remove repository: " + owner + "/" + repository.getName() + ", because it was not found!", e);
                    }
                    else
                    {
                        throw new RuntimeException(e);
                    }
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void sleep(long millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch (InterruptedException e)
        {
            // nothing to do
        }
    }

    private EGitPullRequestServiceWrapper buildPullRequestServiceWrapper(String owner, String repositoryName)
    {

        final RepositoryContext bySlug = repositoryBySlug.get(getSlug(owner, repositoryName));
        final PullRequestService pullRequestService = getPullRequestService(bySlug);
        return new EGitPullRequestServiceWrapper(bySlug.repository, pullRequestService);
    }
}
