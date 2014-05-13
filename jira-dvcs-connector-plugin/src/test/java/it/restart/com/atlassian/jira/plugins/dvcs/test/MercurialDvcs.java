package it.restart.com.atlassian.jira.plugins.dvcs.test;

import com.aragost.javahg.Changeset;
import com.aragost.javahg.Repository;
import com.aragost.javahg.RepositoryConfiguration;
import com.aragost.javahg.commands.AddCommand;
import com.aragost.javahg.commands.BranchCommand;
import com.aragost.javahg.commands.CommitCommand;
import com.aragost.javahg.commands.PushCommand;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.RepositoryRemoteRestpoint;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract, common implementation for all GitHub tests.
 * 
 * @author Miroslav Stencel
 * 
 */
public class MercurialDvcs implements Dvcs
{
    /**
     * Map between test repository URI and local directory of this repository.
     */
    private Map<String, Repository> uriToLocalRepository = new HashMap<String, Repository>();

    /**
     * @param repositoryUri
     *            e.g.: owner/name
     * @return local repository for provided repository uri
     */
    private Repository getLocalRepository(String owner, String repositoryName)
    {
        return uriToLocalRepository.get(getUriKey(owner, repositoryName));
    }

    /**
     * Clones repository
     * 
     * @param cloneUrl
     * 
     * returns local repository
     */
    private Repository clone(String cloneUrl)
    {
        RepositoryConfiguration repositoryConfiguration = new RepositoryConfiguration();
        configureHgBin(repositoryConfiguration);

        return Repository.clone(repositoryConfiguration, Files.createTempDir(), cloneUrl);
    }

    
    /* (non-Javadoc)
     * @see it.restart.com.atlassian.jira.plugins.dvcs.test.Dvcs#createBranch(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void createBranch(String owner, String repositoryName, String branchName)
    {
        BranchCommand.on(getLocalRepository(owner, repositoryName)).set(branchName);
    }

    /* (non-Javadoc)
     * @see it.restart.com.atlassian.jira.plugins.dvcs.test.Dvcs#addFile(java.lang.String, java.lang.String, java.lang.String, byte[])
     */
    @Override
    public void addFile(String owner, String repositoryName, String filePath, byte[] content)
    {
        Repository repository = getLocalRepository(owner, repositoryName);

        File targetFile = new File(repository.getDirectory(), filePath);
        targetFile.getParentFile().mkdirs();
        try
        {
            targetFile.createNewFile();
            FileOutputStream output = new FileOutputStream(targetFile);
            output.write(content);
            output.close();
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        AddCommand.on(repository).execute(filePath);
    }

    /* (non-Javadoc)
     * @see it.restart.com.atlassian.jira.plugins.dvcs.test.Dvcs#commit(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public String commit(String owner, String repositoryName, String message, String authorName, String authorEmail)
    {
        CommitCommand commitCommand = CommitCommand.on(getLocalRepository(owner, repositoryName));
        commitCommand.message(message);
        commitCommand.user(String.format("%s <%s>", authorName, authorEmail));
        Changeset changeset = commitCommand.execute();
        
        return changeset.getNode();
    }

    /* (non-Javadoc)
     * @see it.restart.com.atlassian.jira.plugins.dvcs.test.Dvcs#push(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void push(String owner, String repositoryName, String username, String password)
    {
        push(owner, repositoryName, username, password, null, false);
    }
    
    private String generateCloneUrl(String owner, String repositorySlug, String username, String password)
    {
        return String.format("https://%s:%s@bitbucket.org/%s/%s", username, password, owner, repositorySlug);
    }

    /* (non-Javadoc)
     * @see it.restart.com.atlassian.jira.plugins.dvcs.test.Dvcs#push(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, boolean)
     */
    @Override
    public void push(String owner, String repositoryName, String username, String password, String reference, boolean newBranch)
    {
        PushCommand pushCommand = PushCommand.on(getLocalRepository(owner, repositoryName));
        if (newBranch)
        {
            pushCommand.newBranch();
        }
        
        if (reference != null)
        {
            pushCommand.branch(reference);
        }
        
        try
        {
            pushCommand.execute(generateCloneUrl(owner, repositoryName, username, password));
        } catch (IOException e)
        {
            new RuntimeException(e);
        }
    }

    /* (non-Javadoc)
     * @see it.restart.com.atlassian.jira.plugins.dvcs.test.Dvcs#push(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void push(String owner, String repositoryName, String username, String password, String reference)
    {
        push(owner, repositoryName, username, password, reference, false);
    }
    
    /* (non-Javadoc)
     * @see it.restart.com.atlassian.jira.plugins.dvcs.test.Dvcs#createTestLocalRepository(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void createTestLocalRepository(String owner, String repositoryName, String username, String password)
    {
        Repository localRepository = clone(generateCloneUrl(owner, repositoryName, username, password));
        uriToLocalRepository.put(getUriKey(owner, repositoryName), localRepository);
    }
    
    /* (non-Javadoc)
     * @see it.restart.com.atlassian.jira.plugins.dvcs.test.Dvcs#deleteTestRepository(java.lang.String)
     */
    @Override
    public void deleteTestRepository(String repositoryUri)
    {
        Repository localRepository = uriToLocalRepository.remove(repositoryUri);
        if (localRepository != null)
        {
            localRepository.close();
            try
            {
                FileUtils.deleteDirectory(localRepository.getDirectory());

            } catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }
    
    private void configureHgBin(RepositoryConfiguration repositoryConfiguration)
    {
        Process process;
        try
        {
            process = new ProcessBuilder(repositoryConfiguration.getHgBin(), "--version").start();
            process.waitFor();
        } catch (Exception e)
        {
            repositoryConfiguration.setHgBin("/usr/local/bin/hg");
        }
    }
    
    private String getUriKey(String owner, String slug)
    {
        return owner + "/" + slug;
    }

    @Override
    public String getDvcsType()
    {
        return RepositoryRemoteRestpoint.ScmType.HG;
    }
    
    @Override
    public String getDefaultBranchName()
    {
        return "default";
    }
}
