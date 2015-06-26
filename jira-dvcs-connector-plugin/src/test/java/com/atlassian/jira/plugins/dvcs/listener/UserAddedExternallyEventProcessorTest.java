package com.atlassian.jira.plugins.dvcs.listener;

import com.atlassian.crowd.embedded.api.CrowdService;
import com.atlassian.crowd.embedded.api.UserWithAttributes;
import com.atlassian.crowd.model.user.User;
import com.atlassian.jira.compatibility.bridge.application.ApplicationRoleManagerBridge;
import com.atlassian.jira.plugins.dvcs.bitbucket.access.BitbucketTeamService;
import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import com.atlassian.jira.software.api.roles.LicenseService;
import com.atlassian.jira.user.ApplicationUser;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static com.atlassian.jira.plugins.dvcs.listener.UserAddedExternallyEventProcessor.DVCS_TYPE_BITBUCKET;
import static com.atlassian.jira.plugins.dvcs.listener.UserAddedExternallyEventProcessor.SERVICE_DESK_CUSTOMERS_ATTRIBUTE_KEY;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.fest.util.Sets.newLinkedHashSet;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@Listeners (MockitoTestNgListener.class)
public class UserAddedExternallyEventProcessorTest
{
    private static final String EMAIL = "jsmith@gmail.com";

    private static final String USERNAME = "jsmith";

    @Mock
    private ApplicationRoleManagerBridge applicationRoleManagerBridge;

    @Mock
    private ApplicationUser applicationUser;

    @Mock
    private BitbucketTeamService bitbucketTeamService;

    @Mock
    private CrowdService crowdService;

    @Mock
    private DvcsCommunicator dvcsCommunicator;

    @Mock
    private DvcsCommunicatorProvider dvcsCommunicatorProvider;

    @Mock
    private LicenseService licenseService;

    @Mock
    private Organization organization1;

    @Mock
    private Organization organization2;

    @Mock
    private User user;

    @InjectMocks
    private UserAddedExternallyEventProcessor userAddedExternallyEventProcessor;

    @Mock
    private UserWithAttributes userWithAttributes;

    @BeforeMethod
    public void prepare()
    {
        when(applicationUser.getDirectoryUser()).thenReturn(user);
        when(user.getName()).thenReturn(USERNAME);
        when(crowdService.getUserWithAttributes(USERNAME)).thenReturn(userWithAttributes);

        when(dvcsCommunicatorProvider.getCommunicator(DVCS_TYPE_BITBUCKET)).thenReturn(dvcsCommunicator);

        when(applicationUser.getEmailAddress()).thenReturn(EMAIL);

        when(organization1.getDefaultGroups()).thenReturn(newLinkedHashSet(new Group("developers"), new Group("administrators")));
        when(organization1.getDvcsType()).thenReturn(DVCS_TYPE_BITBUCKET);

        when(organization2.getDefaultGroups()).thenReturn(newLinkedHashSet(new Group("administrators")));
        when(organization2.getDvcsType()).thenReturn(DVCS_TYPE_BITBUCKET);
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionWhenUserIsNull()
    {
        userAddedExternallyEventProcessor.process(null);
    }

    @Test
    public void shouldNotInviteUserWhenRunningInDarkAgesAndUserIsAServiceDeskCustomer()
    {
        when(userWithAttributes.getValue(SERVICE_DESK_CUSTOMERS_ATTRIBUTE_KEY)).thenReturn(Boolean.TRUE.toString());
        when(bitbucketTeamService.getTeamsWithDefaultGroups()).thenReturn(asList(organization1));

        userAddedExternallyEventProcessor.process(applicationUser);

        verifyZeroInteractions(dvcsCommunicator);
    }

    @Test
    public void shouldInviteUserWhenRunningInDarkAgesAndUserIsNotAServiceDeskCustomer()
    {
        when(bitbucketTeamService.getTeamsWithDefaultGroups()).thenReturn(asList(organization1));

        userAddedExternallyEventProcessor.process(applicationUser);

        verify(dvcsCommunicator).inviteUser(organization1, asList("developers", "administrators"), EMAIL);
    }

    @Test
    public void shouldNotInviteUserWhenRunningInRenaissanceAndUserIsNotASoftwareUser()
    {
        when(applicationRoleManagerBridge.isBridgeActive()).thenReturn(true);
        when(applicationRoleManagerBridge.rolesEnabled()).thenReturn(true);
        when(bitbucketTeamService.getTeamsWithDefaultGroups()).thenReturn(asList(organization1));

        userAddedExternallyEventProcessor.process(applicationUser);

        verifyZeroInteractions(dvcsCommunicator);
    }

    @Test
    public void shouldInviteUserWhenRunningInRenaissanceAndUserIsAServiceDeskCustomerAndASoftwareUser()
    {
        when(applicationRoleManagerBridge.isBridgeActive()).thenReturn(true);
        when(applicationRoleManagerBridge.rolesEnabled()).thenReturn(true);
        when(licenseService.isSoftwareUser(applicationUser)).thenReturn(true);
        when(userWithAttributes.getValue(SERVICE_DESK_CUSTOMERS_ATTRIBUTE_KEY)).thenReturn(Boolean.TRUE.toString());
        when(bitbucketTeamService.getTeamsWithDefaultGroups()).thenReturn(asList(organization1));

        userAddedExternallyEventProcessor.process(applicationUser);

        verify(dvcsCommunicator).inviteUser(organization1, asList("developers", "administrators"), EMAIL);
    }

    @Test
    public void shouldNotInviteUserWhenThereAreNoBitbucketTeamsWithDefaultGroups()
    {
        when(applicationRoleManagerBridge.isBridgeActive()).thenReturn(true);
        when(applicationRoleManagerBridge.rolesEnabled()).thenReturn(true);
        when(licenseService.isSoftwareUser(applicationUser)).thenReturn(true);
        when(bitbucketTeamService.getTeamsWithDefaultGroups()).thenReturn(emptyList());

        userAddedExternallyEventProcessor.process(applicationUser);

        verifyZeroInteractions(dvcsCommunicator);
    }

    @Test
    public void shouldInviteUserToMultipleBitbucketTeams()
    {
        when(applicationRoleManagerBridge.isBridgeActive()).thenReturn(true);
        when(applicationRoleManagerBridge.rolesEnabled()).thenReturn(true);
        when(licenseService.isSoftwareUser(applicationUser)).thenReturn(true);
        when(bitbucketTeamService.getTeamsWithDefaultGroups()).thenReturn(asList(organization1, organization2));

        userAddedExternallyEventProcessor.process(applicationUser);

        verify(dvcsCommunicator).inviteUser(organization1, asList("developers", "administrators"), EMAIL);
        verify(dvcsCommunicator).inviteUser(organization2, asList("administrators"), EMAIL);
    }
}

