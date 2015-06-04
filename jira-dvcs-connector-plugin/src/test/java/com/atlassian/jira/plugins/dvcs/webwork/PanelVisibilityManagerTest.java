package com.atlassian.jira.plugins.dvcs.webwork;

import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.software.api.conditions.ProjectDevToolsIntegrationFeatureCondition;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.PluginAccessor;
import org.hamcrest.Matchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Map;

import static com.atlassian.jira.plugins.dvcs.webwork.PanelVisibilityManager.DEV_STATUS_PHASE_TWO_FEATURE_FLAG;
import static com.atlassian.jira.plugins.dvcs.webwork.PanelVisibilityManager.DEV_STATUS_PLUGIN_ID;
import static com.atlassian.jira.software.api.conditions.ProjectDevToolsIntegrationFeatureCondition.CONTEXT_KEY_PROJECT;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Basic set of tests for PanelVisibilityManager
 *
 * @since v6.0
 */
public class PanelVisibilityManagerTest
{
    @Mock
    private ApplicationUser user;

    @Mock
    private FeatureManager featureManager;

    @Mock
    private Issue issue;

    @InjectMocks
    private PanelVisibilityManager panelVisibilityManager;

    @Mock
    private PluginAccessor pluginAccessor;

    @Mock
    private Project project;

    @Mock
    private ProjectDevToolsIntegrationFeatureCondition projectDevToolsIntegrationFeatureCondition;

    @BeforeTest
    public void initMocks()
    {
        MockitoAnnotations.initMocks(this);

        when(issue.getProjectObject()).thenReturn(project);
    }

    @AfterMethod
    public void resetMocks()
    {
        reset(projectDevToolsIntegrationFeatureCondition);
    }

    @DataProvider (name = "visibilityTestCases")
    public static Object[][] visibilityTestCases()
    {
        //The first boolean specifies whether the SW condition is satisfied. This covers: license, user role, project type, and view dev tools permission
        //The second boolean specifies whether the dev status plugin is enabled
        //The third boolean specifies whether the phase two feature flag is enabled
        //The fourth boolean specifies whether the commits tab panel should be visible given the test case
        return new Object[][] {
                {false, false, false, false},
                {false, false, true, false},
                {false, true, false, false},
                {false, true, true, false},
                {true, false, false, true},
                {true, false, true, true},
                {true, true, false, true},
                {true, true, true, false}
        };
    }

    @Test(dataProvider = "visibilityTestCases")
    public void testPanelVisibility(boolean softwareConditionIsSatisfied, boolean isDevStatusPluginEnabled,
            boolean isPhaseTwoFeatureFlagEnabled, boolean commitsPanelVisible)
    {
        when(projectDevToolsIntegrationFeatureCondition.shouldDisplay(anyMapOf(String.class, Object.class))).thenReturn(softwareConditionIsSatisfied);
        when(pluginAccessor.isPluginEnabled(eq(DEV_STATUS_PLUGIN_ID))).thenReturn(isDevStatusPluginEnabled);
        when(featureManager.isEnabled(DEV_STATUS_PHASE_TWO_FEATURE_FLAG)).thenReturn(isPhaseTwoFeatureFlagEnabled);

        assertThat(panelVisibilityManager.showPanel(issue, user), is(commitsPanelVisible));

        verify(projectDevToolsIntegrationFeatureCondition).shouldDisplay((Map<String,Object>) argThat(Matchers.<String,Object>hasEntry(CONTEXT_KEY_PROJECT, project)));
    }
}
