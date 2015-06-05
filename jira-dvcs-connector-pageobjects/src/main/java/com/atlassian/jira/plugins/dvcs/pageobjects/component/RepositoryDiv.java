package com.atlassian.jira.plugins.dvcs.pageobjects.component;

import com.atlassian.jira.plugins.dvcs.pageobjects.page.RepositoriesPage;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Queries;
import com.atlassian.pageobjects.elements.query.TimedQuery;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;
import com.atlassian.pageobjects.elements.timeout.Timeouts;
import org.apache.commons.lang.math.NumberUtils;
import org.openqa.selenium.By;

import javax.inject.Inject;

import static com.atlassian.pageobjects.elements.query.Poller.waitUntil;
import static com.atlassian.pageobjects.elements.query.Poller.waitUntilFalse;
import static com.atlassian.pageobjects.elements.query.Poller.waitUntilTrue;
import static org.hamcrest.Matchers.greaterThan;

/**
 * @deprecated see deprecation note on {@link RepositoriesPage}
 */
@Deprecated
public class RepositoryDiv
{
    @Inject
    protected Timeouts timeouts;

    private final PageElement rootElement;
    private final PageElement syncRadio;

    public RepositoryDiv(PageElement rootElement)
    {
        this.rootElement = rootElement;
        this.syncRadio = rootElement != null? rootElement.find(By.className("radio")) : null;
    }

    public String getMessage()
    {
        return rootElement.find(By.xpath("td[3]/div")).getText();
    }

    public String getRepositoryName()
    {
        return rootElement.find(By.xpath("td[2]/a")).getText();
    }

    public PageElement getSyncIcon()
    {
        return rootElement.find(By.xpath("td[4]//span"));
    }

    public String getElementId()
    {
        return rootElement.getAttribute("id");
    }

    public String getRepositoryId()
    {
        return parseRepositoryId(getElementId());
    }

    public String parseRepositoryId(String elementId)
    {
        return elementId.substring(elementId.lastIndexOf("-") + 1);
    }

    public void enableSync()
    {
        if (syncRadio != null)
        {
            waitUntilTrue("Sync radio should always be enabled", syncRadio.timed().isEnabled());
            if (!syncRadio.isSelected())
            {
                syncRadio.click();
                waitUntilTrue(syncRadio.timed().isSelected());
            }
        }
    }

    public void sync()
    {
        final PageElement syncIcon = getSyncIcon();
        waitUntilTrue(syncIcon.timed().isVisible());
        TimedQuery<Long> lastSync = Queries.forSupplier(timeouts,
                () -> NumberUtils.toLong(syncIcon.getAttribute("data-last-sync")), TimeoutType.SLOW_PAGE_LOAD);
        final long lastSyncBefore = lastSync.now();

        syncIcon.click();
        waitUntil(lastSync, greaterThan(lastSyncBefore));
        waitUntilFalse(syncIcon.withTimeout(TimeoutType.SLOW_PAGE_LOAD).timed().hasClass("running"));
    }

}
