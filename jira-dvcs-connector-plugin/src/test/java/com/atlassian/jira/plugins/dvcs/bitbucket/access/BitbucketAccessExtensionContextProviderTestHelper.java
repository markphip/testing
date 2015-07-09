package com.atlassian.jira.plugins.dvcs.bitbucket.access;

import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BitbucketAccessExtensionContextProviderTestHelper
{

    private static Organization organization1;

    private static Organization organization2;

    private static Organization organization3;

    private static Organization organization4;

    private static Organization organization5;

    static List<Organization> prepareBitbucketTeams()
    {
        organization1 = organization(1, "Atlassian", "developers");
        organization2 = organization(2, "Fusion", "administrators", "developers");
        organization3 = organization(3, "Atlassian Labs", "administrators");
        organization4 = organization(4, "Fusion Renaissance", "developers");
        organization5 = organization(5, "Yet another team", "administrators", "developers");
        return asList(organization1, organization2, organization3, organization4, organization5);
    }

    private static Organization organization(int id, String name, String ... defaultGroupNames)
    {
        final Set<Group> defaultGroups = Sets.newLinkedHashSet();
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

    static List<String> getOrganizationNames()
    {
        return asList(organization1.getName(), organization2.getName(), organization3.getName(), organization4.getName(), organization5.getName());
    }
}
