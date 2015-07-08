package com.atlassian.jira.plugins.dvcs.bitbucket.access.conditions;

import com.atlassian.jira.plugins.dvcs.bitbucket.access.BitbucketTeamService;
import com.atlassian.jira.plugins.dvcs.bitbucket.access.conditions.BitbucketAccessExtensionCondition;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Listeners(MockitoTestNgListener.class)
public class BitbucketAccessExtensionConditionTest
{
    @InjectMocks
    private BitbucketAccessExtensionCondition bitbucketAccessExtensionCondition;

    @Mock
    private BitbucketTeamService bitbucketTeamService;

    @Test
    public void shouldReturnFalseWhenThereAreNoBitbucketTeamsWithDefaultGroups()
    {
        when(bitbucketTeamService.getTeamsWithDefaultGroups()).thenReturn(emptyList());

        boolean shouldDisplay = bitbucketAccessExtensionCondition.shouldDisplay(emptyMap());

        assertThat(shouldDisplay, is(false));
    }

    @Test
    public void shouldReturnTrueWhenThereAreSomeBitbucketTeamsWithDefaultGroups()
    {
        when(bitbucketTeamService.getTeamsWithDefaultGroups()).thenReturn(asList(mock(Organization.class)));

        boolean shouldDisplay = bitbucketAccessExtensionCondition.shouldDisplay(emptyMap());

        assertThat(shouldDisplay, is(true));
    }
}
