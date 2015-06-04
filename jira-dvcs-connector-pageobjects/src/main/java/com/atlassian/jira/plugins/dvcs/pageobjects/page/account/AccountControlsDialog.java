package com.atlassian.jira.plugins.dvcs.pageobjects.page.account;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.WebDriverElement;
import com.atlassian.pageobjects.elements.WebDriverLocatable;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;
import org.openqa.selenium.By;

/**
 * Controls dialog of {@link Account}.
 */
public class AccountControlsDialog extends WebDriverElement
{

    @ElementBy(linkText = "Refresh list")
    private PageElement refreshLink;

    @ElementBy(linkText = "Reset OAuth Settings")
    private PageElement regenerateLink;

    public AccountControlsDialog(By locator, WebDriverLocatable parent, TimeoutType timeoutType)
    {
        super(locator, parent, timeoutType);
    }

    public AccountControlsDialog(By locator, WebDriverLocatable parent)
    {
        super(locator, parent);
    }

    public AccountControlsDialog(By locator)
    {
        super(locator);
    }

    /**
     * Refreshes repositories list of account.
     */
    public void refresh()
    {
        refreshLink.click();
    }

    /**
     * Regenerates account OAuth.
     */
    public void regenerate()
    {
        regenerateLink.click();
    }

}
