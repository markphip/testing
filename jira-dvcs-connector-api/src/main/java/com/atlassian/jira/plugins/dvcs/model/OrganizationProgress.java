package com.atlassian.jira.plugins.dvcs.model;

import java.util.Set;

public interface OrganizationProgress
{
    void startAddingOrganization(int id);

    void stopAddingOrganization(int id);

    Set<Integer> getAddingOrgs();
}
