package com.atlassian.jira.plugins.dvcs.listener;

import com.atlassian.crowd.embedded.api.CrowdService;
import com.atlassian.crowd.embedded.api.UserWithAttributes;
import com.atlassian.crowd.event.user.UserCreatedEvent;
import com.atlassian.crowd.model.user.User;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.dvcs.analytics.AnalyticsService;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.util.UserManager;
import org.junit.Before;
import org.junit.Rule;

import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;


import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class DvcsAddUserListenerTest
{
    @Mock
    EventPublisher eventPublisher;
    @Mock
    OrganizationService organizationService;
    @Mock
    DvcsCommunicatorProvider communicatiorProvider;
    @Mock
    UserManager userManager;
    @Mock
    GroupManager groupManager;
    @Mock
    CrowdService crowd;
    @Mock
    AnalyticsService analyticsService;
    @Mock
    UserCreatedEvent userCreatedEvent;
    @Mock
    User user;
    @Mock
    UserWithAttributes userWithAttributes;
    @Mock
    InviteUserCheckerFactory inviteUserCheckerFactory;
    @Mock
    UserInviteChecker userInviteChecker;

    private final String username = "TestUsername";
    private DvcsAddUserListener classUnderTest;
    private static final String UI_USER_INVITATIONS_PARAM_NAME = "com.atlassian.jira.dvcs.invite.groups";
    private static final String SERVICE_DESK_CUSTOMERS_ATTRIBUTE_KEY = "synch.servicedesk.requestor";

    @Rule
    public final MethodRule initMockito = MockitoJUnit.rule();

    @Before
    public void setUp()
    {
        when(userCreatedEvent.getUser()).thenReturn(user);
        when(user.getName()).thenReturn(username);
        when(crowd.getUserWithAttributes(username)).thenReturn(userWithAttributes);

        classUnderTest = new DvcsAddUserListener(eventPublisher, organizationService, communicatiorProvider, userManager, groupManager, crowd, analyticsService, inviteUserCheckerFactory);
    }

    @Test
    public void fireAnalyticsWhenUserWithInvitesIsCreated()
    {
        when(userWithAttributes.getValue(UI_USER_INVITATIONS_PARAM_NAME)).thenReturn("uiChoice");
        when(userWithAttributes.getValue(SERVICE_DESK_CUSTOMERS_ATTRIBUTE_KEY)).thenReturn("false");
        when(inviteUserCheckerFactory.createInviteUserChecker(any(User.class), any(String.class))).thenReturn(userInviteChecker);
        when(userInviteChecker.willReceiveGroupInvite()).thenReturn(true);

        classUnderTest.fireAnalyticsWhenUserWithInvitesIsCreated(userCreatedEvent);

        verify(analyticsService).publishUserCreatedThatHasInvite();
        verifyNoMoreInteractions(analyticsService);

    }

    @Test
    public void noAnalyticsFireWhenServiceDeskUserIsCreated()
    {
        when(userWithAttributes.getValue(UI_USER_INVITATIONS_PARAM_NAME)).thenReturn("uiChoice");
        when(userWithAttributes.getValue(SERVICE_DESK_CUSTOMERS_ATTRIBUTE_KEY)).thenReturn("true");
        when(inviteUserCheckerFactory.createInviteUserChecker(any(User.class), any(String.class))).thenReturn(userInviteChecker);
        when(userInviteChecker.willReceiveGroupInvite()).thenReturn(false);

        classUnderTest.fireAnalyticsWhenUserWithInvitesIsCreated(userCreatedEvent);

        verifyZeroInteractions(analyticsService);
    }

    @Test
    public void noAnalyticsFireWhenUserWithoutInvitesIsCreated()
    {
        when(userWithAttributes.getValue(UI_USER_INVITATIONS_PARAM_NAME)).thenReturn("uiChoice");
        when(userWithAttributes.getValue(SERVICE_DESK_CUSTOMERS_ATTRIBUTE_KEY)).thenReturn("false");
        when(inviteUserCheckerFactory.createInviteUserChecker(any(User.class), any(String.class))).thenReturn(userInviteChecker);
        when(userInviteChecker.willReceiveGroupInvite()).thenReturn(false);

        classUnderTest.fireAnalyticsWhenUserWithInvitesIsCreated(userCreatedEvent);

        verifyZeroInteractions(analyticsService);
    }
}
