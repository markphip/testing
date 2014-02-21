package com.atlassian.jira.plugins.dvcs.remoterestpoint;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;

/**
 * @author Miroslav Stencel
 */
public class GithubRepositoriesRemoteRestpoint
{
    private final GitHubClient gitHubClient;
    
    public GithubRepositoriesRemoteRestpoint(GitHubClient gitHubClient)
    {
        this.gitHubClient = gitHubClient;
    }

    public void createGithubRepository(String repositoryName)
    {
        Repository repository = new Repository();
        repository.setName(repositoryName);
        
        try
        {
            getRepositoryService().createRepository(repository);
        } catch (IOException e)
        {
            // repository could not be created
        }
    }

    public void removeExistingRepository(String repositoryName, String owner)
    {
        StringBuilder deleteRepoUri = new StringBuilder("/repos/");
        deleteRepoUri.append(owner).append("/").append(repositoryName);
        
        try
        {
            gitHubClient.delete(deleteRepoUri.toString());
        }
        catch (IOException e)
        {
            // status is not a 204 (No Content).
        }
    }

    public List<Repository> getRepositories(String owner)
    {
        RepositoryService repositoryService = getRepositoryService();
        try
        {
            return repositoryService.getRepositories(owner);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private RepositoryService getRepositoryService()
    {
        return new RepositoryService(gitHubClient);
    }
}
