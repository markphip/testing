package com.atlassian.jira.plugins.dvcs.bitbucket.access;

import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.google.common.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;

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
        return organizationService.getAll(false, BITBUCKET_DVCS_TYPE)
                .stream()
                .filter(o -> !o.getDefaultGroups().isEmpty())
                .collect(toList());
    }
}
