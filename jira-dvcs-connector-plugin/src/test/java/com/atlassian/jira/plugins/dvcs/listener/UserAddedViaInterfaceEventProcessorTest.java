package com.atlassian.jira.plugins.dvcs.listener;

import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import com.atlassian.jira.user.ApplicationUser;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static com.atlassian.jira.plugins.dvcs.listener.UserAddedViaInterfaceEventProcessor.DVCS_TYPE_BITBUCKET;
import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@Listeners (MockitoTestNgListener.class)
public class UserAddedViaInterfaceEventProcessorTest
{
    private static final String EMAIL = "jsmith@mail.com";
    private static final String MULTIPLE_TEAMS_UI_SELECTION = "1:developers;1:administrators;2:developers";
    private static final String SINGLE_TEAM_UI_SELECTION = "1:developers;1:administrators";

    @Mock
    private ApplicationUser applicationUser;

    @Mock
    private DvcsCommunicator dvcsCommunicator;

    @Mock
    private DvcsCommunicatorProvider dvcsCommunicatorProvider;

    @Mock
    private Organization organization1;

    @Mock
    private Organization organization2;

    @Mock
    private OrganizationService organizationService;

    @InjectMocks
    private UserAddedViaInterfaceEventProcessor userAddedViaInterfaceEventProcessor;

    @BeforeMethod
    public void prepare()
    {
        when(applicationUser.getEmailAddress()).thenReturn(EMAIL);

        when(dvcsCommunicatorProvider.getCommunicator(DVCS_TYPE_BITBUCKET)).thenReturn(dvcsCommunicator);

        when(organizationService.get(eq(1), any(Boolean.class))).thenReturn(organization1);
        when(organizationService.get(eq(2), any(Boolean.class))).thenReturn(organization2);

        when(organization1.getDvcsType()).thenReturn(DVCS_TYPE_BITBUCKET);
        when(organization2.getDvcsType()).thenReturn(DVCS_TYPE_BITBUCKET);
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldThrowNullPointerExceptionWhenSerialisedSelectionFromUIIsNull()
    {
        userAddedViaInterfaceEventProcessor.process(applicationUser, null);
    }

    @Test (expectedExceptions = NullPointerException.class)
    public void shouldThrowNullPointerExceptionWhenUserIsNull()
    {
        userAddedViaInterfaceEventProcessor.process(null, SINGLE_TEAM_UI_SELECTION);
    }

    @Test
    public void shouldDoNothingWhenSerialisedSelectionFromUIIsEmpty()
    {
        userAddedViaInterfaceEventProcessor.process(applicationUser, "");

        verifyZeroInteractions(dvcsCommunicatorProvider, dvcsCommunicator);
    }

    @Test
    public void shouldDoNothingWhenSerialisedSelectionFromUIIsBlank()
    {
        userAddedViaInterfaceEventProcessor.process(applicationUser, " ");

        verifyZeroInteractions(dvcsCommunicatorProvider, dvcsCommunicator);
    }

    @Test
    public void shouldInviteUserToASingleBitbucketTeam()
    {
        userAddedViaInterfaceEventProcessor.process(applicationUser, SINGLE_TEAM_UI_SELECTION);

        verify(dvcsCommunicator).inviteUser(organization1, asList("developers", "administrators"), EMAIL);
        verifyNoMoreInteractions(dvcsCommunicator);
    }

    @Test
    public void shouldInviteUserToMultipleBitbucketTeams()
    {
        userAddedViaInterfaceEventProcessor.process(applicationUser, MULTIPLE_TEAMS_UI_SELECTION);

        verify(dvcsCommunicator).inviteUser(organization1, asList("developers", "administrators"), EMAIL);
        verify(dvcsCommunicator).inviteUser(organization2, asList("developers"), EMAIL);
        verifyNoMoreInteractions(dvcsCommunicator);
    }
}

