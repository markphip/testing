package com.atlassian.jira.plugins.dvcs.listener;

import com.atlassian.crowd.embedded.api.CrowdService;
import com.atlassian.crowd.exception.OperationNotPermittedException;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.event.web.action.admin.UserAddedEvent;
import com.atlassian.jira.plugins.dvcs.analytics.DvcsAddUserAnalyticsEvent;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import static com.atlassian.jira.user.ApplicationUsers.toDirectoryUser;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singleton;

/**
 * Listens for {@link com.atlassian.jira.event.web.action.admin.UserAddedEvent} and checks the request parameter to see
 * whether the creator has provisioned a Bitbucket access for this user.
 *
 * Regardless of whether a Bitbucket access was provisioned, write an entry into the user's attributes. In the event
 * where Bitbucket access was provisioned, this entry will contain information about Bitbucket teams that this user
 * can access.
 *
 * The entry mentioned above will be read when the user logs in for the first time and will be used to call Bitbucket so it
 * invites the user to the correct team(s).
 */
@Component
public class UserAddedEventListener implements InitializingBean, DisposableBean
{
    private static final Logger LOGGER = LoggerFactory.getLogger(UserAddedEventListener.class);

    private static final String ORG_ID_GROUP_PAIR_SEPARATOR = ";";

    @VisibleForTesting
    static final String REQUEST_KEY_USERNAME = "username";

    @VisibleForTesting
    static final String REQUEST_KEY_DVCS_ORG_SELECTOR = "dvcs_org_selector";

    @VisibleForTesting
    static final String UI_USER_INVITATIONS_PARAM_NAME = "com.atlassian.jira.dvcs.invite.groups";

    private final CrowdService crowdService;

    private final EventPublisher eventPublisher;

    private final UserManager userManager;

    public UserAddedEventListener(@ComponentImport CrowdService crowdService,
            @ComponentImport EventPublisher eventPublisher, @ComponentImport UserManager userManager)
    {
        this.crowdService = checkNotNull(crowdService);
        this.eventPublisher = checkNotNull(eventPublisher);
        this.userManager = checkNotNull(userManager);
    }

    @EventListener
    public void onUserAddedViaCreateUserScreen(final UserAddedEvent event)
    {
        checkArgument(event != null, "Expecting UserAddedEvent to be non-null");
        LOGGER.debug("onUserAddViaInterface - processing event with payload: " + event.getRequestParameters());

        ApplicationUser user = getApplicationUser(event);
        String userInvitationAttribute = getUserInvitationAttribute(event);
        try
        {
            crowdService.setUserAttribute(toDirectoryUser(user),
                    UI_USER_INVITATIONS_PARAM_NAME, singleton(userInvitationAttribute));

        }
        catch (OperationNotPermittedException ex)
        {
            throw new RuntimeException("Failed to process event with payload: " + event.getRequestParameters(), ex);
        }
    }

    @Override
    public void destroy() throws Exception
    {
        eventPublisher.unregister(this);
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        eventPublisher.register(this);
    }

    private ApplicationUser getApplicationUser(UserAddedEvent event)
    {
        String username = event.getRequestParameters().get(REQUEST_KEY_USERNAME)[0];
        return userManager.getUserByName(username);
    }

    private String getUserInvitationAttribute(UserAddedEvent event)
    {
        //Use the following blank String to indicate that the user is created via the UI but the creator does not provision
        //this user with any Bitbucket access. If we use empty string, Crowd will return null when we try to query it back.
        //Null indicates that the user was not created via the UI.
        String userInvitationAttribute = " ";

        String[] organizationIdsAndGroupSlugs = event.getRequestParameters().get(REQUEST_KEY_DVCS_ORG_SELECTOR);
        if (organizationIdsAndGroupSlugs != null)
        {
            userInvitationAttribute = Joiner.on(ORG_ID_GROUP_PAIR_SEPARATOR).join(organizationIdsAndGroupSlugs);
            eventPublisher.publish(new DvcsAddUserAnalyticsEvent());
        }

        return userInvitationAttribute;
    }
}
