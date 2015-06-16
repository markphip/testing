package com.atlassian.jira.plugins.dvcs.bitbucket.access;

import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Lists.newArrayList;

/**
 * A service to get all Bitbucket teams that have default groups.
 */
@Component
class BitbucketTeamService
{
    @VisibleForTesting
    static final String BITBUCKET_DVCS_TYPE = "bitbucket";

    private final OrganizationService organizationService;

    @Autowired
    BitbucketTeamService(OrganizationService organizationService)
    {
        this.organizationService = checkNotNull(organizationService);
    }

    /**
     * Get all Bitbucket teams with default groups
     *
     * @return A list Bitbucket teams with default groups. If no such team
     *         exists, an empty list is returned.
     */
    List<Organization> getTeamsWithDefaultGroups()
    {
        return newArrayList(filter(organizationService.getAll(false, BITBUCKET_DVCS_TYPE), new Predicate<Organization>()
        {
            @Override
            public boolean apply(Organization organization)
            {
                return !organization.getDefaultGroups().isEmpty();
            }
        }));
    }
}
