package com.atlassian.jira.plugins.dvcs.listener;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.util.UserManager;
import com.google.common.base.Splitter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link Runnable} processor that handles logic beside invitations for user added to JIRA via user interface.
 * <p/>
 * <p/>
 * <br /><br /> Created on 21.6.2012, 15:32:33 <br /><br />
 *
 * @author jhocman@atlassian.com
 */
public class UserAddedViaInterfaceEventProcessor extends UserInviteCommonEventProcessor implements Runnable
{
    private static final Logger log = LoggerFactory.getLogger(UserAddedViaInterfaceEventProcessor.class);

    public static String ORGANIZATION_SELECTOR_REQUEST_PARAM = "dvcs_org_selector";

    public static String ORGANIZATION_SELECTOR_REQUEST_PARAM_JOINER = ";";

    public static String EMAIL_PARAM = "email";

    /**
     * The Constant SPLITTER.
     */
    private static final String SPLITTER = ":";

    /**
     * The organization service.
     */
    private final OrganizationService organizationService;

    /**
     * The communicator provider.
     */
    private final DvcsCommunicatorProvider communicatorProvider;

    private final String serializedGroupsUiChoice;

    private final User user;

    /**
     * Instantiates a new user added via interface event processor.
     *
     * @param event the event
     * @param organizationService the organization service
     * @param communicatorProvider the communicator provider
     */
    public UserAddedViaInterfaceEventProcessor(String serializedGroupsUiChoice, User user, OrganizationService organizationService,
            DvcsCommunicatorProvider communicatorProvider, UserManager userManager, GroupManager groupManager)
    {
        super(userManager, groupManager);

        this.serializedGroupsUiChoice = serializedGroupsUiChoice;
        this.user = user;

        this.organizationService = organizationService;
        this.communicatorProvider = communicatorProvider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run()
    {
        // continue ? ------------------------------------------------
        if (StringUtils.isBlank(serializedGroupsUiChoice))
        {
            return;
        }
        // ------------------------------------------------------------

        Collection<Invitations> invitationsFor = convertInvitations();
        String email = user.getEmailAddress();

        // log invite
        logInvite(user, invitationsFor);

        // invite
        invite(email, invitationsFor);

    }

    /**
     * To invitations.
     *
     * @param organizationIdsAndGroupSlugs the organization ids and group slugs
     * @return the collection
     */
    private Collection<Invitations> convertInvitations()
    {

        Map<Integer, Invitations> orgIdsToInvitations = new HashMap<Integer, Invitations>();

        Iterable<String> organizationIdsAndGroupSlugs = Splitter.on(ORGANIZATION_SELECTOR_REQUEST_PARAM_JOINER).split(serializedGroupsUiChoice);

        for (String requestParamToken : organizationIdsAndGroupSlugs)
        {

            String[] tokens = requestParamToken.split(SPLITTER);
            Integer orgId = Integer.parseInt(tokens[0]);
            String slug = tokens[1];
            Invitations existingInvitations = orgIdsToInvitations.get(orgId);

            //
            // first time organization ?
            if (existingInvitations == null)
            {
                Invitations newInvitations = new Invitations();
                newInvitations.organizaton = organizationService.get(orgId, false);

                if (newInvitations.organizaton == null)
                {
                    continue;
                }

                orgIdsToInvitations.put(orgId, newInvitations);
                existingInvitations = newInvitations;
            }
            //
            existingInvitations.groupSlugs.add(slug);
        }

        return orgIdsToInvitations.values();
    }

    /**
     * Invite.
     *
     * @param email the email
     * @param invitations the invitations
     */
    private void invite(String email, Collection<Invitations> invitations)
    {
        if (CollectionUtils.isNotEmpty(invitations))
        {
            for (Invitations invitation : invitations)
            {
                Collection<String> groupSlugs = invitation.groupSlugs;
                Organization organizaton = invitation.organizaton;
                invite(email, organizaton, groupSlugs);
            }
        }
    }

    /**
     * Invite.
     *
     * @param email the email
     * @param organization the organization
     * @param groupSlugs the group slugs
     */
    private void invite(String email, Organization organization, Collection<String> groupSlugs)
    {
        if (CollectionUtils.isNotEmpty(groupSlugs))
        {
            DvcsCommunicator communicator = communicatorProvider.getCommunicatorAndCheckSyncDisabled(organization.getDvcsType());
            if (!communicator.isSyncDisabled())
            {
                communicator.inviteUser(organization, groupSlugs, email);
            }
            else
            {
                log.warn("User cannot be invited to {}. Sync is disabled.", organization.getName());
            }
        }
    }

    /**
     * The Class Invitations.
     */
    static class Invitations
    {
        /**
         * The organizaton.
         */
        Organization organizaton;
        /**
         * The group slugs.
         */
        Collection<String> groupSlugs = new ArrayList<String>();

        @Override
        public String toString()
        {
            return organizaton.getName() + " : " + groupSlugs + "\n";
        }
    }
}
