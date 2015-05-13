package com.atlassian.jira.plugins.dvcs.pageobjects.page;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.pageobjects.components.JiraHeader;
import com.atlassian.jira.pageobjects.dialogs.quickedit.CreateIssueDialog;
import com.atlassian.jira.pageobjects.pages.DashboardPage;
import com.atlassian.jira.pageobjects.project.DeleteProjectPage;
import com.atlassian.jira.pageobjects.project.ViewProjectsPage;
import com.atlassian.jira.pageobjects.project.summary.ProjectSummaryPageTab;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.AccountsPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.AccountsPageAccount;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.AccountsPageAccountRepository;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.elements.query.Conditions;
import com.atlassian.pageobjects.elements.query.TimedCondition;
import com.atlassian.pageobjects.elements.query.TimedQuery;
import com.google.common.collect.Lists;

import java.util.List;

import static com.atlassian.pageobjects.elements.query.Poller.waitUntilFalse;

/**
 * @author Martin Skurla
 */
public class JiraPageUtils
{
    private JiraPageUtils() {}


    public static boolean projectExists(JiraTestedProduct jira, String projectName)
    {
        ViewProjectsPage viewProjectsPage = jira.getPageBinder().navigateToAndBind(ViewProjectsPage.class);

        return viewProjectsPage.isRowPresent(projectName);
    }

    public static void deleteProject(JiraTestedProduct jira, String projectKey)
    {
        ProjectSummaryPageTab projectSummaryPageTab = jira.getPageBinder().navigateToAndBind(ProjectSummaryPageTab.class,
                projectKey);
        long projectId = projectSummaryPageTab.getProjectId();

        jira.getPageBinder().navigateToAndBind(DeleteProjectPage.class, projectId).submitConfirm();
    }

    public static void createIssue(JiraTestedProduct jira, String projectName)
    {
        JiraHeader jiraHeader = jira.getPageBinder().navigateToAndBind(DashboardPage.class).getHeader();
        CreateIssueDialog createIssueDialog = jiraHeader.createIssue();

        createIssueDialog.selectProject(projectName);
        try
        {
            Thread.sleep(1000L);
        }
        catch (InterruptedException e)
        {
            throw new IllegalStateException(e);
        }

        createIssueDialog.fill("summary", "Missing commits fix demonstration");
        createIssueDialog.submit(DashboardPage.class);
    }
    
    public static void checkSyncProcessSuccess(PageBinder pageBinder)
    {
        waitUntilFalse(isSyncing(pageBinder));
    }

    private static TimedCondition isSyncing(PageBinder pageBinder)
    {
        AccountsPage accountsPage = pageBinder.bind(AccountsPage.class);
        List<TimedQuery<Boolean>> syncStatus = Lists.newArrayList();

        for (AccountsPageAccount account : accountsPage.getAccounts())
        {
            for (AccountsPageAccountRepository repository : account.getRepositories())
            {
                syncStatus.add(repository.isSyncing());
            }
        }

        return Conditions.or(syncStatus);
    }

    private static void sleep(long milis)
    {
        try
        {
            Thread.sleep(milis);
        } catch (InterruptedException e)
        {
            // ignore
        }
    }
}
