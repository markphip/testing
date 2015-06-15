package com.atlassian.jira.plugins.dvcs;

import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.scheduling.PluginScheduler;

/**
 * This class exists to provide @ComponentImport annotations for scanning for classes that are not annotated
 * on an actual DVCS class i.e. the pluginScheduler for the scheduler compat
 *
 * @since v6.3
 */
@Scanned
public class ScannerImport
{
    @ComponentImport
    private PluginScheduler pluginScheduler;

    @ComponentImport
    private ConstantsManager constantsManager;
}
