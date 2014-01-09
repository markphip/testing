package com.atlassian.jira.plugins.dvcs.model;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultOrganizationProgress implements OrganizationProgress
{
    private final Map<Integer, Boolean> addingOrgs = new ConcurrentHashMap<Integer, Boolean>();

    @Override
    public void startAddingOrganization(final int id)
    {
        this.addingOrgs.put(id, Boolean.TRUE);
    }

    @Override
    public void stopAddingOrganization(final int id)
    {
        this.addingOrgs.remove(id);
    }

    @Override
    public Set<Integer> getAddingOrgs()
    {
        return addingOrgs.keySet();
    }
}
