package com.atlassian.jira.plugins.dvcs.webwork;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.compatibility.bridge.project.UnlicensedProjectPageRendererBridge;
import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.plugins.dvcs.auth.OAuthStore;
import com.atlassian.jira.plugins.dvcs.listener.PluginFeatureDetector;
import com.atlassian.jira.plugins.dvcs.service.InvalidOrganizationManager;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.remote.SyncDisabledHelper;
import com.atlassian.jira.software.api.conditions.SoftwareGlobalAdminCondition;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static com.atlassian.jira.plugins.dvcs.webwork.ConfigureDvcsOrganizations.UNLICENSED;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.when;
import static webwork.action.Action.INPUT;

@RunWith (MockitoJUnitRunner.class)
public class ConfigureDvcsOrganizationsTest
{
    @InjectMocks
    private ConfigureDvcsOrganizations configureDvcsOrganizations;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private FeatureManager featureManager;

    @Mock
    private InvalidOrganizationManager invalidOrganizationsManager;

    @Mock
    private OAuthStore oAuthStore;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private PageBuilderService pageBuilderService;

    @Mock
    private PluginFeatureDetector pluginFeatureDetector;

    @Mock
    private PluginSettingsFactory pluginSettingsFactory;

    @Mock
    private SoftwareGlobalAdminCondition softwareGlobalAdminCondition;

    @Mock
    private SyncDisabledHelper syncDisabledHelper;

    @Mock
    private UnlicensedProjectPageRendererBridge unlicensedProjectPageRendererBridge;

    @Test
    public void shouldReturnUnlicensedViewWhenDoDefaultIsInvokedAndSoftwareGlobalAdminConditionIsNotMet()
            throws Exception
    {
        when(softwareGlobalAdminCondition.shouldDisplay(anyMap())).thenReturn(false);

        String result = configureDvcsOrganizations.doDefault();

        assertThat(result, is(UNLICENSED));
    }

    @Test
    public void shouldReturnInputViewWhenDoDefaultIsInvokedAndSoftwareGlobalAdminConditionIsMet() throws Exception
    {
        when(softwareGlobalAdminCondition.shouldDisplay(anyMap())).thenReturn(true);

        String result = configureDvcsOrganizations.doDefault();

        assertThat(result, is(INPUT));
    }
}