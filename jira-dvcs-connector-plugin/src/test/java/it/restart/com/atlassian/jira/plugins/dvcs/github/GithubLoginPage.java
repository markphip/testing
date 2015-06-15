package it.restart.com.atlassian.jira.plugins.dvcs.github;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.util.PasswordUtil;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;

import javax.inject.Inject;

import static it.restart.com.atlassian.jira.plugins.dvcs.test.GithubTestHelper.GITHUB_URL;
import static it.util.TestAccounts.JIRA_BB_CONNECTOR_ACCOUNT;

public class GithubLoginPage implements Page
{
    @ElementBy(id = "login_field")
    private PageElement loginField;

    @ElementBy(id = "logout")
    private PageElement oldLogoutLink;

    @ElementBy(className = "dropdown-signout")
    private PageElement logoutLink;

    @ElementBy(xpath = "//a[@aria-label='View profile and more']")
    private PageElement profileDropdown;

    @ElementBy(id = "password")
    private PageElement passwordField;

    @ElementBy(name = "commit")
    private PageElement submitButton;

    @ElementBy(xpath = "//input[@value='Sign out']")
    private PageElement logoutConfirm;

    @Inject
    private JiraTestedProduct jiraTestedProduct;

    private final String hostUrl;

    
    public GithubLoginPage()
    {
        this(GITHUB_URL);
    }
    
    public GithubLoginPage(String hostUrl)
    {
        this.hostUrl = hostUrl;
    }

    @Override
    public String getUrl()
    {
        return hostUrl+"/login";
    }

    public void doLogin()
    {
        doLogin(JIRA_BB_CONNECTOR_ACCOUNT, PasswordUtil.getPassword(JIRA_BB_CONNECTOR_ACCOUNT));
    }
    
    public void doLogin(String username, String password)
    {
        // if logout link is present, other user remained logged in
        if (logoutLink.isPresent())
        {
            doLogout();
            jiraTestedProduct.getTester().gotoUrl(getUrl());
        }

        loginField.type(username);
        passwordField.type(password);
        submitButton.click();
    }
    
    /**
     * Logout is done by POST method. It's not enough to go to /logout page. We
     * need to submit a form that gets us there
     */
    public void doLogout()
    {
        if (logoutLink.isPresent())
        {
            profileDropdown.click();
            Poller.waitUntilTrue(logoutLink.timed().isVisible());
            logoutLink.click();
        }
        else if (oldLogoutLink.isPresent())
        {
            oldLogoutLink.click();
        }
        else
        {
            return; // skip if user has already logged out
        }
        try
        {
            // GitHub sometimes requires logout confirm
            Poller.waitUntilTrue(logoutConfirm.timed().isPresent());
            logoutConfirm.click();
        }
        catch (AssertionError e)
        {
            // GitHub doesn't requires logout confirm
        }
    }
}
