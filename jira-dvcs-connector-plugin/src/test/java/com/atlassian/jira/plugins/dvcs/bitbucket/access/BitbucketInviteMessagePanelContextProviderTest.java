package com.atlassian.jira.plugins.dvcs.bitbucket.access;

import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import org.mockito.InjectMocks;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static com.atlassian.jira.plugins.dvcs.bitbucket.access.BitbucketInviteMessagePanelContextProvider.REQUIRED_WEB_RESOURCE_COMPLETE_KEY;
import static java.util.Collections.emptyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Listeners (MockitoTestNgListener.class)
public class BitbucketInviteMessagePanelContextProviderTest extends BitbucketAccessExtensionContextProviderTest
{
    @InjectMocks
    private BitbucketInviteMessagePanelContextProvider bitbucketInviteMessagePanelContextProvider;

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
}