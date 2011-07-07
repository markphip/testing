package com.atlassian.jira.plugins.bitbucket.webwork;

import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.config.properties.PropertiesManager;
import com.atlassian.jira.plugins.bitbucket.bitbucket.Bitbucket;
import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketRepositoryFactory;
import com.atlassian.jira.plugins.bitbucket.bitbucket.RepositoryUri;
import com.atlassian.jira.plugins.bitbucket.mapper.BitbucketMapper;
import com.atlassian.jira.plugins.bitbucket.mapper.Progress;
import com.atlassian.jira.plugins.bitbucket.mapper.Synchronizer;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.google.common.collect.Iterables;
import com.sun.org.apache.bcel.internal.util.Repository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private String baseURL = PropertiesManager.getInstance().getPropertySet().getString("jira.baseurl");
    private String redirectURL = "";

    private final BitbucketMapper bitbucketMapper;
    private final Bitbucket bitbucket;
    private final Synchronizer synchronizer;
    private List<Progress> progress;

    public ConfigureBitbucketRepositories(BitbucketMapper bitbucketMapper,
                                          Bitbucket bitbucket, Synchronizer synchronizer)
    {
        this.bitbucketMapper = bitbucketMapper;
        this.bitbucket = bitbucket;
        this.synchronizer = synchronizer;
    }

    protected void doValidation()
    {
        for (Enumeration e = request.getParameterNames(); e.hasMoreElements(); )
        {
            String n = (String) e.nextElement();
            String[] vals = request.getParameterValues(n);
            //validations = validations + "name " + n + ": " + vals[0];
        }

        // BitBucket URL Validation
        if (!url.equals(""))
        {
            if (nextAction.equals("AddRepository") || nextAction.equals("DeleteReposiory"))
            {
                // Valid URL and URL starts with bitbucket.org domain
                Pattern p = Pattern.compile("^(https|http)://bitbucket.org/[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
                Matcher m = p.matcher(url);
                if (!m.matches())
                {
                    addErrorMessage("URL must be for a valid Bitbucket.org repository.");
                    validations = "URL must be for a valid Bitbucket.org repository.";
                }
            }
        }
        else
        {
            if (nextAction.equals("AddRepository") || nextAction.equals("DeleteReposiory"))
            {
                validations = "URL must be for a valid Bitbucket.org repository.";
            }
        }

    }

    public String doDefault()
    {
        bitbucketMapper.getRepositories("JST");
        return "input";
    }

    @RequiresXsrfCheck
    protected String doExecute() throws Exception
    {
        logger.debug("configure repository [ " + nextAction + " ]");

        // Remove trailing slashes from URL
        if (url.endsWith("/"))
        {
            url = url.substring(0, url.length() - 1);
        }

        // Set all URLs to HTTPS
        if (url.startsWith("http:"))
        {
            url = url.replaceFirst("http:", "https:");
        }

        // Swap overview with 'default' (likely to be pasted in)
        if (url.endsWith("/overview"))
        {
            url = url.substring(0, url.length() - 9);
            url = url + "/default";
        }

        // Add default branch of 'default' to URL if missing
        String[] urlArray = url.split("/");

        if (urlArray.length == 5)
        {
            url += "/default";
            urlArray = url.split("/");
        }

        if (validations.equals(""))
        {
            if (nextAction.equals("AddRepository"))
            {

                if (repoVisibility.equals("private"))
                {
                    if (StringUtils.isNotBlank(bbUserName) && StringUtils.isNotBlank(bbPassword))
                    {
                        postCommitURL = "BitbucketPostCommit.jspa?projectKey=" + projectKey + "&branch=" + urlArray[urlArray.length - 1];
                        bitbucketMapper.addRepository(projectKey, RepositoryUri.parse(url), bbUserName, bbPassword);
                        nextAction = "ForceSync";
                    }
                }
                else
                {
                    postCommitURL = "BitbucketPostCommit.jspa?projectKey=" + projectKey + "&branch=" + urlArray[urlArray.length - 1];
                    logger.debug(postCommitURL);
                    bitbucketMapper.addRepository(projectKey, RepositoryUri.parse(url), bbUserName, bbPassword);
                    nextAction = "ForceSync";
                }

            }

            if (nextAction.equals("ShowPostCommitURL"))
            {
                postCommitURL = "BitbucketPostCommit.jspa?projectKey=" + projectKey + "&branch=" + urlArray[urlArray.length - 1];
            }

            if (nextAction.equals("DeleteRepository"))
            {
                bitbucketMapper.removeRepository(projectKey, RepositoryUri.parse(url));
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
        synchronizer.synchronize(projectKey, RepositoryUri.parse(url));
    }

    public List getProjects()
    {
        return projects;
    }

    // Stored Repository + JIRA Projects
    public List<RepositoryUri> getProjectRepositories(String projectKey)
    {
        return bitbucketMapper.getRepositories(projectKey);
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

    public String getBaseURL()
    {
        return this.baseURL;
    }

    public String getRedirectURL()
    {
        return this.redirectURL;
    }

    public List<Progress> getProgress()
    {
        return progress;
    }
}
