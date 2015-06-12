package com.atlassian.jira.plugins.dvcs.bitbucket.access;

import com.atlassian.jira.compatibility.bridge.application.ApplicationRoleManagerBridge;
import com.atlassian.plugin.web.Condition;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class ApplicationRoleEnabledCondition implements Condition
{
    private final ApplicationRoleManagerBridge applicationRoleManagerBridge;

    public ApplicationRoleEnabledCondition(ApplicationRoleManagerBridge applicationRoleManagerBridge)
    {
        this.applicationRoleManagerBridge = checkNotNull(applicationRoleManagerBridge);
    }

    @Override
    public void init(Map<String, String> params) {}

    @Override
    public boolean shouldDisplay(Map<String, Object> context)
    {
        return applicationRoleManagerBridge.isBridgeActive() && applicationRoleManagerBridge.rolesEnabled();
    }
}
