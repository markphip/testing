package com.atlassian.jira.plugins.bitbucket.webwork;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.mapper.Progress;
import com.atlassian.jira.plugins.bitbucket.mapper.Synchronizer;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryManager;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.RepositoryUri;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.google.common.collect.Iterables;

/**
 * Webwork action used to configure the bitbucket repositories
 */
public class ConfigureBitbucketRepositories extends JiraWebActionSupport
{
    private final Logger logger = LoggerFactory.getLogger(ConfigureBitbucketRepositories.class);

    // JIRA Project Listing
    private ComponentManager cm = ComponentManager.getInstance();
    private List<Project> projects = cm.getProjectManager().getProjectObjects();
    private String mode = "";
    private String bbUserName = "";
    private String bbPassword = "";
    private String url = "";
    private String postCommitURL = "";
    private String repoVisibility = "";
    private String projectKey = "";
    private String nextAction = "";
    private String validations = "";
    private String messages = "";
    private String redirectURL = "";

    private final Synchronizer synchronizer;
    private List<Progress> progress;

	private final RepositoryManager globalRepositoryManager;

    public ConfigureBitbucketRepositories(Synchronizer synchronizer, 
    		@Qualifier("globalRepositoryManager") RepositoryManager globalRepositoryManager)
    {
        this.synchronizer = synchronizer;
		this.globalRepositoryManager = globalRepositoryManager;
    }

    protected void doValidation()
    {

        if (!globalRepositoryManager.canHandleUrl(url))
        {
            addErrorMessage("URL must be for a valid repository.");
            validations = "URL must be for a valid repository.";
        }
    }

    @RequiresXsrfCheck
    protected String doExecute() throws Exception
    {
        logger.debug("configure repository [ " + nextAction + " ]");

        if (validations.equals(""))
        {
            if (nextAction.equals("AddRepository"))
            {
                if (!repoVisibility.equals("private") || (StringUtils.isNotBlank(bbUserName) && StringUtils.isNotBlank(bbPassword)))
                {
                	globalRepositoryManager.addRepository(projectKey, url, bbPassword, bbPassword);
                    postCommitURL = "BitbucketPostCommit.jspa?projectKey=" + projectKey;
                    nextAction = "ForceSync";
                }
            }

            if (nextAction.equals("ShowPostCommitURL"))
            {
                postCommitURL = "BitbucketPostCommit.jspa?projectKey=" + projectKey;
            }

            if (nextAction.equals("DeleteRepository"))
            {
            	globalRepositoryManager.removeRepository(projectKey, url);
            }

            if (nextAction.equals("CurrentSyncStatus"))
            {
                progress = new ArrayList<Progress>();
                Iterables.addAll(progress,synchronizer.getProgress(projectKey, RepositoryUri.parse(url)));
                return "syncstatus";
            }

            if (nextAction.equals("SyncRepository"))
            {
                syncRepository();
                return "syncmessage";
            }
        }

        return INPUT;
    }

    private void syncRepository() throws MalformedURLException
    {
        logger.debug("sync [ {} ] for project [ {} ]", url, projectKey);
//        globalRepositoryManager.getSynchronisationOperation();
        synchronizer.synchronize(projectKey, RepositoryUri.parse(url));
    }

    public List<Project> getProjects()
    {
        return projects;
    }

    // Stored Repository + JIRA Projects
    public List<SourceControlRepository> getProjectRepositories(String projectKey)
    {
        return globalRepositoryManager.getRepositories(projectKey);
    }

    public String getProjectName()
    {
        return cm.getProjectManager().getProjectObjByKey(projectKey).getName();
    }

    public void setMode(String value)
    {
        this.mode = value;
    }

    public String getMode()
    {
        return mode;
    }

    public void setbbUserName(String value)
    {
        this.bbUserName = value;
    }

    public String getbbUserName()
    {
        return this.bbUserName;
    }

    public void setbbPassword(String value)
    {
        this.bbPassword = value;
    }

    public String getbbPassword()
    {
        return this.bbPassword;
    }

    public void setUrl(String value)
    {
        this.url = value;
    }

    public String getURL()
    {
        return url;
    }

    public void setPostCommitURL(String value)
    {
        this.postCommitURL = value;
    }

    public String getPostCommitURL()
    {
        return postCommitURL;
    }

    public void setRepoVisibility(String value)
    {
        this.repoVisibility = value;
    }

    public String getRepoVisibility()
    {
        return repoVisibility;
    }

    public void setProjectKey(String value)
    {
        this.projectKey = value;
    }

    public String getProjectKey()
    {
        return projectKey;
    }

    public void setNextAction(String value)
    {
        this.nextAction = value;
    }

    public String getNextAction()
    {
        return this.nextAction;
    }

    public String getValidations()
    {
        return this.validations;
    }

    public String getMessages()
    {
        return this.messages;
    }

    public String getRedirectURL()
    {
        return this.redirectURL;
    }

    public List<Progress> getProgress()
    {
        return progress;
    }
    
    public static String encodeUrl(String url)
    {
    	try
		{
			return URLEncoder.encode(url, "UTF-8");
		} catch (UnsupportedEncodingException e)
		{
			return null;
		}
    }
}
