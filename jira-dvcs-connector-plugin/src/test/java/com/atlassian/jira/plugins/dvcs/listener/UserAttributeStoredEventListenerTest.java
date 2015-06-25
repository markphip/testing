package com.atlassian.jira.plugins.dvcs.listener;

import com.atlassian.crowd.event.user.UserAttributeStoredEvent;
import com.atlassian.crowd.model.user.User;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import com.atlassian.jira.software.api.roles.LicenseService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.Set;

import static com.atlassian.jira.plugins.dvcs.listener.UserAttributeStoredEventListener.USER_ATTRIBUTE_KEY_LOGIN_COUNT;
import static com.google.common.collect.ImmutableMap.of;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.emptySet;
import static java.util.Map.Entry;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@Listeners (MockitoTestNgListener.class)
public class UserAttributeStoredEventListenerTest
{
    private static final String USERNAME = "charlie";

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private FirstLoginHandler firstLoginHandler;

    @Mock
    private LicenseService licenseService;

    @Mock
    private User user;

    @Mock
    private UserAttributeStoredEvent userAttributeStoredEvent;

    @InjectMocks
    private UserAttributeStoredEventListener userAttributeStoredEventListener;

    @BeforeMethod
    public void prepare()
    {
        when(user.getName()).thenReturn(USERNAME);
        when(userAttributeStoredEvent.getUser()).thenReturn(user);
        when(licenseService.hasActiveSoftwareLicense()).thenReturn(true);
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldThrowIllegalStateExceptionWhenEventIsNull()
    {
        userAttributeStoredEventListener.onUserAttributeStore(null);
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldThrowIllegalStateExceptionWhenEventContainsANullUser()
    {
        when(userAttributeStoredEvent.getUser()).thenReturn(null);

        userAttributeStoredEventListener.onUserAttributeStore(userAttributeStoredEvent);
    }

    @Test
    public void shouldDoNothingWhenLoginCountAttributeDoesNotExist()
    {
        userAttributeStoredEventListener.onUserAttributeStore(userAttributeStoredEvent);

        verifyZeroInteractions(firstLoginHandler);
    }

    @Test
    public void shouldDoNothingWhenLoginCountAttributeValuesIsEmpty()
    {
        givenUserAttributeStoredEventWithAttributes(of(USER_ATTRIBUTE_KEY_LOGIN_COUNT, emptySet()));

        userAttributeStoredEventListener.onUserAttributeStore(userAttributeStoredEvent);

        verifyZeroInteractions(firstLoginHandler);
    }

    @Test
    public void shouldDoNothingWhenLoginCountIsNotASingleValue()
    {
        givenUserAttributeStoredEventWithAttributes(of(USER_ATTRIBUTE_KEY_LOGIN_COUNT, newHashSet("1", "2")));

        userAttributeStoredEventListener.onUserAttributeStore(userAttributeStoredEvent);

        verifyZeroInteractions(firstLoginHandler);
    }

    @Test
    public void shouldDoNothingWhenLoginCountIsNotANumber()
    {
        givenUserAttributeStoredEventWithAttributes(of(USER_ATTRIBUTE_KEY_LOGIN_COUNT, newHashSet("one")));

        userAttributeStoredEventListener.onUserAttributeStore(userAttributeStoredEvent);

        verifyZeroInteractions(firstLoginHandler);
    }

    @Test
    public void shouldDoNothingWhenLoginCountIsGreaterThanOne()
    {
        givenUserAttributeStoredEventWithAttributes(of(USER_ATTRIBUTE_KEY_LOGIN_COUNT, newHashSet("2")));

        userAttributeStoredEventListener.onUserAttributeStore(userAttributeStoredEvent);

        verifyZeroInteractions(firstLoginHandler);
    }

    @Test
    public void shouldInvokeFirstLoginHandlerWhenLoginCountIsOne()
    {
        givenUserAttributeStoredEventWithAttributes(of(USER_ATTRIBUTE_KEY_LOGIN_COUNT, newHashSet("1")));

        userAttributeStoredEventListener.onUserAttributeStore(userAttributeStoredEvent);

        verify(firstLoginHandler).onFirstLogin(argThat(equalTo(USERNAME)));
    }

    @Test
    public void shouldDoNothingWhenLoginCountIsOneAndButHasNoActiveSoftwareLicense()
    {
        givenUserAttributeStoredEventWithAttributes(of(USER_ATTRIBUTE_KEY_LOGIN_COUNT, newHashSet("1")));
        when(licenseService.hasActiveSoftwareLicense()).thenReturn(false);

        userAttributeStoredEventListener.onUserAttributeStore(userAttributeStoredEvent);

        verifyZeroInteractions(firstLoginHandler);
    }

    @Test
    public void shouldRegisterSelfToEventPublisherAfterListenerIsCreated() throws Exception
    {
        userAttributeStoredEventListener.afterPropertiesSet();

        verify(eventPublisher).register(userAttributeStoredEventListener);
    }

    @Test
    public void shouldDeregisterSelfFromEventPublisherBeforeListenerIsDestroyed() throws Exception
    {
        userAttributeStoredEventListener.destroy();

        verify(eventPublisher).unregister(userAttributeStoredEventListener);
    }

    private void givenUserAttributeStoredEventWithAttributes(Map<String, Set<String>> attributes)
    {
        when(userAttributeStoredEvent.getAttributeNames()).thenReturn(attributes.keySet());
        for (Entry<String, Set<String>> entry : attributes.entrySet())
        {
            when(userAttributeStoredEvent.getAttributeValues(entry.getKey())).thenReturn(entry.getValue());
        }
    }
}
