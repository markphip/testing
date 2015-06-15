package com.atlassian.jira.plugins.dvcs.pageobjects.page.account;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.binder.WaitUntil;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.PageElementFinder;
import com.google.common.collect.Iterables;
import org.openqa.selenium.By;

import javax.inject.Inject;

import static com.atlassian.jira.pageobjects.framework.elements.PageElements.bind;
import static com.atlassian.pageobjects.elements.query.Conditions.and;
import static com.atlassian.pageobjects.elements.query.Poller.waitUntilTrue;
import static com.google.common.collect.Iterables.transform;

public class DvcsAccountsPage implements Page
{
    @Inject
    protected PageElementFinder pageElementFinder;

    @Inject
    protected PageBinder pageBinder;

    @ElementBy(id = "linkRepositoryButton")
    protected PageElement linkAccountButton;

    @ElementBy(id = "organization-list")
    protected PageElement organisationsList;

    @Override
    public String getUrl()
    {
        return "/secure/admin/ConfigureDvcsOrganizations!default.jspa";
    }

    @WaitUntil
    public void waitUntilLoaded()
    {
        waitUntilTrue(and(organisationsList.timed().isPresent(), linkAccountButton.timed().isPresent()));
    }

    public Iterable<Account> getAccounts()
    {
        return transform(pageElementFinder.findAll(By.className("dvcs-orgdata-container")),
                bind(pageBinder, Account.class));
    }

    public Account getAccount(Account.AccountType accountType, String accountName)
    {
        return Iterables.find(getAccounts(), Account.matches(accountName, accountType));
    }

    public static Account syncAccount(JiraTestedProduct jiraTestedProduct,
            Account.AccountType accountType, String accountName, String repositoryName,
            boolean refresh)
    {
        DvcsAccountsPage accountsPage = jiraTestedProduct.visit(DvcsAccountsPage.class);
        Account account = accountsPage.getAccount(accountType, accountName);
        if (refresh)
        {
            account.refresh();
        }
        account.synchronizeRepository(repositoryName);
        return account;
    }
}
