package com.atlassian.jira.plugins.dvcs.pageobjects.page;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.pageobjects.component.BitBucketOrganization;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.ProductInstance;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.PageElementFinder;
import com.atlassian.pageobjects.elements.SelectElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.pageobjects.elements.query.TimedQuery;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPage;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPageAccount;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPageAccountRepository;
import org.hamcrest.Matcher;
import org.openqa.selenium.By;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

/**
 * Represents the page to link repositories to projects
 */
public abstract class BaseConfigureOrganizationsPage implements Page
{
    @Inject
    PageBinder pageBinder;

    @Inject
    ProductInstance product;

    @Inject
    PageElementFinder elementFinder;

    @ElementBy(id = "linkRepositoryButton")
    PageElement linkRepositoryButton;

    @ElementBy(className = "button-panel-submit-button")
    PageElement addOrgButton;

    @ElementBy(className = "gh_messages")
    PageElement syncStatusDiv;

    @ElementBy(id = "aui-message-bar")
    PageElement messageBarDiv;

    @ElementBy(id = "organization")
    PageElement organization;

    @ElementBy(id = "organization-list")
    PageElement organizationsElement;

    @ElementBy(id = "autoLinking")
    PageElement autoLinkNewRepos;

    @ElementBy(id = "urlSelect")
    SelectElement dvcsTypeSelect;

    protected JiraTestedProduct jiraTestedProduct;


    @Override
    public String getUrl()
    {
        return "/secure/admin/ConfigureDvcsOrganizations!default.jspa";
    }


    public List<BitBucketOrganization> getOrganizations()
    {
        List<BitBucketOrganization> list = new ArrayList<BitBucketOrganization>();

        for (PageElement orgContainer : organizationsElement.findAll(By.className("dvcs-orgdata-container")))
        {
             Poller.waitUntilTrue(orgContainer.find(By.className("dvcs-org-container")).timed().isVisible());

             list.add(pageBinder.bind(BitBucketOrganization.class, orgContainer));
        }

        return list;
    }

    public AccountsPageAccount getOrganization(AccountsPageAccount.AccountType accountType, String accountName)
    {
        AccountsPage accountsPage = pageBinder.bind(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(accountType, accountName);

        return account;
    }

    public BaseConfigureOrganizationsPage deleteAllOrganizations()
    {
        List<BitBucketOrganization> orgs;
        while (!(orgs = getOrganizations()).isEmpty())
        {
            orgs.get(0).delete();
        }

        return this;
    }

    public void assertThatErrorMessage(Matcher<String> matcher)
    {
        Poller.waitUntil(messageBarDiv.find(By.className("aui-message-error")).timed().getText(), matcher);
    }

    protected void waitFormBecomeVisible()
    {
        Poller.waitUntilTrue(isFormOpen());
    }

    /**
     * The current error status message
     *
     * @return error status message
     */

    public String getErrorStatusMessage()
    {
        return messageBarDiv.find(By.className("aui-message-error")).timed().getText().now();
    }

    public abstract BaseConfigureOrganizationsPage addOrganizationFailingStep1(String url);

    public abstract BaseConfigureOrganizationsPage addOrganizationFailingOAuth();

    public abstract BaseConfigureOrganizationsPage addOrganizationSuccessfully(String organizationAccount, OAuthCredentials oAuthCredentials, boolean autosync);


    public void setJiraTestedProduct(JiraTestedProduct jiraTestedProduct)
    {
        this.jiraTestedProduct = jiraTestedProduct;
    }

    public void clearForm()
    {

    }


    public boolean containsRepositoryWithName(String askedRepositoryName)
    {
        // parsing following HTML:
        // <td class="dvcs-org-reponame">
        //     <a href="...">browsermob-proxy</a>
        // </td>

        for (PageElement repositoryNameTableRow : elementFinder.findAll(By.className("dvcs-org-reponame")))
        {
            String repositoryName = repositoryNameTableRow.find(By.tagName("a")).getText();

            if (repositoryName.equals(askedRepositoryName))
            {
                return true;
            }
        }

        return false;
    }


    public String getRepositoryIdFromRepositoryName(String queriedRepositoryName)
    {
        for (PageElement repositoryRow : elementFinder.findAll(By.className("dvcs-repo-row")))
        {
            String repositoryName = repositoryRow.find(By.className("dvcs-org-reponame")).find(By.tagName("a")).getText();

            if (repositoryName.equals(queriedRepositoryName))
            {
                String id = repositoryRow.getAttribute("id");
                return id.replaceAll("dvcs-repo-row-", "");
            }
        }

        return null;
    }

    public TimedQuery<Boolean> isFormOpen()
    {
        return getForm().timed().isVisible();
    }

    public TimedQuery<String> getDvcsTypeSelectValue()
    {
        return dvcsTypeSelect.timed().getValue();
    }

    public TimedQuery<String> getFormAction()
    {
        return getForm().timed().getAttribute("action");
    }

    private PageElement getForm()
    {
        return elementFinder.find(By.id("repoEntry"));
    }

    /**
     * Enables and synchronizes given repository
     *
     * @param accountType type of account
     * @param accountName account name
     * @param repositoryName repository name
     * @return {@link it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPageAccountRepository} element
     */
    public AccountsPageAccountRepository enableAndSyncRepository(AccountsPageAccount.AccountType accountType, String accountName, String repositoryName)
    {
        AccountsPage accountsPage = pageBinder.bind(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(accountType, accountName);

        AccountsPageAccountRepository repository = account.getRepository(repositoryName);
        repository.enable();
        repository.synchronize();
        return repository;
    }
}
