package com.atlassian.jira.plugins.dvcs.bitbucket.access;

import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import com.google.common.collect.Sets;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;

import static com.atlassian.jira.plugins.dvcs.bitbucket.access.BitbucketTeamService.BITBUCKET_DVCS_TYPE;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@Listeners(MockitoTestNgListener.class)
public class BitbucketTeamServiceTest
{
    @InjectMocks
    private BitbucketTeamService bitbucketTeamService;

    @Mock
    private OrganizationService organizationService;

    @Test
    public void shouldReturnEmptyListWhenThereAreNoBitbucketTeams()
    {
        when(organizationService.getAll(false, BITBUCKET_DVCS_TYPE)).thenReturn(emptyList());

        List<Organization> bitbucketTeamsWithDefaultGroups = bitbucketTeamService.getTeamsWithDefaultGroups();

        assertThat(bitbucketTeamsWithDefaultGroups, is(emptyList()));
    }

    @Test
    public void shouldReturnEmptyListWhenThereAreNoBitbucketTeamsWithDefaultGroups()
    {
        List<Organization> bitbucketTeams = asList(organization(1), organization(2), organization(3));
        when(organizationService.getAll(false, BITBUCKET_DVCS_TYPE)).thenReturn(bitbucketTeams);

        List<Organization> bitbucketTeamsWithDefaultGroups = bitbucketTeamService.getTeamsWithDefaultGroups();

        assertThat(bitbucketTeamsWithDefaultGroups, is(emptyList()));
    }

    @Test
    public void shouldReturnBitbucketTeamsWithDefaultGroups()
    {
        Organization organization1 = organization(1, "developers");
        Organization organization2 = organization(2);
        Organization organization3 = organization(3, "administrators", "developers");
        List<Organization> bitbucketTeams = asList(organization1, organization2, organization3);
        when(organizationService.getAll(false, BITBUCKET_DVCS_TYPE)).thenReturn(bitbucketTeams);

        List<Organization> bitbucketTeamsWithDefaultGroups = bitbucketTeamService.getTeamsWithDefaultGroups();

        assertThat(bitbucketTeamsWithDefaultGroups, is(asList(organization1, organization3)));
    }

    private Organization organization(int id, String ... defaultGroupNames)
    {
        Set<Group> defaultGroups = Sets.newLinkedHashSet();
        for (String defaultGroupName : defaultGroupNames)
        {
            defaultGroups.add(new Group(defaultGroupName));
        }

        Organization organization = mock(Organization.class);
        when(organization.getId()).thenReturn(id);
        when(organization.getDefaultGroups()).thenReturn(defaultGroups);
        return organization;
    }
}