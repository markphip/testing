package com.atlassian.jira.plugins.dvcs.bitbucket.access.conditions;

import com.atlassian.jira.compatibility.bridge.application.ApplicationRoleManagerBridge;
import com.atlassian.jira.plugins.dvcs.bitbucket.access.conditions.ApplicationRoleEnabledCondition;
import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

@Listeners (MockitoTestNgListener.class)
public class ApplicationRoleEnabledConditionTest
{
    @InjectMocks
    private ApplicationRoleEnabledCondition applicationRoleEnabledCondition;

    @Mock
    private ApplicationRoleManagerBridge applicationRoleManagerBridge;

    @Test
    public void shouldReturnFalseWhenBridgeIsInactive()
    {
        when(applicationRoleManagerBridge.isBridgeActive()).thenReturn(false);
        when(applicationRoleManagerBridge.rolesEnabled()).thenThrow(RuntimeException.class);

        assertThat(applicationRoleEnabledCondition.shouldDisplay(emptyMap()), is(false));
    }

    @Test
    public void shouldReturnFalseWhenBridgeIsActiveAndRolesNotEnabled()
    {
        when(applicationRoleManagerBridge.isBridgeActive()).thenReturn(true);
        when(applicationRoleManagerBridge.rolesEnabled()).thenReturn(false);

        assertThat(applicationRoleEnabledCondition.shouldDisplay(emptyMap()), is(false));
    }

    @Test
    public void shouldReturnTrueWhenBridgeIsActiveAndRolesEnabled()
    {
        when(applicationRoleManagerBridge.isBridgeActive()).thenReturn(true);
        when(applicationRoleManagerBridge.rolesEnabled()).thenReturn(true);

        assertThat(applicationRoleEnabledCondition.shouldDisplay(emptyMap()), is(true));
    }
}