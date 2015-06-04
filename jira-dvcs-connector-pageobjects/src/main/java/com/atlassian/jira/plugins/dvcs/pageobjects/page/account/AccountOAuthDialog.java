package com.atlassian.jira.plugins.dvcs.pageobjects.page.account;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.WebDriverElement;
import com.atlassian.pageobjects.elements.WebDriverLocatable;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;
import com.google.common.base.Function;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.annotation.Nullable;

/**
 * Represents dialog, which is fired on {@link Account#regenerate()}.
 */
public class AccountOAuthDialog extends WebDriverElement
{
    @ElementBy(xpath = ".//input[@name='key']")
    private PageElement keyField;

    @ElementBy(xpath = ".//input[@name='secret']")
    private PageElement secretField;

    @ElementBy(xpath = ".//button[contains(concat(' ', @class, ' '), ' submit ')]")
    private PageElement submitButton;

    public AccountOAuthDialog(By locator, WebDriverLocatable parent, TimeoutType timeoutType)
    {
        super(locator, parent, timeoutType);
    }

    /**
     * @return OAuth key of this dialog.
     */
    public String getKey()
    {
        return keyField.getValue();
    }

    /**
     * @return OAuth secret of this dialog.
     */
    public String getSecret()
    {
        return secretField.getValue();
    }

    /**
     * Regenerates OAuth with provided key/secret.
     * 
     * @param key
     *            of OAuth
     * @param secret
     *            of OAuth
     */
    public void regenerate(String key, String secret)
    {
        keyField.clear().type(key);
        secretField.clear().type(secret);
        submitButton.click();

        try
        {
            new WebDriverWait(driver, 30).until(new Function<WebDriver, Boolean>()
            {

                @Override
                public Boolean apply(@Nullable WebDriver driver)
                {
                    return !AccountOAuthDialog.this.isPresent();
                }

            });
        }
        catch (StaleElementReferenceException e)
        {
            // silently ignored - dialog will disappeared, we know about them
        }
    }
}
