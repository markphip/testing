package it.restart.com.atlassian.jira.plugins.dvcs.test;

import static org.fest.assertions.api.Assertions.assertThat;
import it.restart.com.atlassian.jira.plugins.dvcs.JiraAddUserPage;
import it.restart.com.atlassian.jira.plugins.dvcs.JiraBitbucketOAuthPage;
import it.restart.com.atlassian.jira.plugins.dvcs.JiraLoginPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.OrganizationDiv;
import it.restart.com.atlassian.jira.plugins.dvcs.RepositoriesPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.bitbucket.BitbucketLoginPage;
import it.restart.com.atlassian.jira.plugins.dvcs.bitbucket.BitbucketOAuthPage;
import it.restart.com.atlassian.jira.plugins.dvcs.common.MagicVisitor;
import it.restart.com.atlassian.jira.plugins.dvcs.common.OAuth;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.pageobjects.elements.PageElement;

public class BitbucketOrganizationsTest implements BasicOrganizationTests, MissingCommitsTests
{
    private static JiraTestedProduct jira = TestedProductFactory.create(JiraTestedProduct.class);
    private static final String ACCOUNT_NAME = "jirabitbucketconnector";
    private OAuth oAuth;
    
    @BeforeClass
    public void beforeClass()
    {
        // log in to JIRA 
        new JiraLoginPageController(jira).login();
        // log in to Bitbucket
        new MagicVisitor(jira).visit(BitbucketLoginPage.class).doLogin();
        // setup up OAuth from bitbucket
        oAuth = new MagicVisitor(jira).visit(BitbucketOAuthPage.class).addConsumer();
        jira.visit(JiraBitbucketOAuthPage.class).setCredentials(oAuth.key, oAuth.secret);
    }
    
    @AfterClass
    public void afterClass()
    {
        // delete all organizations
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.getPage().deleteAllOrganizations();
        // remove OAuth in bitbucket
        new MagicVisitor(jira).visit(BitbucketOAuthPage.class).removeConsumer(oAuth.applicationId);
        // log out from bitbucket
        new MagicVisitor(jira).visit(BitbucketLoginPage.class).doLogout();
    }
    
    @BeforeMethod
    public void beforeMethod()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.getPage().deleteAllOrganizations();
    }
    
    @Override
    @Test
    public void addOrganization()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        OrganizationDiv organization = rpc.addOrganization(RepositoriesPageController.BITBUCKET, ACCOUNT_NAME, false);
        
        assertThat(organization).isNotNull(); 
        assertThat(organization.getRepositories().size()).isEqualTo(4);  
        
        // check add user extension
        PageElement dvcsExtensionsPanel = jira.visit(JiraAddUserPage.class).getDvcsExtensionsPanel();
        assertThat(dvcsExtensionsPanel.isVisible());
    }
    
    @Override
    @Test
    public void addOrganizationWaitForSync()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        OrganizationDiv organization = rpc.addOrganization(RepositoriesPageController.BITBUCKET, ACCOUNT_NAME, true);
        
        assertThat(organization).isNotNull(); 
        assertThat(organization.getRepositories().size()).isEqualTo(4);
        assertThat(organization.getRepositories().get(3).getMessage()).isEqualTo("Fri Mar 02 2012");
    }
    
    @Override
    @Test(expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = ".*Error!\\nThe url \\[https://privatebitbucket.org\\] is incorrect or the server is not responding.*")
    public void addOrganizationInvalidUrl()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.addOrganization(RepositoriesPageController.BITBUCKET, "https://privatebitbucket.org/someaccount", false);
    }

    @Override
    @Test(expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = ".*Error!\\nThe authentication with Bitbucket has failed. Please check your OAuth settings.*")
    public void addOrganizationInvalidOAuth()
    {
        try
        {
            jira.visit(JiraBitbucketOAuthPage.class).setCredentials("xxx","yyy");
            RepositoriesPageController rpc = new RepositoriesPageController(jira);
            OrganizationDiv organization = rpc.addOrganization(RepositoriesPageController.BITBUCKET, ACCOUNT_NAME, true);

            assertThat(organization).isNotNull(); 
            assertThat(organization.getRepositories().size()).isEqualTo(4);  
        } finally
        {
            jira.visit(JiraBitbucketOAuthPage.class).setCredentials(oAuth.key, oAuth.secret);
        }
    }

    
}
