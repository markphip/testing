package com.atlassian.jira.plugins.dvcs.listener;

import com.atlassian.crowd.embedded.api.CrowdService;
import com.atlassian.crowd.embedded.api.UserWithAttributes;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static com.atlassian.jira.plugins.dvcs.listener.UserAddedEventListener.UI_USER_INVITATIONS_PARAM_NAME;
import static com.google.common.base.Preconditions.checkNotNull;

@Component
public class FirstLoginHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(FirstLoginHandler.class);

    /** BBC-957: Attribute key to recognise Service Desk Customers during user creation */
    private static final String SERVICE_DESK_CUSTOMERS_ATTRIBUTE_KEY = "synch.servicedesk.requestor";

    private final CrowdService crowdService;

    private final UserAddedExternallyEventProcessor userAddedExternallyEventProcessor;

    private final UserAddedViaInterfaceEventProcessor userAddedViaInterfaceEventProcessor;

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
        UserWithAttributes attributes = crowdService.getUserWithAttributes(username);

        String uiChoice = attributes.getValue(UI_USER_INVITATIONS_PARAM_NAME);
        LOGGER.debug("UI choice for user " + username + " : " + uiChoice);


        // BBC-957: ignore Service Desk Customers when processing the event.
        boolean isServiceDeskRequestor = Boolean.toString(true).equals(attributes.getValue(SERVICE_DESK_CUSTOMERS_ATTRIBUTE_KEY));

        if(!isServiceDeskRequestor)
        {
            if (uiChoice == null)
            {
                // created by NON UI mechanism, e.g. google user
                userAddedExternallyEventProcessor.run(); //TODO: This is not right...needs to pass param to run()

            }
            else if (StringUtils.isNotBlank(uiChoice)) /* something has been chosen from UI */
            {
                userAddedViaInterfaceEventProcessor.run(); //TODO: This is not right...needs to pass param to run()
            }
        }
    }
}
