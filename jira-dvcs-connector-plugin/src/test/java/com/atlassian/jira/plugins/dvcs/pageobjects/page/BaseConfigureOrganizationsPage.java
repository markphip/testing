package com.atlassian.jira.plugins.dvcs.pageobjects.page;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.hamcrest.Matcher;
import org.openqa.selenium.By;

import com.atlassian.jira.plugins.dvcs.pageobjects.component.BitBucketOrganization;
import com.atlassian.jira.plugins.dvcs.util.PageElementUtils;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.PageElementFinder;
import com.atlassian.pageobjects.elements.SelectElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.jira.pageobjects.JiraTestedProduct;

/**
 * Represents the page to link repositories to projects
 */
public abstract class BaseConfigureOrganizationsPage implements Page
{
    @Inject
    PageBinder pageBinder;

    @Inject
    PageElementFinder elementFinder;

    @ElementBy(id = "linkRepositoryButton")
    PageElement linkRepositoryButton;

    @ElementBy(id = "Submit")
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

        for (PageElement orgContainer : organizationsElement.findAll(By.className("dvcs-orgdata-container"))) {

        	 Poller.waitUntilTrue(orgContainer.find(By.className("dvcs-org-container")).timed().isVisible());

             list.add(pageBinder.bind(BitBucketOrganization.class, orgContainer));

    	}

        return list;
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
        Poller.waitUntil(messageBarDiv.find(By.className("error")).timed().getText(), matcher);
    }

    protected void waitFormBecomeVisible()
    {
        jiraTestedProduct.getTester().getDriver().waitUntilElementIsVisible(By.id("repoEntry"));
    }

    /**
     * The current error status message
     *
     * @return error status message
     */

    public String getErrorStatusMessage()
    {
        return messageBarDiv.find(By.className("error")).timed().getText().now();
    }

    public abstract BaseConfigureOrganizationsPage addOrganizationFailingStep1(String url);

    public abstract BaseConfigureOrganizationsPage addRepoToProjectFailingStep2();

    public abstract BaseConfigureOrganizationsPage addRepoToProjectFailingPostcommitService(String url);

    public abstract BaseConfigureOrganizationsPage addOrganizationSuccessfully(String organizationAccount, boolean autosync);


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
                PageElement syncRepoLink = PageElementUtils.findTagWithAttribute(repositoryRow, "a", "onclick");
                
                String onclickAttributeValue = syncRepoLink.getAttribute("onclick");
                // parsing: onclick="forceSync(90); AJS.$('.gh_messages.repository90').slideDown(); return false;"
                int openBraceIndex  = onclickAttributeValue.indexOf('(');
                int closeBraceIndex = onclickAttributeValue.indexOf(')');

                return onclickAttributeValue.substring(openBraceIndex + 1, closeBraceIndex);
            }
        }

        return null;
    }
}
