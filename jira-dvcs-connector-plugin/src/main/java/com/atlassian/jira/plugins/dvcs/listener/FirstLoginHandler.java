package com.atlassian.jira.plugins.dvcs.listener;

import com.atlassian.crowd.embedded.api.CrowdService;
import com.atlassian.crowd.embedded.api.UserWithAttributes;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.ApplicationUsers;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.atlassian.jira.plugins.dvcs.listener.UserAddedEventListener.UI_USER_INVITATIONS_PARAM_NAME;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Listens for users' first login and determines the best way to get them invited to Bitbucket. If the user was provisioned
 * via the create user screen, we can read their Crowd attributes to work out what teams they should be invited to.
 *
 * If the user was provisioned by other means, we can look up Bitbucket teams that are configured with default groups.
 */
@Component
public class FirstLoginHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(FirstLoginHandler.class);

    private final CrowdService crowdService;

    private final UserAddedExternallyEventProcessor userAddedExternallyEventProcessor;

    private final UserAddedViaInterfaceEventProcessor userAddedViaInterfaceEventProcessor;

    @Autowired
    public FirstLoginHandler(@ComponentImport CrowdService crowdService,
            UserAddedExternallyEventProcessor userAddedExternallyEventProcessor,
            UserAddedViaInterfaceEventProcessor userAddedViaInterfaceEventProcessor)
    {
        this.crowdService = checkNotNull(crowdService);
        this.userAddedExternallyEventProcessor = checkNotNull(userAddedExternallyEventProcessor);
        this.userAddedViaInterfaceEventProcessor = checkNotNull(userAddedViaInterfaceEventProcessor);
    }

    void onFirstLogin(final String username)
    {
        checkArgument(username != null && !username.trim().isEmpty(),
                "Expecting username to be non-null and non-blank but received '" + username +"'");

        UserWithAttributes userWithAttributes = crowdService.getUserWithAttributes(username);
        String uiSelection = userWithAttributes.getValue(UI_USER_INVITATIONS_PARAM_NAME);
        LOGGER.debug("User {} logged in for the first time and has Bitbucket teams UI selection value of " + uiSelection);

        ApplicationUser applicationUser = ApplicationUsers.from(userWithAttributes);
        if (uiSelection == null)
        {
            userAddedExternallyEventProcessor.process(applicationUser);
        }
        else
        {
            userAddedViaInterfaceEventProcessor.process(applicationUser, uiSelection);
        }
    }
}
