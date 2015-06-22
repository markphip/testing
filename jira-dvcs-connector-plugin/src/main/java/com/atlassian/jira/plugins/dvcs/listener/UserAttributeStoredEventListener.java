package com.atlassian.jira.plugins.dvcs.listener;

import com.atlassian.crowd.event.user.UserAttributeStoredEvent;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.fugue.Option;
import com.google.common.annotations.VisibleForTesting;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

import static com.atlassian.fugue.Option.none;
import static com.atlassian.fugue.Option.some;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.getOnlyElement;
import static java.lang.Integer.parseInt;

/**
 * Listens for {@link com.atlassian.crowd.event.user.UserAttributeStoredEvent} and checks
 * whether the attribute being updated is the login count attribute. If this is the case,
 * infers whether this is the user's first login and if so invoke
 * {@link com.atlassian.jira.plugins.dvcs.listener.FirstLoginHandler#onFirstLogin(String)}.
 */
@Component
public class UserAttributeStoredEventListener implements InitializingBean, DisposableBean
{
    @VisibleForTesting
    static final String USER_ATTRIBUTE_KEY_LOGIN_COUNT = "login.count";

    private final EventPublisher eventPublisher;

    private final FirstLoginHandler firstLoginHandler;

    @Autowired
    public UserAttributeStoredEventListener(EventPublisher eventPublisher, FirstLoginHandler firstLoginHandler)
    {
        this.eventPublisher = checkNotNull(eventPublisher);
        this.firstLoginHandler = checkNotNull(firstLoginHandler);
        
    }

    @EventListener
    public void onUserAttributeStore(final UserAttributeStoredEvent event)
    {
        checkArgument(event != null, "Expecting event to be non-null");
        checkArgument(event.getUser() != null, "Expecting event to contain a non-null user");

        Option<Integer> optionalLoginCount = getLoginCount(event);
        if (optionalLoginCount.isDefined() && optionalLoginCount.get().equals(1))
        {
            firstLoginHandler.onFirstLogin(event.getUser().getName());
        }
    }

    private Option<Integer> getLoginCount(UserAttributeStoredEvent event)
    {
        Set<String> attributeValues = event.getAttributeValues(USER_ATTRIBUTE_KEY_LOGIN_COUNT);
        if (attributeValues == null || attributeValues.size() != 1)
        {
            return none();
        }

        String attributeValue = getOnlyElement(attributeValues);
        try
        {
            return some(parseInt(attributeValue));
        }
        catch (NumberFormatException ex)
        {
            return none();
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        eventPublisher.register(this);
    }

    @Override
    public void destroy() throws Exception
    {
        eventPublisher.unregister(this);
    }
}
