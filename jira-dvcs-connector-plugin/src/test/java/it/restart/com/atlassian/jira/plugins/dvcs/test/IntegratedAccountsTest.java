package it.restart.com.atlassian.jira.plugins.dvcs.test;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.ondemand.JsonFileBasedAccountsConfigProvider;
import com.atlassian.jira.plugins.dvcs.pageobjects.JiraLoginPageController;
import com.atlassian.jira.plugins.dvcs.pageobjects.bitbucket.BitbucketGrantAccessPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.common.BitbucketTestedProduct;
import com.atlassian.jira.plugins.dvcs.pageobjects.common.OAuth;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitbucketOAuthPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.OAuthCredentials;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.RepositoriesPageController;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.Account;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.Account.AccountType;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.AccountOAuthDialog;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.AccountsPage;
import com.atlassian.jira.plugins.dvcs.util.PasswordUtil;
import com.atlassian.pageobjects.TestedProductFactory;
import it.com.atlassian.jira.plugins.dvcs.DvcsWebDriverTestCase;
import it.util.TestAccounts;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.json.JSONException;
import org.json.JSONWriter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import static org.junit.Assert.*;

/**
 * Tests integrated accounts functionality.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class IntegratedAccountsTest extends DvcsWebDriverTestCase
{

    private static final String BB_ACCOUNT_NAME = TestAccounts.JIRA_BB_CONNECTOR_ACCOUNT;
    /**
     * Name of tested account.
     */
    private static final String ACCOUNT_NAME = BB_ACCOUNT_NAME;

    /**
     * Access Jira instance.
     */
    private static JiraTestedProduct JIRA = TestedProductFactory.create(JiraTestedProduct.class);

    private static BitbucketTestedProduct BITBUCKET = new BitbucketTestedProduct(JIRA.getTester());

    /**
     * Represents information of an integrated accounts.
     * 
     * @author Stanislav Dvorscak
     * 
     */
    private static final class IntegratedAccount
    {

        /**
         * @see #IntegratedAccount(String, String, String)
         */
        public final String name;

        /**
         * @see #IntegratedAccount(String, String, String)
         */
        public final String key;

        /**
         * @see #IntegratedAccount(String, String, String)
         */
        public final String secret;

        /**
         * Constructor.
         * 
         * @param name
         *            name of account
         * @param key
         *            appropriate OAuth key of account
         * @param secret
         *            appropriate OAuth secret of account
         */
        public IntegratedAccount(String name, String key, String secret)
        {
            this.name = name;
            this.key = key;
            this.secret = secret;
        }

    }

    /**
     * OAuth used as original - first/initial ...
     */
    private OAuth oAuthOriginal;

    /**
     * OAuth used as changed/new.
     */
    private OAuth oAuthNew;

    /**
     * Path to ondemand.properties.
     */
    private String onDemandConfigurationPath;

    /**
     * Prepares common environment.
     */
    @BeforeClass
    public void beforeTest()
    {
        // log in to JIRA
        new JiraLoginPageController(JIRA).login();
        BITBUCKET.login(BB_ACCOUNT_NAME, PasswordUtil.getPassword(BB_ACCOUNT_NAME));

        oAuthOriginal = BITBUCKET.visit(BitbucketOAuthPage.class, BB_ACCOUNT_NAME).addConsumer();
        oAuthNew = BITBUCKET.visit(BitbucketOAuthPage.class, BB_ACCOUNT_NAME).addConsumer();

        onDemandConfigurationPath = System.getProperty( //
                JsonFileBasedAccountsConfigProvider.ENV_ONDEMAND_CONFIGURATION, // environment customization
                JsonFileBasedAccountsConfigProvider.ENV_ONDEMAND_CONFIGURATION_DEFAULT // default value
                );
    }

    /**
     * Destroys common environment.
     */
    @AfterClass(alwaysRun = true)
    public void afterTestAlways()
    {
        BITBUCKET.visit(BitbucketOAuthPage.class, BB_ACCOUNT_NAME).removeConsumer(oAuthOriginal.applicationId);
        BITBUCKET.visit(BitbucketOAuthPage.class, BB_ACCOUNT_NAME).removeConsumer(oAuthNew.applicationId);
    }

    /**
     * Prepares test environment.
     */
    @BeforeMethod
    public void beforeMethod()
    {
        removeAllIntegratedAccounts();
        new RepositoriesPageController(JIRA).getPage().deleteAllOrganizations();
    }

    /**
     * Destroys test environment.
     */
    @AfterMethod(alwaysRun = true)
    public void afterMethod()
    {
        removeAllIntegratedAccounts();
        new RepositoriesPageController(JIRA).getPage().deleteAllOrganizations();
    }

    /**
     * Tests that OAuth of an account is regenerated/changed correctly.
     */
    @Test
    public void testEditOAuth()
    {
        RepositoriesPageController repositoriesPageController = new RepositoriesPageController(JIRA);
        repositoriesPageController.addOrganization(
                RepositoriesPageController.AccountType.BITBUCKET, ACCOUNT_NAME,
                new OAuthCredentials(oAuthOriginal.key, oAuthOriginal.secret), false);

        AccountsPage accountsPage = JIRA.visit(AccountsPage.class);
        Account account = accountsPage.getAccount(AccountType.BITBUCKET, ACCOUNT_NAME);
        account.regenerate().regenerate(oAuthNew.key, oAuthNew.secret);

        if (JIRA.getTester().getDriver().getCurrentUrl().startsWith("https://bitbucket.org/api/"))
        {
            JIRA.getPageBinder().bind(BitbucketGrantAccessPage.class).grantAccess();
        }

        AccountOAuthDialog oAuthDialog = account.regenerate();
        assertEquals(oAuthNew.key, oAuthDialog.getKey());
        assertEquals(oAuthNew.secret, oAuthDialog.getSecret());
    }

    /**
     * Tests if new integrated account is added.
     */
    @Test
    public void testNewIntegratedAccount()
    {
        buildOnDemandProperties(new IntegratedAccount(ACCOUNT_NAME, oAuthOriginal.key, oAuthOriginal.secret));
        refreshIntegratedAccounts();

        AccountsPage accountsPage = JIRA.visit(AccountsPage.class);
        Account account = accountsPage.getAccount(AccountType.BITBUCKET, ACCOUNT_NAME);
        assertTrue("Provided account has to be integrated account/OnDemand account!", account.isOnDemand());
    }

    /**
     * Tests if an existing account is switched into the integrated account.
     */
    @Test
    public void testSwitchToIntegratedAccount()
    {
        RepositoriesPageController repositoriesPageController = new RepositoriesPageController(JIRA);
        repositoriesPageController.addOrganization(
                RepositoriesPageController.AccountType.BITBUCKET, ACCOUNT_NAME,
                new OAuthCredentials(oAuthOriginal.key, oAuthOriginal.secret), false);

        buildOnDemandProperties(new IntegratedAccount(ACCOUNT_NAME, oAuthNew.key, oAuthNew.secret));
        refreshIntegratedAccounts();

        AccountsPage accountsPage = JIRA.visit(AccountsPage.class);
        Account account = accountsPage.getAccount(AccountType.BITBUCKET, ACCOUNT_NAME);
        assertTrue("Provided account has to be integrated account/OnDemand account!", account.isOnDemand());
    }

    /**
     * Removes all integrated accounts.
     */
    private void removeAllIntegratedAccounts()
    {
        buildOnDemandProperties();
        refreshIntegratedAccounts();

        AccountsPage accountsPage = JIRA.visit(AccountsPage.class);
        Iterator<Account> accounts = accountsPage.getAccounts().iterator();
        while (accounts.hasNext())
        {
            assertFalse(accounts.next().isOnDemand());
        }
    }

    /**
     * Refreshes integrated accounts.
     */
    private void refreshIntegratedAccounts()
    {
        try
        {
            String restUrl = JIRA.getProductInstance().getBaseUrl() + "/rest/bitbucket/1.0/integrated-accounts/reloadSync";
            GetMethod getMethod = new GetMethod(restUrl);
            assertEquals(200, new HttpClient().executeMethod(getMethod));
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Builds ondemand.properties for provided accounts.
     * 
     * @param accounts
     *            integrated accounts
     */
    private void buildOnDemandProperties(IntegratedAccount... accounts)
    {
        try
        {
            File onDemandConfigurationFile = new File(onDemandConfigurationPath);

            // creates parent structure
            if (!onDemandConfigurationFile.getParentFile().exists())
            {
                onDemandConfigurationFile.getParentFile().mkdirs();
            }

            if (!onDemandConfigurationFile.exists())
            {
                onDemandConfigurationFile.createNewFile();
            }

            FileWriter onDemandConfigurationWriter = new FileWriter(onDemandConfigurationFile);
            JSONWriter onDemandConfigurationJSON = new JSONWriter(onDemandConfigurationWriter);

            // root
            onDemandConfigurationJSON.object();

            // root/sysadmin-application-links[]
            onDemandConfigurationJSON.key("sysadmin-application-links").array();

            // root/sysadmin-application-links[]/bitbucket[]
            onDemandConfigurationJSON.object();
            onDemandConfigurationJSON.key("bitbucket").array();

            for (IntegratedAccount account : accounts)
            {
                onDemandConfigurationJSON.object();
                onDemandConfigurationJSON.key("account").value(account.name);
                onDemandConfigurationJSON.key("key").value(account.key);
                onDemandConfigurationJSON.key("secret").value(account.secret);
                onDemandConfigurationJSON.endObject();
            }

            // end: root/sysadmin-application-links[]/bitbucket[]
            onDemandConfigurationJSON.endArray();
            onDemandConfigurationJSON.endObject();

            // end: root/sysadmin-application-links[]
            onDemandConfigurationJSON.endArray();

            // end: root
            onDemandConfigurationJSON.endObject();

            // close file
            onDemandConfigurationWriter.close();

        } catch (IOException e)
        {
            throw new RuntimeException(e);

        } catch (JSONException e)
        {
            throw new RuntimeException(e);

        }
    }

}
