package com.atlassian.jira.plugins.dvcs.pageobjects.page.account;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.elements.PageElementFinder;
import org.openqa.selenium.By;

import javax.inject.Inject;

import static com.atlassian.jira.pageobjects.framework.elements.PageElements.bind;
import static com.google.common.collect.Iterables.transform;

/**
 * Holds available DVCS accounts.
 *
 * @author Stanislav Dvorscak
 */
public class AccountsPage implements Page
{
    @Inject
    protected PageElementFinder pageElementFinder;

    @Inject
    protected PageBinder pageBinder;

    @Override
    public String getUrl()
    {
        return "/secure/admin/ConfigureDvcsOrganizations!default.jspa";
    }

    public Iterable<Account> getAccounts()
    {
        return transform(pageElementFinder.findAll(By.className("dvcs-orgdata-container")),
                bind(pageBinder, Account.class));
    }

    /**
     * Constructor.
     *
     * @param accountType type of account
     * @param accountName name of account
     * @return founded account element
     */
    public Account getAccount(Account.AccountType accountType, String accountName)
    {
        return pageBinder.bind(Account.class, pageElementFinder.find(
                By.xpath("//h4[contains(concat(' ', @class, ' '), '" + accountType.getLogoClassName() + "')]/a[text() = '" + accountName
                        + "']/ancestor::div[contains(concat(' ', @class, ' '), 'dvcs-orgdata-container')]")));
    }

    public static Account syncAccount(JiraTestedProduct jiraTestedProduct,
            Account.AccountType accountType, String accountName, String repositoryName,
            boolean refresh)
    {
        AccountsPage accountsPage = jiraTestedProduct.visit(AccountsPage.class);
        Account account = accountsPage.getAccount(accountType, accountName);
        if (refresh)
        {
            account.refresh();
        }
        account.synchronizeRepository(repositoryName);
        return account;
    }
}
