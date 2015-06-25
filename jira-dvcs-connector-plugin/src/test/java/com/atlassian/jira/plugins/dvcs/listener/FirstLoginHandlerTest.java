package com.atlassian.jira.plugins.dvcs.listener;

import com.atlassian.crowd.embedded.api.CrowdService;
import com.atlassian.crowd.embedded.api.UserWithAttributes;
import com.atlassian.jira.mock.component.MockComponentWorker;
import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.MockUserManager;
import com.atlassian.jira.user.util.UserManager;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static com.atlassian.jira.plugins.dvcs.listener.UserAddedEventListener.UI_USER_INVITATIONS_PARAM_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Listeners (MockitoTestNgListener.class)
public class FirstLoginHandlerTest
{
    private static final String USERNAME = "jsmith";

    @Mock
    private CrowdService crowdService;

    @InjectMocks
    private FirstLoginHandler firstLoginHandler;

    @Mock
    private UserAddedExternallyEventProcessor userAddedExternallyEventProcessor;

    @Mock
    private UserAddedViaInterfaceEventProcessor userAddedViaInterfaceEventProcessor;

    @Mock
    private UserWithAttributes userWithAttributes;

    @BeforeMethod
    public void prepare()
    {
        when(crowdService.getUserWithAttributes(USERNAME)).thenReturn(userWithAttributes);
        when(userWithAttributes.getName()).thenReturn(USERNAME);

        new MockComponentWorker()
                .addMock(UserManager.class, new MockUserManager())
                .init();
        when(userWithAttributes.getDirectoryId()).thenReturn(-1L);
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionWhenUsernameIsNull()
    {
        firstLoginHandler.onFirstLogin(null);
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionWhenUsernameIsEmpty()
    {
        firstLoginHandler.onFirstLogin(" ");
    }

    @Test
    public void shouldDelegateToUserAddedExternallyEventProcessorWhenUserAttributesDoNotContainUISelection()
    {
        firstLoginHandler.onFirstLogin(USERNAME);

        ArgumentCaptor<ApplicationUser> applicationUserArgumentCaptor = forClass(ApplicationUser.class);
        verify(userAddedExternallyEventProcessor).process(applicationUserArgumentCaptor.capture());

        ApplicationUser applicationUser = applicationUserArgumentCaptor.getValue();
        assertThat(applicationUser.getDirectoryUser(), is(userWithAttributes));
    }

    @Test
    public void shouldDelegateToUserAddedViaInterfaceEventProcessorWhenUserAttributesContainUISelection()
    {
        String uiSelectionAttributeValue = "1:developers;2:administrators";
        when(userWithAttributes.getValue(UI_USER_INVITATIONS_PARAM_NAME)).thenReturn(uiSelectionAttributeValue);

        firstLoginHandler.onFirstLogin(USERNAME);

        ArgumentCaptor<ApplicationUser> applicationUserArgumentCaptor = forClass(ApplicationUser.class);
        verify(userAddedViaInterfaceEventProcessor).process(applicationUserArgumentCaptor.capture(), argThat(equalTo(uiSelectionAttributeValue)));

        ApplicationUser applicationUser = applicationUserArgumentCaptor.getValue();
        assertThat(applicationUser.getDirectoryUser(), is(userWithAttributes));
    }

    @Test
    public void shouldDelegateToUserAddedViaInterfaceEventProcessorWhenUserAttributesContainBlankUISelection()
    {
        String uiSelectionAttributeValue = " ";
        when(userWithAttributes.getValue(UI_USER_INVITATIONS_PARAM_NAME)).thenReturn(uiSelectionAttributeValue);

        firstLoginHandler.onFirstLogin(USERNAME);

        ArgumentCaptor<ApplicationUser> applicationUserArgumentCaptor = forClass(ApplicationUser.class);
        verify(userAddedViaInterfaceEventProcessor).process(applicationUserArgumentCaptor.capture(), argThat(equalTo(uiSelectionAttributeValue)));

        ApplicationUser applicationUser = applicationUserArgumentCaptor.getValue();
        assertThat(applicationUser.getDirectoryUser(), is(userWithAttributes));
    }
}
