package com.atlassian.jira.plugins.dvcs.conditions;

import com.atlassian.jira.config.FeatureManager;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugin.web.Condition;

import java.util.Map;

import static com.atlassian.jira.config.CoreFeatures.LICENSE_ROLES_ENABLED;

/**
 * This component should be removed when DVCS connector fully enters JIRA Software.
 */
public class LicenseRolesEnabled implements Condition
{
    private final FeatureManager featureManager;

    public LicenseRolesEnabled(@ComponentImport FeatureManager featureManager)
    {
        this.featureManager = featureManager;
    }


    @Override
    public void init(final Map<String, String> map) throws PluginParseException
    {
    }

    @Override
    public boolean shouldDisplay(final Map<String, Object> map)
    {
        return featureManager.isEnabled(LICENSE_ROLES_ENABLED);
    }
}
