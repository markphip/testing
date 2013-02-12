package com.atlassian.jira.plugins.dvcs.spi.github.dao;

import java.util.List;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestLineComment;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;

/**
 * The {@link GitHubPullRequestLineComment} related DAO services.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public interface GitHubPullRequestLineCommentDAO
{

    /**
     * Saves or updates provided {@link GitHubPullRequestLineComment}.
     * 
     * @param gitHubPullRequestLineComment
     *            to save/update
     */
    void save(GitHubPullRequestLineComment gitHubPullRequestLineComment);

    /**
     * Deletes provided {@link GitHubPullRequestLineComment}.
     * 
     * @param gitHubPullRequestLineComment
     *            to delete
     */
    void delete(GitHubPullRequestLineComment gitHubPullRequestLineComment);

    /**
     * @param id
     *            {@link GitHubPullRequestLineComment#getId()}
     * @return resolved {@link GitHubPullRequestLineComment}
     */
    GitHubPullRequestLineComment getById(int id);

    /**
     * @param gitHubId
     *            {@link GitHubPullRequestLineComment#getGitHubId()}
     * @return {@link GitHubPullRequestLineComment}
     */
    GitHubPullRequestLineComment getByGitHubId(long gitHubId);

    /**
     * @param repository
     *            {@link GitHubPullRequestLineComment#getRepository()}
     * @return resolved {@link GitHubPullRequestLineComment}-s
     */
    List<GitHubPullRequestLineComment> getByRepository(GitHubRepository repository);

}