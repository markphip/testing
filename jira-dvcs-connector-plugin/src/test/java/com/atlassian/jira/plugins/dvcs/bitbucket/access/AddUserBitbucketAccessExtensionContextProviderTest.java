package com.atlassian.jira.plugins.dvcs.bitbucket.access;

import com.atlassian.jira.compatibility.bridge.application.ApplicationRoleManagerBridge;
import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import com.atlassian.webresource.api.assembler.RequiredResources;
import com.atlassian.webresource.api.assembler.WebResourceAssembler;
import com.google.common.collect.Sets;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.atlassian.jira.plugins.dvcs.ApplicationKey.SOFTWARE;
import static com.atlassian.jira.plugins.dvcs.bitbucket.access.AddUserBitbucketAccessExtensionContextProvider.CONTEXT_KEY_INVITE_TO_GROUPS;
import static com.atlassian.jira.plugins.dvcs.bitbucket.access.AddUserBitbucketAccessExtensionContextProvider.CONTEXT_KEY_INVITE_USER_BY_DEFAULT;
import static com.atlassian.jira.plugins.dvcs.bitbucket.access.AddUserBitbucketAccessExtensionContextProvider.CONTEXT_KEY_MORE_COUNT;
import static com.atlassian.jira.plugins.dvcs.bitbucket.access.AddUserBitbucketAccessExtensionContextProvider.CONTEXT_KEY_MORE_TEAMS;
import static com.atlassian.jira.plugins.dvcs.bitbucket.access.AddUserBitbucketAccessExtensionContextProvider.CONTEXT_KEY_TEAMS_WITH_DEFAULT_GROUPS;
import static com.atlassian.jira.plugins.dvcs.bitbucket.access.AddUserBitbucketAccessExtensionContextProvider.REQUIRED_WEB_RESOURCE_COMPLETE_KEY;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Listeners (MockitoTestNgListener.class)
public class AddUserBitbucketAccessExtensionContextProviderTest
{
    @InjectMocks
    private AddUserBitbucketAccessExtensionContextProvider addUserBitbucketAccessExtensionContextProvider;

    @Mock
    private ApplicationRoleManagerBridge applicationRoleManagerBridge;

    @Mock
    private BitbucketTeamService bitbucketTeamService;

    private Organization organization1;

    private Organization organization3;

    private Organization organization4;

    private Organization organization5;

    private Organization organization6;

    @Mock
    private PageBuilderService pageBuilderService;

    @Mock
    private RequiredResources requiredResources;

    @Mock
    private WebResourceAssembler webResourceAssembler;

    @BeforeMethod
    public void prepare()
    {
        when(applicationRoleManagerBridge.isBridgeActive()).thenReturn(true);
        when(applicationRoleManagerBridge.rolesEnabled()).thenReturn(true);

        List<Organization> bitbucketTeams = prepareBitbucketTeams();
        when(bitbucketTeamService.getTeamsWithDefaultGroups()).thenReturn(bitbucketTeams);

        when(pageBuilderService.assembler()).thenReturn(webResourceAssembler);
        when(webResourceAssembler.resources()).thenReturn(requiredResources);
    }

    private List<Organization> prepareBitbucketTeams()
    {
        organization1 = organization(1, "Atlassian", "developers");
        organization3 = organization(3, "Fusion", "administrators", "developers");
        organization4 = organization(4, "Atlassian Labs", "administrators");
        organization5 = organization(5, "Fusion Renaissance", "developers");
        organization6 = organization(6, "Yet another team", "administrators", "developers");
        return asList(organization1, organization3, organization4, organization5, organization6);
    }

    private Organization organization(int id, String name, String ... defaultGroupNames)
    {
        Set<Group> defaultGroups = Sets.newLinkedHashSet();
        for (String defaultGroupName : defaultGroupNames)
        {
            Group defaultGroup = mock(Group.class);
            when(defaultGroup.getSlug()).thenReturn(defaultGroupName);
            defaultGroups.add(defaultGroup);
        }

        Organization organization = mock(Organization.class);
        when(organization.getId()).thenReturn(id);
        when(organization.getName()).thenReturn(name);
        when(organization.getDefaultGroups()).thenReturn(defaultGroups);
        return organization;
    }

