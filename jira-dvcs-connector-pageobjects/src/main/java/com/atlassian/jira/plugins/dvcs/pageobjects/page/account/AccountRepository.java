package com.atlassian.jira.plugins.dvcs.pageobjects.page.account;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.AbstractComponentPageObject;
import com.atlassian.pageobjects.elements.CheckboxElement;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.PageElementFinder;
import com.atlassian.pageobjects.elements.WebDriverElement;
import com.atlassian.pageobjects.elements.WebDriverLocatable;
import com.atlassian.pageobjects.elements.query.TimedCondition;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;
import com.google.common.base.Strings;
import org.junit.Assert;
import org.openqa.selenium.By;

import javax.inject.Inject;

import static com.atlassian.pageobjects.elements.query.Poller.waitUntilFalse;
import static com.atlassian.pageobjects.elements.query.Poller.waitUntilTrue;

/**
 * Represents repository table row of {@link Account}.
 *
 * @author Stanislav Dvorscak
 *
 */
public class AccountRepository extends AbstractComponentPageObject
{

    @Inject
    private PageElementFinder elementFinder;

    @Inject
    private JiraTestedProduct jiraTestedProduct;

    public AccountRepository(PageElement container) {
        super(container);
    }

    public int getId()
    {
        return Integer.parseInt(container.getAttribute("id").substring("dvcs-repo-row-".length()));
    }

    /**
     * @return is repository enabled?
     */
    public boolean isEnabled()
    {
        return getEnableCheckbox().isSelected();
    }

    public String getMessage()
    {
        return getSynchronizationMessageElement().getText();
    }

    public boolean hasWarning()
    {
        return getWarningIconElement().hasClass("admin_permission")
                && getWarningIconElement().hasClass("aui-iconfont-warning")
                && getWarningIconElement().isVisible();
    }

    /**
     * Enables repository.
     *
     * @see #isEnabled()
     */
    public void enable()
    {
        enable(false);
    }

    public void enable(boolean forceNoAdminPermissionCheck)
    {
        if (!isEnabled())
        {
            getEnableCheckbox().check();

            LinkingRepositoryDialog linkingRepositoryDialog = elementFinder.find(By.id("dvcs-postcommit-hook-registration-dialog"), LinkingRepositoryDialog.class);

            // check that dialog appears
            try
            {
                waitUntilTrue(linkingRepositoryDialog.withTimeout(TimeoutType.DIALOG_LOAD).timed().isVisible());
                linkingRepositoryDialog.clickOk();
            }
            catch (AssertionError e)
            {
                if (forceNoAdminPermissionCheck)
                {
                    throw new AssertionError("DVCS Webhhook registration dialog expected, but not present");
                }
            }

            waitUntilTrue(getSynchronizationButton().withTimeout(TimeoutType.PAGE_LOAD).timed().isVisible());
        }
    }

    /**
     * @return True if synchronization is currently in progress.
     */
    public TimedCondition isSyncing()
    {
        return getSynchronizationIcon().withTimeout(TimeoutType.PAGE_LOAD).timed().hasClass("running");
    }

    /**
     * Fires synchronization button.
     */
    public void synchronize()
    {
        syncAndWaitForFinish();
        if (hasRepoSyncError())
        {
            // retrying synchronization once
            syncAndWaitForFinish();
        }
        Assert.assertFalse("Synchronization failed", hasRepoSyncError());
    }

    private boolean hasRepoSyncError()
    {
        return !Strings.isNullOrEmpty(getSynchronizationErrorMessageElement().timed().getText().now());
    }

    private void syncAndWaitForFinish()
    {
        getSynchronizationButton().click();
        waitUntilFalse(isSyncing());
    }

    public void synchronizeWithNoWait()
    {
        getSynchronizationButton().click();
    }

    /**
     * Fires full synchronization
     */
    public void fullSynchronize()
    {
        String script = getSynchronizationButton().getAttribute("onclick");
        script = script.replace("event", "{shiftKey: true}");
        getSynchronizationButton().javascript().execute(script);
        ForceSyncDialog forceSyncDialog = elementFinder.find(By.xpath("//div[contains(concat(' ', @class, ' '), ' forceSyncDialog ')]"), ForceSyncDialog.class);
        forceSyncDialog.fullSync();
        waitUntilFalse(isSyncing());
    }

    protected CheckboxElement getEnableCheckbox()
    {
        return container.find(By.id("repo_autolink_check" + getId()), CheckboxElement.class);
    }

    protected PageElement getSynchronizationButton()
    {
        return container.find(By.id("jira-dvcs-connector-forceSyncDialog-" + getId()));
    }

    protected PageElement getSynchronizationIcon()
    {
        return container.find(By.id("syncrepoicon_" + getId()));
    }

    protected PageElement getSynchronizationMessageElement()
    {
        return container.find(By.id("sync_status_message_" + getId()));
    }

    protected PageElement getSynchronizationErrorMessageElement()
    {
        return container.find(By.id("sync_error_message_" + getId()));
    }

    protected PageElement getWarningIconElement()
    {
        return container.find(By.id("error_status_icon_" + getId()));
    }

    public static class ForceSyncDialog extends WebDriverElement
    {
        @ElementBy(xpath = "//a[@class='aui-button']")
        private PageElement fullSyncButton;

        public ForceSyncDialog(final By locator)
        {
            super(locator);
        }

        public ForceSyncDialog(final By locator, final TimeoutType timeoutType)
        {
            super(locator, timeoutType);
        }

        public ForceSyncDialog(final By locator, final WebDriverLocatable parent)
        {
            super(locator, parent);
        }

        public ForceSyncDialog(final By locator, final WebDriverLocatable parent, final TimeoutType timeoutType)
        {
            super(locator, parent, timeoutType);
        }

        public ForceSyncDialog(final WebDriverLocatable locatable, final TimeoutType timeoutType)
        {
            super(locatable, timeoutType);
        }

        public void fullSync()
        {
            fullSyncButton.click();
        }
    }

    /**
     * Page class for linking repository dialog
     *
     */
    public static class LinkingRepositoryDialog extends WebDriverElement
    {
        @ElementBy (xpath = "//div[@class='dialog-button-panel']/button")
        private PageElement okButton;

        public LinkingRepositoryDialog(final By locator)
        {
            super(locator);
        }

        public LinkingRepositoryDialog(final By locator, final TimeoutType timeoutType)
        {
            super(locator, timeoutType);
        }

        public LinkingRepositoryDialog(final By locator, final WebDriverLocatable parent)
        {
            super(locator, parent);
        }

        public LinkingRepositoryDialog(final By locator, final WebDriverLocatable parent, final TimeoutType timeoutType)
        {
            super(locator, parent, timeoutType);
        }

        public LinkingRepositoryDialog(final WebDriverLocatable locatable, final TimeoutType timeoutType)
        {
            super(locatable, timeoutType);
        }

        public void clickOk()
        {
            okButton.click();
        }
    }
}
