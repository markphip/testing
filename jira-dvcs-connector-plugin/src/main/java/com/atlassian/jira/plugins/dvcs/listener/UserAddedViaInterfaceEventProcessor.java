package com.atlassian.jira.plugins.dvcs.listener;

import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.jira.user.ApplicationUser;
import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.atlassian.jira.plugins.dvcs.listener.UserAddedEventListener.ORG_ID_GROUP_PAIR_SEPARATOR;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static java.lang.Integer.parseInt;
import static java.util.Map.Entry;

/**
 * Invites users that were created via the 'Create user' screen to Bitbucket teams. Selections made by the administrator
 * who provisioned this user are used to determine the list of teams and groups that users will get invited to.
 *
 * Current default teams have no impact on teams and groups that users will get invited to.
 */
@Component
public class UserAddedViaInterfaceEventProcessor
{
    private static final Logger LOGGER = LoggerFactory.getLogger(UserAddedViaInterfaceEventProcessor.class);

    @VisibleForTesting
    static final String DVCS_TYPE_BITBUCKET = "bitbucket";

	private static final String ORG_ID_GROUP_DELIMITER = ":";

    private final DvcsCommunicatorProvider dvcsCommunicatorProvider;

	private final OrganizationService organizationService;

    @Autowired
	public UserAddedViaInterfaceEventProcessor(DvcsCommunicatorProvider dvcsCommunicatorProvider,
            OrganizationService organizationService)
	{
        this.dvcsCommunicatorProvider = checkNotNull(dvcsCommunicatorProvider);
		this.organizationService = checkNotNull(organizationService);
	}

	public void process(ApplicationUser user, String serializedUISelection)
	{
        checkArgument(user != null, "Expecting a non-null user");
        checkArgument(user.getEmailAddress() != null, "Expecting a non-null email address for the user");
        checkArgument(serializedUISelection != null, "Expecting a non-null serialized UI selection");

		if (serializedUISelection.trim().isEmpty())
		{
			return;
		}

		Map<Integer,List<String>> groupsByOrganizationId = parseSerializedUISelection(serializedUISelection);
        inviteUserToOrganizations(user, groupsByOrganizationId);
	}

    private Map<Integer, List<String>> parseSerializedUISelection(String serializedUISelection)
    {
        Map<Integer, List<String>> groupsByOrganizationId = newHashMap();

        for (String orgIdGroupPairStr : serializedUISelection.split(ORG_ID_GROUP_PAIR_SEPARATOR))
        {
            String[] orgIdGroupPair = orgIdGroupPairStr.split(ORG_ID_GROUP_DELIMITER);
            int orgId = parseInt(orgIdGroupPair[0]);
            String group = orgIdGroupPair[1];

            List<String> groupsForOrgId = groupsByOrganizationId.get(orgId);
            if (groupsForOrgId == null)
            {
                groupsForOrgId = newArrayList();
                groupsByOrganizationId.put(orgId, groupsForOrgId);
            }

            groupsForOrgId.add(group);
        }

        return groupsByOrganizationId;
    }

    private void inviteUserToOrganizations(ApplicationUser user, Map<Integer, List<String>> groupsByOrganizationId)
    {
        DvcsCommunicator dvcsCommunicator = dvcsCommunicatorProvider.getCommunicator(DVCS_TYPE_BITBUCKET);
        for (Entry<Integer, List<String>> entry : groupsByOrganizationId.entrySet())
        {
            Integer orgId = entry.getKey();
            List<String> groups = entry.getValue();

            Organization organization = organizationService.get(orgId, false);
            if (organization == null)
            {
                LOGGER.warn("Skipped inviting user {} to groups {} in organization with id {} because such organization does not exist",
                        new String[] {user.getUsername(), groups.toString(), orgId.toString()});
                continue;
            }

            checkState(organization.getDvcsType().equals(DVCS_TYPE_BITBUCKET), "Expecting Bitbucket organizations only");
            LOGGER.debug("Inviting user {} to groups {} in organization {}",
                    new String[] {user.getUsername(), groups.toString(), organization.getName()});
            dvcsCommunicator.inviteUser(organization, groups, user.getEmailAddress());
        }
    }
}
