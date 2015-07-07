package com.atlassian.jira.plugins.dvcs.bitbucket.access.conditions;

import com.atlassian.jira.compatibility.bridge.application.ApplicationRoleManagerBridge;
import com.atlassian.plugin.web.Condition;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Checks whether application role is enabled. This condition is safe to use in older versions of JIRA as the
 * implementation uses a bridge object.
 *
 * In JIRA versions prior to 7.0, this condition always evaluates to false.
 */
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
