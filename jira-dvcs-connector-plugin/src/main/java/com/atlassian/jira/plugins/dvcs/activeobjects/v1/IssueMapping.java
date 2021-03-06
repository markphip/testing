package com.atlassian.jira.plugins.dvcs.activeobjects.v1;

import net.java.ao.Entity;

/**
 * Active objects storage for the mapping between a bitbucket repository and a jira project.
 */
@Deprecated
public interface IssueMapping extends Entity
{
    String getRepositoryUri();

    String getProjectKey();

    String getNode();

    String getIssueId();

    void setRepositoryUri(String owner);

    void setProjectKey(String projectKey);

    void setNode(String node);

    void setIssueId(String issueId);
}
