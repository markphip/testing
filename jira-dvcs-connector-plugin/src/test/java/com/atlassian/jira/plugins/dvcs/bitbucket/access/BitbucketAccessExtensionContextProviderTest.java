package com.atlassian.jira.plugins.dvcs.bitbucket.access;

import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import com.atlassian.webresource.api.assembler.RequiredData;
import com.atlassian.webresource.api.assembler.RequiredResources;
import com.atlassian.webresource.api.assembler.WebResourceAssembler;
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
import static org.mockito.Mockito.verifyZeroInteractions;
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

    @Mock
    protected PageBuilderService pageBuilderService;

    @Mock
    protected RequiredData requiredData;

    @Mock
    protected RequiredResources requiredResources;

    @Mock
    protected WebResourceAssembler webResourceAssembler;

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
        assertThatContextMapHasEntry(CONTEXT_KEY_JIRA_BASE_URL, JIRA_BASE_URL);
    }

    @Test
    public void shouldContainMoreCount()
    {
        assertThatContextMapHasEntry(CONTEXT_KEY_MORE_COUNT, 2);
    }

    @Test
    public void shouldContainMoreTeams()
    {
        assertThatContextMapHasEntry(CONTEXT_KEY_MORE_TEAMS, asList("Fusion Renaissance", "Yet another team"));
    }

    @Test
    public void shouldContainACollectionOfOrganizationsWithDefaultGroups()
    {
        assertThatContextMapHasEntry(CONTEXT_KEY_TEAMS_WITH_DEFAULT_GROUPS, getOrganizationNames());
    }

    @Test
    public void shouldNotRequireResourcesAndDataWhenThereAreNoBitbucketTeamsWithDefaultGroups()
    {
        when(bitbucketTeamService.getTeamsWithDefaultGroups()).thenReturn(emptyList());

        bitbucketAccessExtensionContextProvider.getContextMap(emptyMap());

        verifyZeroInteractions(requiredResources, requiredData);
    }

    @Test
    public void shouldContainAnEmptyListWhenThereAreNoBitbucketTeamsWithDefaultGroups()
    {
        when(bitbucketTeamService.getTeamsWithDefaultGroups()).thenReturn(emptyList());

        assertThatContextMapHasEntry(CONTEXT_KEY_TEAMS_WITH_DEFAULT_GROUPS, emptyList());
    }

    protected BitbucketAccessExtensionContextProvider getInstanceUnderTest()
    {
        return new BitbucketAccessExtensionContextProvider(applicationProperties, bitbucketTeamService, pageBuilderService)
        {
            @Override
            protected void requireResourcesAndData(final List<Organization> bitbucketTeamsWithDefaultGroups)
            {

            }
        };
    }

    private void assertThatContextMapHasEntry(final String key, final Object value)
    {
        Map<String,Object> context = bitbucketAccessExtensionContextProvider.getContextMap(emptyMap());

        assertThat(context, hasEntry(key, value));
    }
}
