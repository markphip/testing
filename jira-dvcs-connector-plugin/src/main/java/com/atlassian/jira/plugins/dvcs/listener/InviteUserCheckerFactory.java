package com.atlassian.jira.plugins.dvcs.listener;

import com.atlassian.crowd.model.user.User;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.ApplicationUsers;
import com.atlassian.jira.user.util.UserManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class InviteUserCheckerFactory
{
    @Autowired
    OrganizationService organizationService;
    @Autowired
    UserManager userManager;
    @Autowired
    DvcsCommunicatorProvider dvcsCommunicatorProvider;
    @Autowired
    GroupManager groupManager;

    public UserInviteChecker createInviteUserChecker(User user, String uiChoice)
    {
        if (uiChoice == null)
        {
            return new UserAddedExternallyEventProcessor(user.getName(), organizationService, dvcsCommunicatorProvider, userManager,
                    groupManager);
        }
        return new UserAddedViaInterfaceEventProcessor(uiChoice, ApplicationUsers.from(user), organizationService,
                dvcsCommunicatorProvider, userManager, groupManager);
    }
}
