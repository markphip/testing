package com.atlassian.jira.plugins.dvcs.bitbucket.access.conditions;

import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import com.google.common.collect.ImmutableMap;
import org.mockito.InjectMocks;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Collections;

import static com.atlassian.jira.plugins.dvcs.bitbucket.access.conditions.IsBitbucketImporterCondition.BITBUCKET_IMPORTER_PLUGIN_KEY;
import static com.atlassian.jira.plugins.dvcs.bitbucket.access.conditions.IsBitbucketImporterCondition.CONTEXT_KEY_IMPORTER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@Listeners (MockitoTestNgListener.class)
public class IsBitbucketImporterConditionTest
{
    @InjectMocks
    private IsBitbucketImporterCondition isBitbucketImporterCondition;

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void conditionShouldExceptionWhenProjectIsNotSpecified()
    {
        isBitbucketImporterCondition.shouldDisplay(Collections.<String, Object>emptyMap());
    }

    @Test
    public void shouldReturnTrueWhenImporterIsBitBucket()
    {
        ImmutableMap<String, Object> context = ImmutableMap.<String,Object>of(CONTEXT_KEY_IMPORTER, BITBUCKET_IMPORTER_PLUGIN_KEY);
        assertThat(isBitbucketImporterCondition.shouldDisplay(context), is(true));
    }

    @Test
    public void shouldReturnFalseWhenImporterIsNotBitBucket()
    {
        ImmutableMap<String, Object> context = ImmutableMap.<String,Object>of(CONTEXT_KEY_IMPORTER, "");
        assertThat(isBitbucketImporterCondition.shouldDisplay(context), is(false));
    }
}
