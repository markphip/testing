package com.atlassian.jira.plugins.dvcs.webwork;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.compatibility.bridge.project.ProjectTypeKey;
import com.atlassian.jira.compatibility.bridge.project.UnlicensedProjectPageRendererBridge;
import com.atlassian.jira.config.CoreFeatures;
import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.plugins.dvcs.analytics.DvcsConfigPageShownAnalyticsEvent;
import com.atlassian.jira.plugins.dvcs.auth.OAuthStore;
import com.atlassian.jira.plugins.dvcs.listener.PluginFeatureDetector;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.InvalidOrganizationManager;
import com.atlassian.jira.plugins.dvcs.service.InvalidOrganizationsManagerImpl;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.remote.SyncDisabledHelper;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.githubenterprise.GithubEnterpriseCommunicator;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.software.api.conditions.SoftwareGlobalAdminCondition;
import com.atlassian.jira.web.action.ActionViewDataMappings;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import com.atlassian.webresource.api.assembler.RequiredResources;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.atlassian.jira.plugins.dvcs.ProjectTypeKey.SOFTWARE;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.emptyMap;

/**
 * Webwork action used to configure the bitbucket organizations
 */
@Scanned
public class ConfigureDvcsOrganizations extends JiraWebActionSupport
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigureDvcsOrganizations.class);
    private static final String CONTENT_DATA_MAP_KEY = "content";
    private static final String DEFAULT_SOURCE = CommonDvcsConfigurationAction.DEFAULT_SOURCE;
    private static final String SYNCHRONIZATION_DISABLED_TITLE = "%s synchronization disabled";
    private static final String SYNCHRONIZATION_ALL_DISABLED_TITLE = "Synchronization disabled";
    private static final String SYNCHRONIZATION_DISABLED_MESSAGE = "Atlassian has temporarily disabled synchronization with %s for maintenance. Activity during this period will sync once connectivity is restored. Thank you for your patience.";
    private static final String SYNCHRONIZATION_ALL_DISABLED_MESSAGE = "Atlassian has temporarily disabled synchronization for maintenance. Activity during this period will sync once connectivity is restored. Thank you for your patience.";
    private static final String UNLICENSED_PAGE_WEB_RESOURCE = "com.atlassian.jira.plugins.jira-bitbucket-connector-plugin:unlicensed-page-resources";

    @VisibleForTesting
    static final String UNLICENSED = "unlicensed";

    private final EventPublisher eventPublisher;
    private final FeatureManager featureManager;
    private final InvalidOrganizationManager invalidOrganizationsManager;
    private final OAuthStore oAuthStore;
    private final OrganizationService organizationService;
    private final PageBuilderService pageBuilderService;
    private final PluginFeatureDetector pluginFeatureDetector;
    private final SoftwareGlobalAdminCondition softwareGlobalAdminCondition;
    private final SyncDisabledHelper syncDisabledHelper;
    private final UnlicensedProjectPageRendererBridge unlicensedProjectPageRendererBridge;

    private String postCommitRepositoryType;
    private String source;

    @Autowired
    public ConfigureDvcsOrganizations(@ComponentImport EventPublisher eventPublisher,
            @ComponentImport FeatureManager featureManager, OAuthStore oAuthStore, OrganizationService organizationService,
            @ComponentImport PageBuilderService pageBuilderService, PluginFeatureDetector featuresDetector,
            @ComponentImport PluginSettingsFactory pluginSettingsFactory, SoftwareGlobalAdminCondition softwareGlobalAdminCondition,
            SyncDisabledHelper syncDisabledHelper, UnlicensedProjectPageRendererBridge unlicensedProjectPageRendererBridge)
    {
        checkNotNull(pluginSettingsFactory);

        this.eventPublisher = checkNotNull(eventPublisher);
        this.featureManager = checkNotNull(featureManager);
        this.invalidOrganizationsManager = new InvalidOrganizationsManagerImpl(pluginSettingsFactory);
        this.oAuthStore = checkNotNull(oAuthStore);
        this.organizationService = checkNotNull(organizationService);
        this.pageBuilderService = checkNotNull(pageBuilderService);
        this.pluginFeatureDetector = checkNotNull(featuresDetector);
        this.softwareGlobalAdminCondition = checkNotNull(softwareGlobalAdminCondition);
        this.syncDisabledHelper = checkNotNull(syncDisabledHelper);
        this.unlicensedProjectPageRendererBridge = checkNotNull(unlicensedProjectPageRendererBridge);
    }

    @Override
    protected void doValidation()
    {
    }

    @Override
    @RequiresXsrfCheck
    protected String doExecute() throws Exception
    {
        LOGGER.debug("Configure organization default action.");
        eventPublisher.publish(new DvcsConfigPageShownAnalyticsEvent(getSourceOrDefault()));
        return INPUT;
    }

    public String doDefault() throws Exception
    {
        if (softwareGlobalAdminCondition.shouldDisplay(emptyMap()))
        {
            return doExecute();
        }

        return UNLICENSED;
    }

    @ActionViewDataMappings (UNLICENSED)
    public Map<String,Object> getUnlicensedViewData()
    {
        checkState(unlicensedProjectPageRendererBridge.isBridgeActive(), "unlicensedProjectPageRendererBridge should be active.");

        RequiredResources requiredResources = pageBuilderService.assembler().resources()
                .requireWebResource(UNLICENSED_PAGE_WEB_RESOURCE);
        ProjectTypeKey softwareKey = new ProjectTypeKey(SOFTWARE);
        return ImmutableMap.of(CONTENT_DATA_MAP_KEY, unlicensedProjectPageRendererBridge.render(requiredResources, softwareKey));
    }

    public List<Organization> loadOrganizations()
    {
        List<Organization> allOrganizations = organizationService.getAll(true);
        sort(allOrganizations);
        return allOrganizations;
    }

    public boolean isInvalidOrganization(Organization organization)
    {
        return !invalidOrganizationsManager.isOrganizationValid(organization.getId());
    }

    /**
     * Custom sorting of organizations - integrated accounts are displayed on top.
     */
    private void sort(List<Organization> allOrganizations)
    {
        Collections.sort(allOrganizations, new Comparator<Organization>()
        {
            @Override
            public int compare(Organization org1, Organization org2)
            {
                // integrated accounts has precedence
                if (org1.isIntegratedAccount() && !org2.isIntegratedAccount())
                {
                    return -1;

                }
                else if (!org1.isIntegratedAccount() && org2.isIntegratedAccount())
                {
                    return +1;

                }
                else
                {
                    // by default compares via name
                    return org1.getName().toLowerCase().compareTo(org2.getName().toLowerCase());
                }

            }
        });
    }

    public String getPostCommitRepositoryType()
    {
        return postCommitRepositoryType;
    }

    public void setPostCommitRepositoryType(String postCommitRepositoryType)
    {
        this.postCommitRepositoryType = postCommitRepositoryType;
    }

    public boolean isOnDemandLicense()
    {
        return featureManager.isEnabled(CoreFeatures.ON_DEMAND);
    }

    public boolean isUserInvitationsEnabled()
    {
        return pluginFeatureDetector.isUserInvitationsEnabled();
    }

    public boolean isIntegratedAccount(Organization org)
    {
        return org.isIntegratedAccount();
    }

    public OAuthStore getOAuthStore()
    {
        return oAuthStore;
    }

    public String getSource()
    {
        return source;
    }

    public String getSourceOrDefault()
    {
        return StringUtils.defaultIfEmpty(source, DEFAULT_SOURCE);
    }

    public void setSource(String source)
    {
        this.source = source;
    }

    public boolean isGitHubSyncDisabled()
    {
        return syncDisabledHelper.isGithubSyncDisabled();
    }

    public boolean isBitbucketSyncDisabled()
    {
        return syncDisabledHelper.isBitbucketSyncDisabled();
    }

    public boolean isGitHubEnterpriseSyncDisabled()
    {
        return syncDisabledHelper.isGithubEnterpriseSyncDisabled();
    }

    public boolean isAnySyncDisabled()
    {
        return syncDisabledHelper.isBitbucketSyncDisabled() || syncDisabledHelper.isGithubSyncDisabled() || syncDisabledHelper.isGithubEnterpriseSyncDisabled();
    }

    public boolean isAllSyncDisabled()
    {
        return syncDisabledHelper.isBitbucketSyncDisabled() && syncDisabledHelper.isGithubSyncDisabled() && syncDisabledHelper.isGithubEnterpriseSyncDisabled();
    }

    public String getSyncDisabledWarningTitle()
    {
        if (!isAnySyncDisabled())
        {
            return null;
        }

        if (syncDisabledHelper.isSyncDisabled())
        {
            // All synchronizations are disabled
            return SYNCHRONIZATION_ALL_DISABLED_TITLE;
        }

        return String.format(SYNCHRONIZATION_DISABLED_TITLE, getDisabledSystemsList());
    }

    public String getSyncDisabledWarningMessage()
    {
        if (!isAnySyncDisabled())
        {
            return null;
        }

        if (syncDisabledHelper.isSyncDisabled())
        {
            // All synchronizations are disabled
            return SYNCHRONIZATION_ALL_DISABLED_MESSAGE;
        }

        return String.format(SYNCHRONIZATION_DISABLED_MESSAGE, getDisabledSystemsList());
    }

    private String getDisabledSystemsList()
    {
        return Joiner.on("/").skipNulls().join(
                syncDisabledHelper.isBitbucketSyncDisabled() ? "Bitbucket" : null,
                syncDisabledHelper.isGithubSyncDisabled() ? "GitHub" : null,
                syncDisabledHelper.isGithubEnterpriseSyncDisabled() ? "GitHub Enterprise" : null
        );
    }

    public boolean isSyncDisabled(String dvcsType)
    {
        if (BitbucketCommunicator.BITBUCKET.equals(dvcsType))
        {
            return isBitbucketSyncDisabled();
        }

        if (GithubCommunicator.GITHUB.equals(dvcsType))
        {
            return isGitHubSyncDisabled();
        }

        if (GithubEnterpriseCommunicator.GITHUB_ENTERPRISE.equals(dvcsType))
        {
            return isGitHubEnterpriseSyncDisabled();
        }

        return false;
    }
}
