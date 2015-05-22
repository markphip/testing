package com.atlassian.jira.plugins.dvcs.pageobjects.page.account;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.AbstractComponentPageObject;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.elements.CheckboxElement;
import com.atlassian.pageobjects.elements.DataAttributeFinder;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.PageElementActions;
import com.atlassian.pageobjects.elements.PageElementFinder;
import com.atlassian.pageobjects.elements.WebDriverElement;
import com.atlassian.pageobjects.elements.WebDriverLocatable;
import com.atlassian.pageobjects.elements.query.Conditions;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.pageobjects.elements.query.Queries;
import com.atlassian.pageobjects.elements.query.TimedCondition;
import com.atlassian.pageobjects.elements.query.TimedQuery;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;
import com.atlassian.pageobjects.elements.timeout.Timeouts;
import com.google.common.base.Supplier;
import org.apache.commons.lang.math.NumberUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import static com.atlassian.pageobjects.elements.query.Poller.waitUntilFalse;
import static com.atlassian.pageobjects.elements.query.Poller.waitUntilTrue;
import static org.hamcrest.Matchers.*;

/**
 * Represents repository table row of {@link Account}.
 *
 * @author Stanislav Dvorscak
 *
 */
public class AccountRepository extends AbstractComponentPageObject
{
    @Inject
    protected PageElementFinder elementFinder;

    @Inject
    protected PageBinder pageBinder;

    @Inject
    protected JiraTestedProduct jiraTestedProduct;

    @Inject
    protected PageElementActions actions;

    @Inject
    protected Timeouts timeouts;

    public AccountRepository(PageElement container) {
        super(container);
    }

    public int getId()
    {
        return Integer.parseInt(DataAttributeFinder.query(container).getDataAttribute("id"));
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
     * NOTE: this method only checks whether the sync is in progress, for comple
     *
     * @return True if synchronization is currently in progress.
     */
    @Nonnull
    public TimedCondition isSyncing()
    {
        return getSynchronizationIcon().withTimeout(TimeoutType.SLOW_PAGE_LOAD).timed().hasClass("running");
    }

    /**
     * Checks whether the "last sync" timestamp has changed, which indicates that a sync has finished.
     *
     * NOTE: this must be called _before_ the synchronization is triggered so that the current "last sync" timestamp
     * is captured
     *
     * @return {@code true} if synchronization process has been triggered and finished since this timed condition has
     * been obtained
     */
    @Nonnull
    public TimedCondition isSynchronizationFinished()
    {
        TimedQuery<Long> lastSync = Queries.forSupplier(timeouts, new Supplier<Long>() {
            @Override
            public Long get() {
                return NumberUtils.toLong(getSynchronizationIcon().getAttribute("data-last-sync"));
            }
        }, TimeoutType.SLOW_PAGE_LOAD);
        long lastSyncBefore = lastSync.now();
        return Conditions.forMatcher(lastSync, greaterThan(lastSyncBefore));
    }

    @Nonnull
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
        actions.keyDown(Keys.LEFT_SHIFT).click(getSynchronizationButton()).perform();

        ForceSyncDialog forceSyncDialog = getForceSyncDialog();
        Poller.waitUntilTrue(forceSyncDialog.isOpen());
        TimedCondition syncFinished = isSynchronizationFinished();
        forceSyncDialog.fullSync();
        waitUntilTrue(syncFinished);

        return this;
    }

    protected ForceSyncDialog getForceSyncDialog() {
        return pageBinder.bind(ForceSyncDialog.class, elementFinder.find(By.className("forceSyncDialog")));
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
        TimedCondition isSyncFinished = isSynchronizationFinished();
        triggerSynchronization();
        waitUntilTrue(isSyncFinished);
    }

    public static class ForceSyncDialog extends AbstractComponentPageObject
    {
        @ElementBy(className = "full-sync-trigger")
        private PageElement fullSyncButton;

        public ForceSyncDialog(PageElement container) {
            super(container);
        }

        public void fullSync()
        {
            fullSyncButton.click();
            Poller.waitUntilFalse(isOpen());
        }

        public TimedCondition isOpen()
        {
            return container.timed().isVisible();
        }
    }

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