    @Test
    public void shouldContainACollectionOfOrganizationsWithDefaultGroups()
    {
        Map<String,Object> context = addUserBitbucketAccessExtensionContextProvider.getContextMap(emptyMap());

        assertThat(context, hasEntry(CONTEXT_KEY_TEAMS_WITH_DEFAULT_GROUPS,
                asList(organization1.getName(), organization3.getName(), organization4.getName(), organization5.getName(), organization6.getName())));
    }

    @Test
    public void shouldContainAnEmptyListWhenThereAreNoBitbucketTeamsWithDefaultGroups()
    {
        when(bitbucketTeamService.getTeamsWithDefaultGroups()).thenReturn(emptyList());

        Map<String,Object> context = addUserBitbucketAccessExtensionContextProvider.getContextMap(emptyMap());

        assertThat(context, hasEntry(CONTEXT_KEY_TEAMS_WITH_DEFAULT_GROUPS, emptyList()));
    }

    @Test
    public void shouldContainAStringRepresentationOfAllGroupsUserWillBeInvitedTo()
    {
        Map<String,Object> context = addUserBitbucketAccessExtensionContextProvider.getContextMap(emptyMap());

        assertThat(context, hasEntry(CONTEXT_KEY_INVITE_TO_GROUPS,
                "1:developers;3:administrators;3:developers;4:administrators;5:developers;6:administrators;6:developers"));
    }

    @Test
    public void shouldContainEmptyCollectionWhenThereAreNoBitbucketTeamsWithDefaultGroups()
    {
        when(bitbucketTeamService.getTeamsWithDefaultGroups()).thenReturn(emptyList());

        Map<String,Object> context = addUserBitbucketAccessExtensionContextProvider.getContextMap(emptyMap());

        assertThat(context, hasEntry(CONTEXT_KEY_TEAMS_WITH_DEFAULT_GROUPS, emptyList()));
        assertThat(context, hasEntry(CONTEXT_KEY_INVITE_TO_GROUPS, " "));
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void shouldThrowIllegalStateExceptionWhenApplicationRoleManagerBridgeIsInactive()
    {
        when(applicationRoleManagerBridge.isBridgeActive()).thenReturn(false);

        addUserBitbucketAccessExtensionContextProvider.getContextMap(emptyMap());
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void shouldThrowIllegalStateExceptionWhenApplicationRoleManagerBridgeIsActiveAndRolesIsNotEnabled()
    {
        when(applicationRoleManagerBridge.rolesEnabled()).thenReturn(false);

        addUserBitbucketAccessExtensionContextProvider.getContextMap(emptyMap());
    }

    @Test
    public void shouldInviteUserByDefaultWhenApplicationRoleManagerBridgeIsActiveAndRolesIsEnabledAndSoftwareIsPartOfTheDefaultApplicationKeys()
    {
        when(applicationRoleManagerBridge.getDefaultApplicationKeys()).thenReturn(Sets.newHashSet(SOFTWARE));

        Map<String,Object> context = addUserBitbucketAccessExtensionContextProvider.getContextMap(emptyMap());

        assertThat(context, hasEntry(CONTEXT_KEY_INVITE_USER_BY_DEFAULT, true));
    }

    @Test
    public void shouldNotInviteUserByDefaultWhenApplicationRoleManagerBridgeIsActiveAndRolesIsEnabledAndSoftwareIsNotPartOfTheDefaultApplicationKeys()
    {
        when(applicationRoleManagerBridge.getDefaultApplicationKeys()).thenReturn(Sets.newHashSet("jira-core"));

        Map<String,Object> context = addUserBitbucketAccessExtensionContextProvider.getContextMap(emptyMap());

        assertThat(context, hasEntry(CONTEXT_KEY_INVITE_USER_BY_DEFAULT, false));
    }

    @Test
    public void shouldRequireWebResource()
    {
        addUserBitbucketAccessExtensionContextProvider.getContextMap(emptyMap());

        verify(requiredResources).requireWebResource(REQUIRED_WEB_RESOURCE_COMPLETE_KEY);
    }

    @Test
    public void shouldContainMoreCount()
    {
        Map<String,Object> context = addUserBitbucketAccessExtensionContextProvider.getContextMap(emptyMap());

        assertThat(context, hasEntry(CONTEXT_KEY_MORE_COUNT, 2));
    }

    @Test
    public void shouldContainMoreTeams()
    {
        Map<String,Object> context = addUserBitbucketAccessExtensionContextProvider.getContextMap(emptyMap());

        assertThat(context, hasEntry(CONTEXT_KEY_MORE_TEAMS, asList(organization5.getName(), organization6.getName())));
    }
}