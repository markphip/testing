package com.atlassian.jira.plugins.dvcs.smartcommits;

import com.atlassian.jira.config.FeatureManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Dark feature to control Smart Commits operation in the DVCS Connector
 */
@Named
public class SmartcommitsDarkFeature
{
    public static final String KEY = "dvcs.connector.smartcommits.disabled";

    private final FeatureManager featureManager;

    @Inject
    public SmartcommitsDarkFeature(@ComponentImport @Nonnull final FeatureManager featureManager)
    {
        this.featureManager = checkNotNull(featureManager);
    }

    /**
     * @return <code>true</code> if this dark feature is disabled
     */
    public boolean isDisabled()
    {
        return featureManager.isEnabled(KEY);
    }
}
