package com.atlassian.jira.plugins.dvcs.bitbucket.access;

import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import org.mockito.InjectMocks;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static com.atlassian.jira.plugins.dvcs.bitbucket.access.AddUserBitbucketAccessExtensionContextProvider.REQUIRED_DATA_BITBUCKET_INVITE_TO_GROUPS_KEY;
import static com.atlassian.jira.plugins.dvcs.bitbucket.access.AddUserBitbucketAccessExtensionContextProvider.REQUIRED_WEB_RESOURCE_COMPLETE_KEY;
import static java.util.Collections.emptyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Listeners (MockitoTestNgListener.class)
public class AddUserBitbucketAccessExtensionContextProviderTest extends BitbucketAccessExtensionContextProviderTest
{
    @InjectMocks
    private AddUserBitbucketAccessExtensionContextProvider addUserBitbucketAccessExtensionContextProvider;

    @BeforeMethod
    public void prepare()
    {
        super.prepare();

        when(webResourceAssembler.data()).thenReturn(requiredData);
    }

    @Override
    protected BitbucketAccessExtensionContextProvider getInstanceUnderTest()
    {
        return addUserBitbucketAccessExtensionContextProvider;
    }

    @Test
    public void shouldRequireResourcesAndData()
    {
        addUserBitbucketAccessExtensionContextProvider.getContextMap(emptyMap());

        verify(requiredResources).requireWebResource(REQUIRED_WEB_RESOURCE_COMPLETE_KEY);
        verify(requiredData).requireData(REQUIRED_DATA_BITBUCKET_INVITE_TO_GROUPS_KEY,
                "1:developers;2:administrators;2:developers;3:administrators;4:developers;5:administrators;5:developers");
    }
}