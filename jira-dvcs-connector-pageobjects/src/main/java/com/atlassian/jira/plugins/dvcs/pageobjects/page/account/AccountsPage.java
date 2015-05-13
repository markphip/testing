package com.atlassian.jira.plugins.dvcs.pageobjects.page.account;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElementFinder;
import com.atlassian.webdriver.AtlassianWebDriver;
import org.openqa.selenium.By;

import javax.inject.Inject;

/**
 * Holds available DVCS accounts.
 *
 * @author Stanislav Dvorscak
 */
public class AccountsPage implements Page
{
    @Inject
    protected AtlassianWebDriver driver;

    @Inject
    private PageElementFinder pageElementFinder;

    @ElementBy (className = "dvcs-orgdata-container", pageElementClass = AccountsPageAccount.class)
    private Iterable<AccountsPageAccount> accounts;

    public Iterable<AccountsPageAccount> getAccounts()
    {
        return accounts;
    }

    /**
     * Constructor.
     *
     * @param accountType type of account
     * @param accountName name of account
     * @return founded account element
     */
    public AccountsPageAccount getAccount(AccountsPageAccount.AccountType accountType, String accountName)
    {
        return pageElementFinder.find(
                By.xpath("//h4[contains(concat(' ', @class, ' '), '" + accountType.getLogoClassName() + "')]/a[text() = '" + accountName
                        + "']/ancestor::div[contains(concat(' ', @class, ' '), 'dvcs-orgdata-container')]"), AccountsPageAccount.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUrl()
    {
        return "/secure/admin/ConfigureDvcsOrganizations!default.jspa";
    }

    public static AccountsPageAccount syncAccount(JiraTestedProduct jiraTestedProduct,
            AccountsPageAccount.AccountType accountType, String accountName, String repositoryName,
            boolean refresh)
    {
        AccountsPage accountsPage = jiraTestedProduct.visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(accountType, accountName);
        if (refresh)
        {
            account.refresh();
        }
        account.synchronizeRepository(repositoryName);
        return account;
    }
}
