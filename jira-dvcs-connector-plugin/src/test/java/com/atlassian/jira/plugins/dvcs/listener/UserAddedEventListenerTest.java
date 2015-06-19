package com.atlassian.jira.plugins.dvcs.listener;

import com.atlassian.crowd.embedded.api.CrowdService;
import com.atlassian.crowd.exception.OperationNotPermittedException;
import com.atlassian.crowd.model.user.User;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.event.web.action.admin.UserAddedEvent;
import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.google.common.collect.Maps;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Map;

import static com.atlassian.jira.plugins.dvcs.listener.UserAddedEventListener.REQUEST_KEY_DVCS_ORG_SELECTOR;
import static com.atlassian.jira.plugins.dvcs.listener.UserAddedEventListener.REQUEST_KEY_USERNAME;
import static com.atlassian.jira.plugins.dvcs.listener.UserAddedEventListener.UI_USER_INVITATIONS_PARAM_NAME;
import static java.util.Collections.singleton;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@Listeners(MockitoTestNgListener.class)
public class UserAddedEventListenerTest
{
    private static final String USERNAME = "charlie";

    @Mock
    private ApplicationUser applicationUser;

    @Mock
    private CrowdService crowdService;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private User user;

    @InjectMocks
    private UserAddedEventListener userAddedEventListener;

    @Mock
    private UserManager userManager;

    @BeforeMethod
    public void prepare()
    {
        when(userManager.getUserByName(USERNAME)).thenReturn(applicationUser);
        when(applicationUser.getDirectoryUser()).thenReturn(user);
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionWhenWeGetANullEvent()
    {
        userAddedEventListener.onUserAddedViaCreateUserScreen(null);
    }

    @Test
    public void shouldSetUserAttributeValueToEmptyStringWhenNoBitbucketGroupIsSelected() throws OperationNotPermittedException
    {
        userAddedEventListener.onUserAddedViaCreateUserScreen(userAddedEvent());

        verify(crowdService).setUserAttribute(user, UI_USER_INVITATIONS_PARAM_NAME, singleton(" "));
    }

    @Test
    public void shouldSetUserAttributeValueWhenOneBitbucketGroupIsSelected() throws OperationNotPermittedException
    {
        userAddedEventListener.onUserAddedViaCreateUserScreen(userAddedEvent("1:developers"));

        verify(crowdService).setUserAttribute(user, UI_USER_INVITATIONS_PARAM_NAME, singleton("1:developers"));
    }

    @Test
    public void shouldSetUserAttributeValueWhenMultipleBitbucketGroupsAreSelected() throws OperationNotPermittedException
    {
        userAddedEventListener.onUserAddedViaCreateUserScreen(userAddedEvent("1:developers", "2:administrators"));

        verify(crowdService).setUserAttribute(user, UI_USER_INVITATIONS_PARAM_NAME, singleton("1:developers;2:administrators"));
    }

    @Test
    public void shouldSetUserAttributeValueWhenMultipleBitbucketGroupsAreSelectedViaRenaissanceUI() throws OperationNotPermittedException
    {
        userAddedEventListener.onUserAddedViaCreateUserScreen(userAddedEvent("1:developers;2:administrators"));

        verify(crowdService).setUserAttribute(user, UI_USER_INVITATIONS_PARAM_NAME, singleton("1:developers;2:administrators"));
    }

    @Test
    public void shouldRegisterSelfToEventPublisherAfterListenerIsCreated() throws Exception
    {
        userAddedEventListener.afterPropertiesSet();

        verify(eventPublisher).register(userAddedEventListener);
    }

    @Test
    public void shouldDeregisterSelfFromEventPublisherBeforeListenerIsDestroyed() throws Exception
    {
        userAddedEventListener.destroy();

        verify(eventPublisher).unregister(userAddedEventListener);
    }

    private UserAddedEvent userAddedEvent(String ... orgIdAndGroupPairs)
    {
        Map<String,String[]> requestParameters = Maps.newHashMap();
        requestParameters.put(REQUEST_KEY_USERNAME, new String[] { USERNAME });
        if(orgIdAndGroupPairs.length != 0)
        {
            requestParameters.put(REQUEST_KEY_DVCS_ORG_SELECTOR, orgIdAndGroupPairs);
        }

        return new UserAddedEvent(requestParameters);
    }
}