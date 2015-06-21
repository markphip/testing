package com.atlassian.jira.plugins.dvcs.listener;

import com.atlassian.crowd.event.user.UserAttributeStoredEvent;
import com.atlassian.crowd.model.user.User;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static com.atlassian.jira.plugins.dvcs.listener.UserAttributeStoredEventListener.USER_ATTRIBUTE_KEY_LOGIN_COUNT;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.emptySet;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.argThat;
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
        when(userAttributeStoredEvent.getAttributeValues(USER_ATTRIBUTE_KEY_LOGIN_COUNT)).thenReturn(emptySet());

        userAttributeStoredEventListener.onUserAttributeStore(userAttributeStoredEvent);

        verifyZeroInteractions(firstLoginHandler);
    }

    @Test
    public void shouldDoNothingWhenLoginCountIsNotASingleValue()
    {
        when(userAttributeStoredEvent.getAttributeValues(USER_ATTRIBUTE_KEY_LOGIN_COUNT)).thenReturn(newHashSet("1", "2"));

        userAttributeStoredEventListener.onUserAttributeStore(userAttributeStoredEvent);

        verifyZeroInteractions(firstLoginHandler);
    }

    @Test
    public void shouldDoNothingWhenLoginCountIsNotANumber()
    {
        when(userAttributeStoredEvent.getAttributeValues(USER_ATTRIBUTE_KEY_LOGIN_COUNT)).thenReturn(newHashSet("one"));

        userAttributeStoredEventListener.onUserAttributeStore(userAttributeStoredEvent);

        verifyZeroInteractions(firstLoginHandler);
    }

    @Test
    public void shouldDoNothingWhenLoginCountIsGreaterThanOne()
    {
        when(userAttributeStoredEvent.getAttributeValues(USER_ATTRIBUTE_KEY_LOGIN_COUNT)).thenReturn(newHashSet("2"));

        userAttributeStoredEventListener.onUserAttributeStore(userAttributeStoredEvent);

        verifyZeroInteractions(firstLoginHandler);
    }

    @Test
    public void shouldInvokeFirstLoginHandlerWhenLoginCountIsOne()
    {
        when(userAttributeStoredEvent.getAttributeValues(USER_ATTRIBUTE_KEY_LOGIN_COUNT)).thenReturn(newHashSet("1"));

        userAttributeStoredEventListener.onUserAttributeStore(userAttributeStoredEvent);

        verify(firstLoginHandler).onFirstLogin(argThat(equalTo(USERNAME)));
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
}
