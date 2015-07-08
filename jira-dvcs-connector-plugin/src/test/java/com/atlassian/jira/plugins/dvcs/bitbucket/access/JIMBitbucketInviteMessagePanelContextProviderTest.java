package com.atlassian.jira.plugins.dvcs.bitbucket.access;

import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import org.mockito.InjectMocks;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static com.atlassian.jira.plugins.dvcs.bitbucket.access.JIMBitbucketInviteMessagePanelContextProvider.REQUIRED_WEB_RESOURCE_COMPLETE_KEY;
import static java.util.Collections.emptyMap;
import static org.mockito.Mockito.verify;

@Listeners (MockitoTestNgListener.class)
public class JIMBitbucketInviteMessagePanelContextProviderTest extends BaseBitbucketAccessExtensionContextProviderTest
{
    @InjectMocks
    private JIMBitbucketInviteMessagePanelContextProvider jimBitbucketInviteMessagePanelContextProvider;

    @Override
    protected JIMBitbucketInviteMessagePanelContextProvider getInstanceUnderTest()
    {
        return jimBitbucketInviteMessagePanelContextProvider;
    }

    @Test
    public void shouldRequireResourcesAndData()
    {
        jimBitbucketInviteMessagePanelContextProvider.getContextMap(emptyMap());

        verify(requiredResources).requireWebResource(REQUIRED_WEB_RESOURCE_COMPLETE_KEY);
    }
}
