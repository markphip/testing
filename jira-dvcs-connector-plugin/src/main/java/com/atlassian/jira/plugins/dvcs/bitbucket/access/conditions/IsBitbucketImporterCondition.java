package com.atlassian.jira.plugins.dvcs.bitbucket.access.conditions;

import com.atlassian.plugin.web.Condition;
import com.google.common.annotations.VisibleForTesting;

import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Checks whether JIM is importing users via bitbucket importer plugin.
 *
 * This condition was used to hide Bitbucket invite message in JIM user access page when users are imported using the bitbucket importer plugin
 * But now it is not used any more
 */
public class IsBitbucketImporterCondition implements Condition
{
    @VisibleForTesting
    static final String BITBUCKET_IMPORTER_PLUGIN_KEY = "com.atlassian.jira.plugins.jira-importers-bitbucket-plugin:BitbucketImporterKey";

    @VisibleForTesting
    static final String CONTEXT_KEY_IMPORTER = "importerKey";

    @Override
    public void init(final Map<String, String> map)
    {

    }

    @Override
    public boolean shouldDisplay(final Map<String, Object> context)
    {
        checkArgument(context.get(CONTEXT_KEY_IMPORTER) != null, "Expecting context map to contain the key '" + CONTEXT_KEY_IMPORTER + "' with a non-null value.");
        return BITBUCKET_IMPORTER_PLUGIN_KEY.equals(context.get(CONTEXT_KEY_IMPORTER));
    }
}
