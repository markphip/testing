package com.atlassian.jira.plugins.dvcs.webwork;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.changehistory.ChangeHistoryManager;
import com.atlassian.jira.plugins.dvcs.util.SystemUtils;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;

import java.util.Collections;
import java.util.Set;

/**
 * Utility class to retrieve historical issue a project keys for issue
 *
 * @author Miroslav Stencel <mstencel@atlassian.com>
 */
public class IssueAndProjectKeyManagerImpl implements IssueAndProjectKeyManager
{
    private final IssueManager issueManager;
    private final ChangeHistoryManager changeHistoryManager;
    private final ProjectManager projectManager;

    public IssueAndProjectKeyManagerImpl(final IssueManager issueManager, final ChangeHistoryManager changeHistoryManager, final ProjectManager projectManager)
    {
        this.issueManager = issueManager;
        this.changeHistoryManager = changeHistoryManager;
        this.projectManager = projectManager;
    }

    @Override
    public Set<String> getAllIssueKeys(Issue issue)
    {
        if (issue == null)
        {
            return Collections.emptySet();
        }
        return SystemUtils.getAllIssueKeys(issueManager, changeHistoryManager, issue);
    }

    @Override
    public Set<String> getAllIssueKeys(String issueKey)
    {
        MutableIssue issue = issueManager.getIssueObject(issueKey);
        if (issue == null)
        {
            // the issue has not been found, but we will use the issueKey
            return Collections.singleton(issueKey);
        }
        return SystemUtils.getAllIssueKeys(issueManager, changeHistoryManager, issue);
    }

    @Override
    public Set<String> getAllProjectKeys(Project project)
    {
        return SystemUtils.getAllProjectKeys(projectManager, project);
    }

    @Override
    public Set<String> getAllProjectKeys(String projectKey)
    {
        return SystemUtils.getAllProjectKeys(projectManager, projectManager.getProjectObjByKey(projectKey));
    }
}