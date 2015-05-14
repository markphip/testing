package com.atlassian.jira.plugins.dvcs.pageobjects.page.account;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.AbstractComponentPageObject;
import com.atlassian.pageobjects.elements.CheckboxElement;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.PageElementFinder;
import com.atlassian.pageobjects.elements.WebDriverElement;
import com.atlassian.pageobjects.elements.WebDriverLocatable;
import com.atlassian.pageobjects.elements.query.Conditions;
import com.atlassian.pageobjects.elements.query.TimedCondition;
import com.atlassian.pageobjects.elements.query.TimedQuery;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;
import org.openqa.selenium.By;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import static com.atlassian.pageobjects.elements.query.Poller.waitUntilFalse;
import static com.atlassian.pageobjects.elements.query.Poller.waitUntilTrue;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;

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
    public TimedCondition isEnabled()
    {
        return Conditions.and(
                getEnableCheckbox().timed().isSelected(),
                getSynchronizationButton().timed().isVisible()
        );
    }

    public TimedQuery<String> getMessage()
    {
        return getSynchronizationMessageElement().timed().getText();
    }

    public TimedCondition hasWarning()
    {
        return Conditions.and(
                getWarningIconElement().timed().isVisible(),
                getWarningIconElement().timed().hasClass("admin_permission"),
                getWarningIconElement().timed().hasClass("aui-iconfont-warning")
        );
    }

    /**
     * @return True if synchronization is currently in progress.
     */
    @Nonnull
    public TimedCondition isSyncing()
    {
        return getSynchronizationIcon().withTimeout(TimeoutType.PAGE_LOAD).timed().hasClass("running");
    }

    public TimedCondition hasRepositorySyncError()
    {
        return Conditions.forMatcher(getRepositorySyncError(), not(isEmptyOrNullString()));
    }

    @Nonnull
    public TimedQuery<String> getRepositorySyncError()
    {
        return getSynchronizationErrorMessageElement().timed().getText();
    }

    /**
     * Enables repository.
     *
     * @see #isEnabled()
     */
    public AccountRepository enable()
    {
        return enable(false);
    }

    public AccountRepository enable(boolean forceNoAdminPermissionCheck)
    {
        if (!isEnabled().now())
        {
            getEnableCheckbox().check();
            LinkingRepositoryDialog linkingRepositoryDialog = getLinkRepositoryDialog();
            approveLinkRepository(forceNoAdminPermissionCheck, linkingRepositoryDialog);
            waitUntilTrue(isEnabled());
        }

        return this;
    }

    /**
     * Triggers synchronization and waits for the result. Re-tries if the synchronization failed on the first attempt.
     *
     * @return this repository
     */
    public AccountRepository synchronize()
    {
        syncAndWaitForFinish();
        if (hasRepositorySyncError().now())
        {
            // retrying synchronization once
            syncAndWaitForFinish();
        }
        waitUntilFalse("Synchronization failed", hasRepositorySyncError());

        return this;
    }

    /**
     * Triggers synchronization without waiting for and/or checking the results of it.
     *
     * @return this repository
     */
    public AccountRepository triggerSynchronization()
    {
        getSynchronizationButton().click();

        return this;
    }

    /**
     * Triggers full synchronization.
     *
     * @return this repository
     */
    public AccountRepository fullSynchronize()
    {
        String script = getSynchronizationButton().getAttribute("onclick");
        script = script.replace("event", "{shiftKey: true}");
        getSynchronizationButton().javascript().execute(script);
        ForceSyncDialog forceSyncDialog = elementFinder.find(By.xpath("//div[contains(concat(' ', @class, ' '), ' forceSyncDialog ')]"), ForceSyncDialog.class);
        forceSyncDialog.fullSync();
        waitUntilFalse(isSyncing());

        return this;
    }

    protected CheckboxElement getEnableCheckbox()
    {
        return container.find(By.id("repo_autolink_check" + getId()), CheckboxElement.class, TimeoutType.PAGE_LOAD);
    }

    protected LinkingRepositoryDialog getLinkRepositoryDialog() {
        return elementFinder.find(By.id("dvcs-postcommit-hook-registration-dialog"), LinkingRepositoryDialog.class);
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

    private void approveLinkRepository(boolean forceNoAdminPermissionCheck, LinkingRepositoryDialog linkingRepositoryDialog) {
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
    }

    private void syncAndWaitForFinish()
    {
        triggerSynchronization();
        waitUntilFalse(isSyncing());
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
