package com.atlassian.jira.plugins.dvcs.webwork;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.dvcs.analytics.DvcsIssueAnalyticsEvent;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.issuetabpanel.IssueAction;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.ContextProvider;

public class DvcsTabPanelContextProvider implements ContextProvider
{

    private final Logger logger = LoggerFactory.getLogger(DvcsTabPanelContextProvider.class);

    private final ChangesetRenderer changesetRenderer;
    private final EventPublisher eventPublisher;
    private final RepositoryService repositoryService;

    public DvcsTabPanelContextProvider(ChangesetRenderer changesetRenderer, RepositoryService repositoryService, EventPublisher eventPublisher)
    {
        this.changesetRenderer = changesetRenderer;
        this.repositoryService = repositoryService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void init(Map<String, String> stringStringMap) throws PluginParseException
    {
        // nop
    }

    @Override
    public Map<String, Object> getContextMap(Map<String, Object> context)
    {
        Issue issue = (Issue) context.get("issue");

        final StringBuilder sb = new StringBuilder();

        Long items = 0L;

        try
        {
            final List<IssueAction> actions = changesetRenderer.getAsActions(issue);
            boolean isNoMessages = isNoMessages(actions);
            if (isNoMessages)
            {
                sb.append("<div class=\"ghx-container\"><p class=\"ghx-fa\">" + ChangesetRenderer.DEFAULT_MESSAGE_GH_TXT + "</p></div>");
            } else
            {
                for (IssueAction issueAction : actions) {
                    sb.append(issueAction.getHtml());
                    items += 1;
                }
            }
        } catch (SourceControlException e)
        {
            logger.debug("Could not retrieve changeset for [ " + issue.getKey() + " ]: " + e, e);
        }

        HashMap<String, Object> params = new HashMap<String, Object>();

        params.put("renderedChangesetsWithHtml", new Callable<String>()
        {
            @Override
            public String call()
            {
                if (!repositoryService.existsLinkedRepositories())
                {
                    eventPublisher.publish(new DvcsIssueAnalyticsEvent("agile", "tabshowing", false));
                } else
                {
                    eventPublisher.publish(new DvcsIssueAnalyticsEvent("agile", "tabshowing", true));

                }

                return sb.toString();
            }
        });
        params.put("atl.gh.issue.details.tab.count", items);

        return params;
    }

    private boolean isNoMessages(List<IssueAction> actions)
    {
        return actions.isEmpty();
    }
}
