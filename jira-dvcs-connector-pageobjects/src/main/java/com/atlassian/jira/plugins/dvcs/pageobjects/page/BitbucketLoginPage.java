package com.atlassian.jira.plugins.dvcs.pageobjects.page;

import com.atlassian.jira.plugins.dvcs.pageobjects.util.PageElementUtils;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;

import static com.atlassian.pageobjects.elements.timeout.TimeoutType.PAGE_LOAD;

public class BitbucketLoginPage implements Page
{
    @ElementBy(id = "id_username")
    private PageElement usernameOrEmailInput;

    @ElementBy(id = "id_password")
    private PageElement passwordInput;
    
    @ElementBy(name = "submit")
    private PageElement loginButton;

    @ElementBy(id = "user-dropdown-trigger", timeoutType = PAGE_LOAD)
    private PageElement userDropdownTriggerLink;
    
    @ElementBy(linkText = "Log out")
    private PageElement logoutLink;

    @Override
    public String getUrl()
    {
        return "/account/signin/?next=/";
    }

    public void doLogin(String username, String password)
    {
        // accessing tag name as workaround for permission denied to access property 'nr@context' issue
        PageElementUtils.permissionDeniedWorkAround(usernameOrEmailInput);

        usernameOrEmailInput.clear().type(username);
        passwordInput.clear().type(password);
        loginButton.click();
    }
    
    public void doLogout()
    {
        // accessing tag name as workaround for permission denied to access property 'nr@context' issue
        PageElementUtils.permissionDeniedWorkAround(usernameOrEmailInput);

        if (userDropdownTriggerLink.isPresent())
        {
            // only do the logout if the user drop down is present, i.e., if the user is logged in.
            userDropdownTriggerLink.click();
            logoutLink.click();
        }
    }
    
}
