package com.atlassian.jira.plugins.dvcs.listener;

import com.atlassian.crowd.embedded.api.CrowdService;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.crowd.embedded.api.UserWithAttributes;
import com.atlassian.jira.compatibility.bridge.application.ApplicationRoleManagerBridge;
import com.atlassian.jira.plugins.dvcs.bitbucket.access.BitbucketTeamService;
import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.jira.software.api.roles.LicenseService;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.ApplicationUsers;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Invite users that are *not* created via the 'Create user' screen, e.g. cloud users creations via UM, importing users
 * using JIM, users created from Service Desk screens (or screens that belong to other plugins).
 *
 * This class differs from {@link com.atlassian.jira.plugins.dvcs.listener.UserAddedViaInterfaceEventProcessor} in the way
 * it determines the teams and groups that users will be invited to. Instead of relying on the administrator's selection
 * (which is non-existent in this case), it looks up all Bitbucket teams with default groups.
 *
 * It also has a small logic to check whether users should be invited. The requirement for invitation is slightly different
 * between Dark Ages and Renaissance.
 */
@Component
public class UserAddedExternallyEventProcessor
{
    private static final Logger LOGGER = LoggerFactory.getLogger(UserAddedExternallyEventProcessor.class);

    @VisibleForTesting
    static final String DVCS_TYPE_BITBUCKET = "bitbucket";

    /** BBC-957: Attribute key to recognise Service Desk Customers during user creation */
    @VisibleForTesting
    static final String SERVICE_DESK_CUSTOMERS_ATTRIBUTE_KEY = "synch.servicedesk.requestor";

    private final ApplicationRoleManagerBridge applicationRoleManagerBridge;

    private final BitbucketTeamService bitbucketTeamService;

    private final CrowdService crowdService;

    private final DvcsCommunicatorProvider dvcsCommunicatorProvider;

    private final LicenseService licenseService;

    @Autowired
    public UserAddedExternallyEventProcessor(ApplicationRoleManagerBridge applicationRoleManagerBridge,
            BitbucketTeamService bitbucketTeamService, @ComponentImport CrowdService crowdService,
            DvcsCommunicatorProvider dvcsCommunicatorProvider, @ComponentImport LicenseService licenseService)
    {
        this.applicationRoleManagerBridge = checkNotNull(applicationRoleManagerBridge);
        this.bitbucketTeamService = checkNotNull(bitbucketTeamService);
        this.crowdService = checkNotNull(crowdService);
        this.dvcsCommunicatorProvider = checkNotNull(dvcsCommunicatorProvider);
        this.licenseService = checkNotNull(licenseService);
    }

    public void process(ApplicationUser user)
    {
        checkArgument(user != null, "Expecting user to be non-null");

        if (shouldInvite(user))
        {
            invite(user);
        }
    }

    private boolean shouldInvite(ApplicationUser user)
    {
        return (isRenaissance() && licenseService.isSoftwareUser(user))
               || (!isRenaissance() && !isServiceDeskCustomer(user));
    }

    private boolean isRenaissance()
    {
        return applicationRoleManagerBridge.isBridgeActive() && applicationRoleManagerBridge.rolesEnabled();
    }

    private boolean isServiceDeskCustomer(ApplicationUser user)
    {
        User directoryUser = ApplicationUsers.toDirectoryUser(user);
        UserWithAttributes userWithAttributes = crowdService.getUserWithAttributes(directoryUser.getName());
        return Boolean.valueOf(userWithAttributes.getValue(SERVICE_DESK_CUSTOMERS_ATTRIBUTE_KEY));
    }

    private void invite(ApplicationUser user)
    {
        DvcsCommunicator dvcsCommunicator = dvcsCommunicatorProvider.getCommunicator(DVCS_TYPE_BITBUCKET);
        for (Organization bitbucketTeam : bitbucketTeamService.getTeamsWithDefaultGroups())
        {
            Collection<String> groups = groupNames(bitbucketTeam.getDefaultGroups());
            LOGGER.debug("Inviting user {} to groups {} in organization {}",
                    new Object[] {user.getUsername(), groups, bitbucketTeam.getName()});

            dvcsCommunicator.inviteUser(bitbucketTeam, groups, user.getEmailAddress());
        }
    }

    private Collection<String> groupNames(Set<Group> groups)
    {
        List<String> groupNames = Lists.newArrayList();
        for (Group group : groups)
        {
            groupNames.add(group.getSlug());
        }

        return groupNames;
    }
}
