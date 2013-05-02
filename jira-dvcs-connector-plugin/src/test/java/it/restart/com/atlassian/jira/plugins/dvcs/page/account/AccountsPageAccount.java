package it.restart.com.atlassian.jira.plugins.dvcs.page.account;

import static com.atlassian.pageobjects.elements.query.Poller.by;
import static org.hamcrest.Matchers.is;

import org.openqa.selenium.By;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.WebDriverElement;
import com.atlassian.pageobjects.elements.WebDriverLocatable;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;

/**
 * Container of single account of {@link AccountsPage}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class AccountsPageAccount extends WebDriverElement
{

    /**
     * Type of account.
     * 
     * @author Stanislav Dvorscak
     * 
     */
    public enum AccountType
    {
        /**
         * GitHub account type.
         */
        GIT_HUB("githubLogo"), GIT_HUB_ENTERPRISE("githubeLogo"),

        /**
         * Bitbucket account type.
         */
        BITBUCKET("bitbucketLogo");

        /**
         * @see #getLogoClassName()
         */
        private String logoClassName;

        /**
         * Constructor.
         * 
         * @param logoClassName
         *            {@link #getLogoClassName()}
         */
        private AccountType(String logoClassName)
        {
            this.logoClassName = logoClassName;
        }

        /**
         * @return CSS class name of logo
         */
        public String getLogoClassName()
        {
            return logoClassName;
        }
    }

    /**
     * Reference to "Controls" button.
     */
    @ElementBy(xpath = ".//li[contains(concat(' ', @class, ' '), ' dvcs-organization-controls-tool ')]//button")
    private PageElement controlsButton;

    /**
     * Reference to "Controls" dialog, which appeared after {@link #controlsButton} fire.
     */
    @ElementBy(xpath = ".//li[contains(concat(' ', @class, ' '), ' dvcs-organization-controls-tool ')]//ul")
    private AccountsPageAccountControlsDialog controlsDialog;

    /**
     * Reference to {@link AccountsPageAccountOAuthDialog}.
     * 
     * @see #regenerate()
     */
    @ElementBy(xpath = "//div[contains(concat(' ', @class, ' '), ' dialog-components ')]")
    private AccountsPageAccountOAuthDialog oAuthDialog;

    /**
     * @see #isOnDemand()
     */
    @ElementBy(xpath = ".//span[@title='OnDemand']")
    private PageElement onDemandDecorator;

    /**
     * Constructor.
     * 
     * @param locator
     */
    public AccountsPageAccount(By locator)
    {
        super(locator);
    }

    /**
     * Constructor.
     * 
     * @param locatable
     * @param timeoutType
     */
    public AccountsPageAccount(WebDriverLocatable locatable, TimeoutType timeoutType)
    {
        super(locatable, timeoutType);
    }

    /**
     * Resolves repository for provided name.
     * 
     * @param repositoryName
     *            name of repository
     * @return resolved repository
     */
    public AccountsPageAccountRepository getRepository(String repositoryName)
    {
        return find(By.xpath("table/tbody/tr/td[@class='dvcs-org-reponame']/a[text()='" + repositoryName + "']/ancestor::tr"),
                AccountsPageAccountRepository.class);
    }

    /**
     * @return True if this account is consider to be OnDemand account.
     */
    public boolean isOnDemand()
    {
        return onDemandDecorator.isPresent() && onDemandDecorator.isVisible();
    }

    /**
     * Refreshes repositories of this account.
     */
    public void refresh()
    {
        controlsButton.click();
        controlsDialog.refresh();
        // wait for popup to show up
        try
        {
            Poller.waitUntilTrue(find(By.id("refreshing-account-dialog")).timed().isVisible());
        } catch (AssertionError e)
        {
            // ignore, the refresh was probably very quick and the popup has been already closed.
        }
        Poller.waitUntil(find(By.id("refreshing-account-dialog")).timed().isVisible(), is(false), by(30000));
    }

    /**
     * Regenerates account OAuth.
     * 
     * @return OAuth dialog
     */
    public AccountsPageAccountOAuthDialog regenerate()
    {
        controlsButton.click();
        controlsDialog.regenerate();
        return oAuthDialog;
    }

}
