package com.atlassian.jira.plugins.dvcs.webwork;

import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.software.api.conditions.ProjectDevToolsIntegrationFeatureCondition;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.atlassian.jira.software.api.conditions.ProjectDevToolsIntegrationFeatureCondition.CONTEXT_KEY_PROJECT;
import static com.google.common.base.Preconditions.checkNotNull;

@Component
public class PanelVisibilityManager
{
    @VisibleForTesting
    static final String DEV_STATUS_PHASE_TWO_FEATURE_FLAG = "jira.plugin.devstatus.phasetwo";

    @VisibleForTesting
    static final String DEV_STATUS_PLUGIN_ID = "com.atlassian.jira.plugins.jira-development-integration-plugin";

    private final FeatureManager featureManager;
    private final PluginAccessor pluginAccessor;
    private final ProjectDevToolsIntegrationFeatureCondition projectDevToolsIntegrationFeatureCondition;

    @Autowired
    @SuppressWarnings("SpringJavaAutowiringInspection")
    public PanelVisibilityManager(@ComponentImport FeatureManager featureManager,
            @ComponentImport PluginAccessor pluginAccessor,
            ProjectDevToolsIntegrationFeatureCondition projectDevToolsIntegrationFeatureCondition)
    {
        this.featureManager = checkNotNull(featureManager);
        this.pluginAccessor = checkNotNull(pluginAccessor);
        this.projectDevToolsIntegrationFeatureCondition = checkNotNull(projectDevToolsIntegrationFeatureCondition);
    }

    public boolean showPanel(Issue issue, ApplicationUser user)
    {
        return projectDevToolsIntegrationFeatureConditionIsSatisfied(issue.getProjectObject())
               && devStatusPluginDoesNotShowCommitInformation();
    }

    private boolean projectDevToolsIntegrationFeatureConditionIsSatisfied(Project project)
    {
        Map<String,Object> context = ImmutableMap.<String,Object>of(CONTEXT_KEY_PROJECT, project);
        return projectDevToolsIntegrationFeatureCondition.shouldDisplay(context);
    }

    private boolean devStatusPluginDoesNotShowCommitInformation()
    {
        return !pluginAccessor.isPluginEnabled(DEV_STATUS_PLUGIN_ID)
               || !featureManager.isEnabled(DEV_STATUS_PHASE_TWO_FEATURE_FLAG);
    }
}
