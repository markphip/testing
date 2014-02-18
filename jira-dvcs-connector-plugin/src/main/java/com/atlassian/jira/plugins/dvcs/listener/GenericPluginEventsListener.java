package com.atlassian.jira.plugins.dvcs.listener;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.dvcs.util.DvcsConstants;
import com.atlassian.jira.plugins.dvcs.util.SystemUtils;
import com.atlassian.plugin.event.events.PluginDisabledEvent;
import com.atlassian.plugin.event.events.PluginEnabledEvent;

public class GenericPluginEventsListener implements InitializingBean, DisposableBean
{
    private final EventPublisher eventPublisher;

    public GenericPluginEventsListener(EventPublisher eventPublisher)
    {
        super();
        this.eventPublisher = eventPublisher;
    }

    @EventListener
    // listens also to install event
    public void pluginEnabled(PluginEnabledEvent event)
    {
        if (DvcsConstants.DEVSUMMARY_PLUGIN_ID.equals(event.getPlugin().getKey()))
        {
           clearDevstatus();
        }
    }

    @EventListener
    // listens also to uninstall event
    public void pluginDisabled(PluginDisabledEvent event)
    {
        if (DvcsConstants.DEVSUMMARY_PLUGIN_ID.equals(event.getPlugin().getKey()))
        {
           clearDevstatus();
        }
    }

    private void clearDevstatus()
    {
        SystemUtils.clearDevStatusExists();
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
