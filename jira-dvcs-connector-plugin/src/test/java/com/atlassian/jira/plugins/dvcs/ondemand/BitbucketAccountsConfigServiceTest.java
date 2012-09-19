package com.atlassian.jira.plugins.dvcs.ondemand;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.ondemand.AccountsConfig.BitbucketAccountInfo;
import com.atlassian.jira.plugins.dvcs.ondemand.AccountsConfig.Links;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;

@RunWith(MockitoJUnitRunner.class)
public class BitbucketAccountsConfigServiceTest
{

    @Mock
    private OrganizationService organizationService;

    @Mock
    private AccountsConfigProvider configProvider;

    @Captor
    ArgumentCaptor<Organization> organizationCaptor;

    private BitbucketAccountsConfigService testedService;

    public BitbucketAccountsConfigServiceTest()
    {
        super();
    }

    @Before
    public void setUp()
    {

        testedService = new BitbucketAccountsConfigService(configProvider, organizationService);

        when(configProvider.supportsIntegratedAccounts()).thenReturn(true);
    }

    @Test
    public void testAddNewAccountWithSuccess()
    {

        AccountsConfig correctConfig = createCorrectConfig();
        when(configProvider.provideConfiguration()).thenReturn(correctConfig);
        when(organizationService.findIntegratedAccount()).thenReturn(null);
        when(organizationService.getByHostAndName(eq("https://bitbucket.org"), eq("A"))).thenReturn(null);

        testedService.reload(false);

        verify(organizationService).save(organizationCaptor.capture());

        Organization savedOrg = organizationCaptor.getValue();

        Assert.assertEquals("A", savedOrg.getName());
        Assert.assertEquals("K", savedOrg.getCredential().getOauthKey());
        Assert.assertEquals("S", savedOrg.getCredential().getOauthSecret());
    }

    @Test
    public void testAddNewAccountEmptyConfig()
    {

        when(configProvider.provideConfiguration()).thenReturn(null);
        when(organizationService.findIntegratedAccount()).thenReturn(null);

        testedService.reload(false);

        verify(organizationService, times(0)).save(organizationCaptor.capture());

    }
    
    @Test
    public void testUpdateAccountEmptyConfig()
    {

        when(configProvider.provideConfiguration()).thenReturn(null);
        
        Organization existingAccount = createSampleAccount("A", "B", "S");
        when(organizationService.findIntegratedAccount()).thenReturn(existingAccount);

        testedService.reload(false);

        verify(organizationService, times(0)).save(organizationCaptor.capture());

        verify(organizationService).remove(eq(5));
    }

    @Test
    public void testUpdateAccountCredentialsWithSuccess()
    {

        AccountsConfig correctConfig = createCorrectConfig();
        when(configProvider.provideConfiguration()).thenReturn(correctConfig);

        Organization existingAccount = createSampleAccount("A", "B", "S");
        when(organizationService.findIntegratedAccount()).thenReturn(existingAccount);

        when(organizationService.getByHostAndName(eq("https://bitbucket.org"), eq("A"))).thenReturn(null);

        testedService.reload(false);

        verify(organizationService).updateCredentialsKeySecret(eq(5), eq("K"), eq("S"));

    }

    @Test
    public void testUpdateAccountCredentialsEmptyConfig_ShouldRemoveIntegratedAccount()
    {

        when(configProvider.provideConfiguration()).thenReturn(null);

        Organization existingAccount = createSampleAccount("A", "B", "S");
        when(organizationService.findIntegratedAccount()).thenReturn(existingAccount);

        when(organizationService.getByHostAndName(eq("https://bitbucket.org"), eq("A"))).thenReturn(null);

        testedService.reload(false);

        verify(organizationService).remove(eq(5));

    }
    
    //--
    
    @Test
    public void testAddNewAccountWithSuccess_UserAddedAccountExists()
    {

        AccountsConfig correctConfig = createCorrectConfig();
        when(configProvider.provideConfiguration()).thenReturn(correctConfig);
        
        when(organizationService.findIntegratedAccount()).thenReturn(null);
        
        Organization userAddedAccount = createSampleAccount("A", null, null);
        when(organizationService.getByHostAndName(eq("https://bitbucket.org"), eq("A"))).thenReturn(userAddedAccount);

        testedService.reload(false);
        
        verify(organizationService).updateCredentialsKeySecret(userAddedAccount.getId(), "K", "S");
    }

    //--
    
    
    private Organization createSampleAccount(String name, String key, String secret)
    {
        Organization org = new Organization();
        org.setId(5);
        org.setName(name);
        org.setCredential(new Credential(null, null, null, key, secret));
        return org;
    }

    private AccountsConfig createCorrectConfig()
    {
        return createConfig("A", "K", "S");
    }

    private AccountsConfig createConfig(String account, String key, String secret)
    {
        AccountsConfig accountsConfig = new AccountsConfig();
        List<Links> sysadminApplicationLinks = new ArrayList<AccountsConfig.Links>();
        Links links = new Links();
        List<BitbucketAccountInfo> bbLinks = new ArrayList<AccountsConfig.BitbucketAccountInfo>();
        BitbucketAccountInfo bbAccount = new BitbucketAccountInfo();
        bbAccount.setAccount(account);
        bbAccount.setKey(key);
        bbAccount.setSecret(secret);
        bbLinks.add(bbAccount);
        links.setBitbucket(bbLinks);
        sysadminApplicationLinks.add(links);
        accountsConfig.setSysadminApplicationLinks(sysadminApplicationLinks);
        return accountsConfig;
    }

}
