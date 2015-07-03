package com.atlassian.jira.plugins.dvcs.bitbucket.access;

import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import com.atlassian.webresource.api.assembler.RequiredData;
import com.atlassian.webresource.api.assembler.RequiredResources;
import com.atlassian.webresource.api.assembler.WebResourceAssembler;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static com.atlassian.jira.plugins.dvcs.bitbucket.access.AddUserBitbucketAccessExtensionContextProvider.REQUIRED_DATA_BITBUCKET_INVITE_TO_GROUPS_KEY;
import static com.atlassian.jira.plugins.dvcs.bitbucket.access.AddUserBitbucketAccessExtensionContextProvider.REQUIRED_WEB_RESOURCE_COMPLETE_KEY;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@Listeners (MockitoTestNgListener.class)
public class AddUserBitbucketAccessExtensionContextProviderTest extends BitbucketAccessExtensionContextProviderTest
{
    @InjectMocks
    private AddUserBitbucketAccessExtensionContextProvider addUserBitbucketAccessExtensionContextProvider;

    @Mock
    private PageBuilderService pageBuilderService;

    @Mock
    private RequiredData requiredData;

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

    @Test
    public void shouldNotRequireResourcesAndDataWhenThereAreNoBitbucketTeamsWithDefaultGroups()
    {
        when(bitbucketTeamService.getTeamsWithDefaultGroups()).thenReturn(emptyList());

        addUserBitbucketAccessExtensionContextProvider.getContextMap(emptyMap());

        verifyZeroInteractions(requiredResources, requiredData);
    }
}