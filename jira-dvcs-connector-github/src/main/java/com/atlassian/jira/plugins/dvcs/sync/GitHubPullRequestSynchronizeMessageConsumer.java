package com.atlassian.jira.plugins.dvcs.sync;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryCommitMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.github.api.GitHubRESTClient;
import com.atlassian.jira.plugins.dvcs.github.api.model.GitHubPullRequest;
import com.atlassian.jira.plugins.dvcs.github.api.model.GitHubRepository;
import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.model.Participant;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.message.MessageAddress;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.message.GitHubPullRequestSynchronizeMessage;

/**
 * Message consumer {@link GitHubPullRequestSynchronizeMessage}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubPullRequestSynchronizeMessageConsumer implements MessageConsumer<GitHubPullRequestSynchronizeMessage>
{

    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPullRequestSynchronizeMessageConsumer.class);

    /**
     * @see #getQueue()
     */
    public static final String QUEUE = GitHubPullRequestSynchronizeMessageConsumer.class.getCanonicalName();

    /**
     * @see #getAddress()
     */
    public static final String ADDRESS = GitHubPullRequestSynchronizeMessage.class.getCanonicalName();

    /**
     * Injected {@link com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestDao} dependency.
     */
    @Resource
    private RepositoryPullRequestDao repositoryPullRequestDao;

    /**
     * Injected {@link com.atlassian.jira.plugins.dvcs.service.PullRequestService} dependency.
     */
    @Resource
    private com.atlassian.jira.plugins.dvcs.service.PullRequestService pullRequestService;

    /**
     * Injected {@link GithubClientProvider} dependency.
     */
    @Resource(name = "githubClientProvider")
    private GithubClientProvider gitHubClientProvider;

    /**
     * Injected {@link GitHubRESTClient} dependency.
     */
    @Resource
    private GitHubRESTClient gitHubRESTClient;

    /**
     * Injected {@link MessagingService} dependency.
     */
    @Resource
    private MessagingService messagingService;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getQueue()
    {
        return QUEUE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(Message<GitHubPullRequestSynchronizeMessage> message, GitHubPullRequestSynchronizeMessage payload)
    {
        Repository repository = payload.getRepository();

        GitHubPullRequest remotePullRequest = gitHubRESTClient.getPullRequest(repository, payload.getPullRequestNumber());
        RepositoryPullRequestMapping localPullRequest = repositoryPullRequestDao.findRequestByRemoteId(repository,
                remotePullRequest.getNumber());

        Map<String, Participant> participantIndex = new HashMap<String, Participant>();

        localPullRequest = updateLocalPullRequest(repository, remotePullRequest, localPullRequest, participantIndex);
        repositoryPullRequestDao.updatePullRequestIssueKeys(repository, localPullRequest.getID());
        updateLocalPullRequestCommits(repository, remotePullRequest, localPullRequest);

        processPullRequestComments(repository, remotePullRequest, localPullRequest, participantIndex);
        processPullRequestReviewComments(repository, remotePullRequest, localPullRequest, participantIndex);

        pullRequestService.updatePullRequestParticipants(localPullRequest.getID(), repository.getId(), participantIndex);
    }

    /**
     * Creates or updates local version of remote {@link PullRequest}.
     * 
     * @param repository
     *            pull request owner
     * @param remotePullRequest
     *            remote pull request representation
     * @param localPullRequest
     * @return created/updated local pull request
     */
    private RepositoryPullRequestMapping updateLocalPullRequest(Repository repository, GitHubPullRequest remotePullRequest,
            RepositoryPullRequestMapping localPullRequest, Map<String, Participant> participantIndex)
    {
        if (localPullRequest == null)
        {
            Map<String, Object> activity = new HashMap<String, Object>();
            map(activity, repository, remotePullRequest);
            localPullRequest = repositoryPullRequestDao.savePullRequest(repository, activity);
        } else
        {
            localPullRequest = repositoryPullRequestDao.updatePullRequestInfo(localPullRequest.getID(), remotePullRequest.getTitle(),
                    remotePullRequest.getHead().getRef(), remotePullRequest.getBase().getRef(), resolveStatus(remotePullRequest),
                    remotePullRequest.getUpdatedAt(), getRepositoryFullName(remotePullRequest.getHead().getRepo()),
                    remotePullRequest.getComments());
        }

        addParticipant(participantIndex, remotePullRequest.getUser().getLogin(), Participant.ROLE_PARTICIPANT);
        if (remotePullRequest.getMergedBy() != null)
        {
            addParticipant(participantIndex, remotePullRequest.getMergedBy().getLogin(), Participant.ROLE_REVIEWER);
        }
        if (remotePullRequest.getAssignee() != null)
        {
            addParticipant(participantIndex, remotePullRequest.getAssignee().getLogin(), Participant.ROLE_REVIEWER);
        }

        return localPullRequest;
    }

    private void addParticipant(Map<String, Participant> participantIndex, String userLogin, String role)
    {
        Participant participant = participantIndex.get(userLogin);
        if (participant == null)
        {
            participantIndex.put(userLogin, new Participant(userLogin, false, role));
        }
    }

    private void updateLocalPullRequestCommits(Repository repository, GitHubPullRequest remotePullRequest,
            RepositoryPullRequestMapping localPullRequest)
    {
        List<RepositoryCommit> remoteCommits = getRemotePullRequestCommits(repository, remotePullRequest);

        Set<RepositoryCommitMapping> remainingCommitsToDelete = new HashSet<RepositoryCommitMapping>(
                Arrays.asList(localPullRequest.getCommits()));

        for (RepositoryCommit remoteCommit : remoteCommits)
        {
            RepositoryCommitMapping commit = repositoryPullRequestDao.getCommitByNode(repository, remoteCommit.getSha());
            if (commit == null)
            {
                Map<String, Object> commitData = new HashMap<String, Object>();
                map(commitData, remoteCommit);
                commit = repositoryPullRequestDao.saveCommit(repository, commitData);
                repositoryPullRequestDao.linkCommit(repository, localPullRequest, commit);
            } else
            {
                remainingCommitsToDelete.remove(commit);
            }
        }

        for (RepositoryCommitMapping commit : remainingCommitsToDelete)
        {
            repositoryPullRequestDao.unlinkCommit(repository, localPullRequest, commit);
        }
    }

    /**
     * Loads remote commits for provided pull request.
     * 
     * @param repository
     *            pull request owner
     * @param remotePullRequest
     *            remote pull request
     * @return remote commits of pull request
     */
    private List<RepositoryCommit> getRemotePullRequestCommits(Repository repository, GitHubPullRequest remotePullRequest)
    {
        PullRequestService pullRequestService = gitHubClientProvider.getPullRequestService(repository);
        try
        {
            return pullRequestService.getCommits(RepositoryId.createFromUrl(repository.getRepositoryUrl()), remotePullRequest.getNumber());
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Processes comments of a Pull Request.
     * 
     * @param repository
     * @param remotePullRequest
     * @param localPullRequest
     */
    private void processPullRequestComments(Repository repository, GitHubPullRequest remotePullRequest,
            RepositoryPullRequestMapping localPullRequest, Map<String, Participant> participantIndex)
    {
        updateCommentsCount(remotePullRequest, localPullRequest);

        IssueService issueService = gitHubClientProvider.getIssueService(repository);
        RepositoryId repositoryId = RepositoryId.createFromUrl(repository.getRepositoryUrl());
        List<Comment> pullRequestComments;
        try
        {
            pullRequestComments = issueService.getComments(repositoryId, remotePullRequest.getNumber());
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        for (Comment comment : pullRequestComments)
        {
            addParticipant(participantIndex, comment.getUser().getLogin(), Participant.ROLE_PARTICIPANT);
        }
    }

    /**
     * Processes review comments of a Pull Request.
     * 
     * @param repository
     * @param remotePullRequest
     * @param localPullRequest
     */
    private void processPullRequestReviewComments(Repository repository, GitHubPullRequest remotePullRequest,
            RepositoryPullRequestMapping localPullRequest, Map<String, Participant> participantIndex)
    {
        updateCommentsCount(remotePullRequest, localPullRequest);

        PullRequestService pullRequestService = gitHubClientProvider.getPullRequestService(repository);
        RepositoryId repositoryId = RepositoryId.createFromUrl(repository.getRepositoryUrl());
        List<CommitComment> pullRequestReviewComments;
        try
        {
            pullRequestReviewComments = pullRequestService.getComments(repositoryId, remotePullRequest.getNumber());
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        for (CommitComment comment : pullRequestReviewComments)
        {
            addParticipant(participantIndex, comment.getUser().getLogin(), Participant.ROLE_PARTICIPANT);
        }
    }

    private void updateCommentsCount(GitHubPullRequest remotePullRequest, RepositoryPullRequestMapping localPullRequest)
    {
        int commentsCount = remotePullRequest.getComments() + remotePullRequest.getReviewComments();
        // updates count
        repositoryPullRequestDao.updatePullRequestInfo(localPullRequest.getID(), localPullRequest.getName(),
                localPullRequest.getSourceBranch(), localPullRequest.getDestinationBranch(),
                RepositoryPullRequestMapping.Status.valueOf(localPullRequest.getLastStatus()), localPullRequest.getUpdatedOn(),
                localPullRequest.getSourceRepo(), commentsCount);
    }

    private void map(Map<String, Object> target, Repository repository, GitHubPullRequest source)
    {
        target.put(RepositoryPullRequestMapping.REMOTE_ID, Long.valueOf(source.getNumber()));
        target.put(RepositoryPullRequestMapping.NAME, source.getTitle());

        target.put(RepositoryPullRequestMapping.URL, source.getHtmlUrl());
        target.put(RepositoryPullRequestMapping.TO_REPO_ID, repository.getId());

        target.put(RepositoryPullRequestMapping.AUTHOR, source.getUser().getLogin());
        target.put(RepositoryPullRequestMapping.CREATED_ON, source.getCreatedAt());
        target.put(RepositoryPullRequestMapping.UPDATED_ON, source.getUpdatedAt());
        target.put(RepositoryPullRequestMapping.SOURCE_REPO, getRepositoryFullName(source.getHead().getRepo()));
        target.put(RepositoryPullRequestMapping.SOURCE_BRANCH, source.getHead().getRef());
        target.put(RepositoryPullRequestMapping.DESTINATION_BRANCH, source.getBase().getRef());
        target.put(RepositoryPullRequestMapping.LAST_STATUS, resolveStatus(source).name());
        target.put(RepositoryPullRequestMapping.COMMENT_COUNT, source.getComments());
    }

    private void map(Map<String, Object> target, RepositoryCommit source)
    {
        target.put(RepositoryCommitMapping.RAW_AUTHOR, source.getCommit().getAuthor().getName());
        target.put(RepositoryCommitMapping.MESSAGE, source.getCommit().getMessage());
        target.put(RepositoryCommitMapping.NODE, source.getCommit().getSha());
        target.put(RepositoryCommitMapping.DATE, source.getCommit().getAuthor().getDate());
    }

    private RepositoryPullRequestMapping.Status resolveStatus(GitHubPullRequest pullRequest)
    {
        if ("open".equalsIgnoreCase(pullRequest.getState()))
        {
            return RepositoryPullRequestMapping.Status.OPEN;
        } else if ("closed".equalsIgnoreCase(pullRequest.getState()))
        {
            return pullRequest.getMergedAt() != null ? RepositoryPullRequestMapping.Status.MERGED
                    : RepositoryPullRequestMapping.Status.DECLINED;
        } else
        {
            LOGGER.warn("Unable to parse Status of GitHub Pull Request, unknown GH state: " + pullRequest.getState());
            return RepositoryPullRequestMapping.Status.OPEN;
        }
    }

    private String getRepositoryFullName(GitHubRepository gitHubRepository)
    {
        if (gitHubRepository == null || gitHubRepository.getOwner() == null)
        {
            return null;
        }

        return gitHubRepository.getOwner().getLogin() + "/" + gitHubRepository.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessageAddress<GitHubPullRequestSynchronizeMessage> getAddress()
    {
        return messagingService.get(GitHubPullRequestSynchronizeMessage.class, ADDRESS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getParallelThreads()
    {
        // Only one thread - comments processing is currently not thread safe!!!
        // The same comments can be proceed over the same Pull Request - because of multiple messages over the same PR
        return 1;
    }
}
