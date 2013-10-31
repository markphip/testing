package com.atlassian.jira.plugins.dvcs.webwork;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.tabpanels.GenericMessageAction;
import com.atlassian.jira.plugin.issuetabpanel.AbstractIssueTabPanel;
import com.atlassian.jira.plugin.issuetabpanel.IssueAction;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.webwork.render.DefaultIssueAction;
import com.atlassian.jira.plugins.dvcs.webwork.render.IssueActionFactory;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class DvcsActivityTabPanel extends AbstractIssueTabPanel
{
    private final Logger logger = LoggerFactory.getLogger(DvcsActivityTabPanel.class);

    private static final GenericMessageAction DEFAULT_MESSAGE = new GenericMessageAction("No pull requests found.");
    private final PermissionManager permissionManager;
    private final RepositoryService repositoryService;
    private final RepositoryPullRequestDao activityDao;
    private final IssueActionFactory issueActionFactory;
    private final TemplateRenderer templateRenderer;
    private final IssueAndProjectKeyManager issueAndProjectKeyManager;

    private static final Comparator<? super IssueAction> ISSUE_ACTION_COMPARATOR = new Comparator<IssueAction>()
    {
        @Override
        public int compare(IssueAction o1, IssueAction o2)
        {
            DefaultIssueAction o1d = (DefaultIssueAction) o1;
            DefaultIssueAction o2d = (DefaultIssueAction) o2;
            if (o1 == null || o1.getTimePerformed() == null)
                return -1;
            if (o2 == null || o2.getTimePerformed() == null)
                return 1;
            return new Integer(o1d.getId()).compareTo(o2d.getId());
        }
    };

    public DvcsActivityTabPanel(PermissionManager permissionManager,
            RepositoryService repositoryService, RepositoryPullRequestDao activityDao,
            @Qualifier("aggregatedIssueActionFactory") IssueActionFactory issueActionFactory, TemplateRenderer templateRenderer, IssueAndProjectKeyManager issueAndProjectKeyManager)
    {
        this.permissionManager = permissionManager;
        this.repositoryService = repositoryService;
        this.activityDao = activityDao;
        this.issueActionFactory = issueActionFactory;
        this.templateRenderer = templateRenderer;
        this.issueAndProjectKeyManager = issueAndProjectKeyManager;
    }

    @Override
    public List<IssueAction> getActions(Issue issue, User user)
    {
        String issueKey = issue.getKey();
        Set<String> issueKeys = issueAndProjectKeyManager.getAllIssueKeys(issue);
        List<IssueAction> issueActions = new ArrayList<IssueAction>();

        //
        List<RepositoryPullRequestMapping> prs = activityDao.getPullRequestsForIssue(issueKeys);
        Map<String, Object> ctxt = Maps.newHashMap(new  ImmutableMap.Builder<String, Object>().put("prs", prs).build());
        issueActions.add(new DefaultIssueAction(templateRenderer, "/templates/activity/pr-view.vm", ctxt, new Date()));
        //
        //
        if (issueActions.isEmpty())
        {
            issueActions.add(DEFAULT_MESSAGE);
        }

        Collections.sort(issueActions, ISSUE_ACTION_COMPARATOR);
        return issueActions;
    }

    @Override
    public boolean showPanel(Issue issue, User user)
    {
        return permissionManager.hasPermission(Permissions.VIEW_VERSION_CONTROL, issue, user)
                && repositoryService.existsLinkedRepositories();
    }

}
