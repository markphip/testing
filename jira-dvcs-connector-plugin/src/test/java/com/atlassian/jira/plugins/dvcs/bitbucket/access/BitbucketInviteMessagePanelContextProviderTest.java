package com.atlassian.jira.plugins.dvcs.bitbucket.access;

import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import com.atlassian.webresource.api.assembler.RequiredResources;
import com.atlassian.webresource.api.assembler.WebResourceAssembler;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static com.atlassian.jira.plugins.dvcs.bitbucket.access.BitbucketInviteMessagePanelContextProvider.REQUIRED_WEB_RESOURCE_COMPLETE_KEY;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@Listeners (MockitoTestNgListener.class)
public class BitbucketInviteMessagePanelContextProviderTest extends BitbucketAccessExtensionContextProviderTest
{
    @InjectMocks
    private BitbucketInviteMessagePanelContextProvider bitbucketInviteMessagePanelContextProvider;

    @Mock
    private PageBuilderService pageBuilderService;

    @Mock
    private RequiredResources requiredResources;

    @Mock
    private WebResourceAssembler webResourceAssembler;

    @BeforeMethod
    public void prepare()
    {
        super.prepare();

        when(pageBuilderService.assembler()).thenReturn(webResourceAssembler);
        when(webResourceAssembler.resources()).thenReturn(requiredResources);
    }

    @Override
    protected BitbucketAccessExtensionContextProvider getInstanceUnderTest()
    {
        return bitbucketInviteMessagePanelContextProvider;
    }

    @Test
    public void shouldRequireResourcesAndData()
    {
        bitbucketInviteMessagePanelContextProvider.getContextMap(emptyMap());

        verify(requiredResources).requireWebResource(REQUIRED_WEB_RESOURCE_COMPLETE_KEY);
    }

    @Test
    public void shouldNotRequireResourcesAndDataWhenThereAreNoBitbucketTeamsWithDefaultGroups()
    {
        when(bitbucketTeamService.getTeamsWithDefaultGroups()).thenReturn(emptyList());

        bitbucketInviteMessagePanelContextProvider.getContextMap(emptyMap());

        verifyZeroInteractions(requiredResources);
    }


}