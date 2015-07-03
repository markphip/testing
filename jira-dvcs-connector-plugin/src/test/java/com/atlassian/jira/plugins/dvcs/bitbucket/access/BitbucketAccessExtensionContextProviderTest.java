package com.atlassian.jira.plugins.dvcs.bitbucket.access;

import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static com.atlassian.jira.config.properties.APKeys.JIRA_BASEURL;
import static com.atlassian.jira.plugins.dvcs.bitbucket.access.BitbucketAccessExtensionContextProvider.CONTEXT_KEY_JIRA_BASE_URL;
import static com.atlassian.jira.plugins.dvcs.bitbucket.access.BitbucketAccessExtensionContextProvider.CONTEXT_KEY_MORE_COUNT;
import static com.atlassian.jira.plugins.dvcs.bitbucket.access.BitbucketAccessExtensionContextProvider.CONTEXT_KEY_MORE_TEAMS;
import static com.atlassian.jira.plugins.dvcs.bitbucket.access.BitbucketAccessExtensionContextProvider.CONTEXT_KEY_TEAMS_WITH_DEFAULT_GROUPS;
import static com.atlassian.jira.plugins.dvcs.bitbucket.access.BitbucketAccessExtensionContextProviderTestHelper.getOrganizationNames;
import static com.atlassian.jira.plugins.dvcs.bitbucket.access.BitbucketAccessExtensionContextProviderTestHelper.prepareBitbucketTeams;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.mockito.Mockito.when;

@Listeners (MockitoTestNgListener.class)
public class BitbucketAccessExtensionContextProviderTest
{
    private static final String JIRA_BASE_URL = "http://example.com";

    private BitbucketAccessExtensionContextProvider bitbucketAccessExtensionContextProvider;

    @Mock
    protected ApplicationProperties applicationProperties;

    @Mock
    protected BitbucketTeamService bitbucketTeamService;

    @BeforeMethod
    public void prepare()
    {
        when(applicationProperties.getString(JIRA_BASEURL)).thenReturn(JIRA_BASE_URL);

        List<Organization> bitbucketTeams = prepareBitbucketTeams();
        when(bitbucketTeamService.getTeamsWithDefaultGroups()).thenReturn(bitbucketTeams);

        bitbucketAccessExtensionContextProvider = getInstanceUnderTest();
    }

    @Test
    public void shouldContainJiraBaseUrl()
    {
        Map<String,Object> context = bitbucketAccessExtensionContextProvider.getContextMap(emptyMap());

        assertThat(context, hasEntry(CONTEXT_KEY_JIRA_BASE_URL, JIRA_BASE_URL));
    }

    @Test
    public void shouldContainMoreCount()
    {
        Map<String,Object> context = bitbucketAccessExtensionContextProvider.getContextMap(emptyMap());

        assertThat(context, hasEntry(CONTEXT_KEY_MORE_COUNT, 2));
    }

    @Test
    public void shouldContainMoreTeams()
    {
        Map<String,Object> context = bitbucketAccessExtensionContextProvider.getContextMap(emptyMap());

        assertThat(context, hasEntry(CONTEXT_KEY_MORE_TEAMS, asList("Fusion Renaissance", "Yet another team")));
    }

    @Test
    public void shouldContainACollectionOfOrganizationsWithDefaultGroups()
    {
        Map<String,Object> context = bitbucketAccessExtensionContextProvider.getContextMap(emptyMap());

        assertThat(context, hasEntry(CONTEXT_KEY_TEAMS_WITH_DEFAULT_GROUPS,
                getOrganizationNames()));
    }

    @Test
    public void shouldContainAnEmptyListWhenThereAreNoBitbucketTeamsWithDefaultGroups()
    {
        when(bitbucketTeamService.getTeamsWithDefaultGroups()).thenReturn(emptyList());

        Map<String,Object> context = bitbucketAccessExtensionContextProvider.getContextMap(emptyMap());

        assertThat(context, hasEntry(CONTEXT_KEY_TEAMS_WITH_DEFAULT_GROUPS, emptyList()));
    }

    protected BitbucketAccessExtensionContextProvider getInstanceUnderTest()
    {
        return new BitbucketAccessExtensionContextProvider(applicationProperties, bitbucketTeamService)
        {
            @Override
            protected void requireResourcesAndData(final List<Organization> bitbucketTeamsWithDefaultGroups)
            {

            }
        };
    }
}