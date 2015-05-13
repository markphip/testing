package com.atlassian.jira.plugins.dvcs.pageobjects.page.account;

import com.atlassian.jira.plugins.dvcs.pageobjects.page.AbstractComponentPageObject;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.PageElementFinder;
import com.atlassian.pageobjects.elements.query.Poller;
import org.openqa.selenium.By;

import javax.inject.Inject;

import static com.atlassian.jira.pageobjects.framework.elements.PageElements.bind;
import static com.atlassian.pageobjects.elements.query.Poller.by;
import static com.google.common.collect.Iterables.transform;
import static org.hamcrest.Matchers.is;

/**
 * Container of single account of {@link AccountsPage}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class Account extends AbstractComponentPageObject
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

    @Inject
    private PageElementFinder elementFinder;

    @Inject
    private PageBinder pageBinder;
    
    /**
     * Reference to "Controls" button.
     */
    @ElementBy(xpath = ".//button[contains(concat(' ', @class, ' '), ' aui-dropdown2-trigger ')]")
    private PageElement controlsButton;

    /**
     * Reference to {@link AccountOAuthDialog}.
     * 
     * @see #regenerate()
     */
    @ElementBy(xpath = "//div[contains(concat(' ', @class, ' '), ' dialog-components ')]")
    private AccountOAuthDialog oAuthDialog;

    /**
     * @see #isOnDemand()
     */
    @ElementBy(xpath = ".//span[@title='OnDemand']")
    private PageElement onDemandDecorator;

    public Account(PageElement container) {
        super(container);
    }

    /**
     * Resolves repository for provided name.
     * 
     * @param repositoryName
     *            name of repository
     * @return resolved repository
     */
    public AccountRepository getRepository(String repositoryName)
    {
        return pageBinder.bind(
                AccountRepository.class,
                find(By.xpath("table/tbody/tr/td[@class='dvcs-org-reponame']/a[text()='" + repositoryName + "']/ancestor::tr"))
        );
    }

    /**
     * Resolves repository for provided name.
     *
     * @return resolved repositories
     */
    public Iterable<AccountRepository> getRepositories()
    {
        return transform(findAll(By.className("dvcs-repo-row")),
                bind(pageBinder, AccountRepository.class));
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
        findControlDialog().refresh();
        // wait for popup to show up
        try
        {
            Poller.waitUntilTrue(find(By.id("refreshing-account-dialog")).timed().isVisible());
        } catch (AssertionError e)
        {
            // ignore, the refresh was probably very quick and the popup has been already closed.
        }
        Poller.waitUntil(find(By.id("refreshing-account-dialog")).timed().isVisible(), is(false), by(600000));
    }

    /**
     * Regenerates account OAuth.
     * 
     * @return OAuth dialog
     */
    public AccountOAuthDialog regenerate()
    {
        controlsButton.click();
        findControlDialog().regenerate();
        return oAuthDialog;
    }

    public AccountRepository enableRepository(String repositoryName, boolean noAdminPermission)
    {
        AccountRepository repository = getRepository(repositoryName);

        repository.enable(noAdminPermission);

        return repository;
    }
    
    /**
     * @return "Controls" dialog, which appeared after {@link #controlsButton} fire.
     */
    private AccountControlsDialog findControlDialog()
    {
        String dropDownMenuId = controlsButton.getAttribute("aria-owns");
        return elementFinder.find(By.id(dropDownMenuId), AccountControlsDialog.class);
    }

    /**
     * Synchronizes the repository with the given name
     *
     * @param repositoryName name of the repository to be synchronized
     * @return page object of the repository
     */
    public AccountRepository synchronizeRepository(String repositoryName)
    {
        AccountRepository repository = getRepository(repositoryName);

        if (!repository.isEnabled())
        {
            repository.enable();
        }

        repository.synchronize();

        return repository;
    }

    /**
     * Full synchronizes the repository with the given name
     *
     * @param repositoryName name of the repository to be synchronized
     * @return page object of the repository
     */
    public AccountRepository fullSynchronizeRepository(String repositoryName)
    {
        AccountRepository repository = getRepository(repositoryName);
        if (!repository.isEnabled())
        {
            repository.enable();
        }
        repository.fullSynchronize();

        return repository;
    }

    /**
     * Synchronize the repositories with the given names
     *
     * @param repositoryNames names of the repositories to be synchronized
     */
    public void synchronizeRepositories(String... repositoryNames)
    {
        for (String repositoryName : repositoryNames)
        {
            AccountRepository repository = getRepository(repositoryName);
            repository.enable();
            repository.synchronizeWithNoWait();
        }
    }
}
