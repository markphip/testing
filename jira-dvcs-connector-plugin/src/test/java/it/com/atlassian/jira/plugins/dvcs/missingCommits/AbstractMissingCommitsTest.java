package it.com.atlassian.jira.plugins.dvcs.missingCommits;

import com.atlassian.jira.plugins.dvcs.base.resource.TimestampNameTestResource;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BaseConfigureOrganizationsPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitBucketConfigureOrganizationsPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.JiraPageUtils;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.OAuthCredentials;
import com.atlassian.jira.plugins.dvcs.remoterestpoint.PostCommitHookCallSimulatingRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.util.PasswordUtil;
import com.google.common.collect.Lists;
import it.com.atlassian.jira.plugins.dvcs.BaseOrganizationTest;
import it.restart.com.atlassian.jira.plugins.dvcs.common.OAuth;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPageAccount;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPageAccountRepository;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * @author Martin Skurla
 */
public abstract class AbstractMissingCommitsTest<T extends BaseConfigureOrganizationsPage> extends BaseOrganizationTest<T>
{
    private static final Logger log = LoggerFactory.getLogger(AbstractMissingCommitsTest.class);
    static final String DVCS_REPO_OWNER = "dvcsconnectortest";
    static final String DVCS_REPO_PASSWORD = PasswordUtil.getPassword("dvcsconnectortest");
    protected static final String MISSING_COMMITS_REPOSITORY_NAME_PREFIX = "missingcommitstest";
    private static final int MISSING_COMMITS_REPOSITORY_EXPIRATION_DURATION = 30 * 60 * 1000;

    private static final String JIRA_PROJECT_NAME_AND_KEY = "MC"; // Missing Commits
    public static final String SYNC_FAILED_MESSAGE = "Sync Failed";
    protected OAuth oAuth;
    protected TimestampNameTestResource timestampNameTestResource = new TimestampNameTestResource();
    private String missingCommitsRepositoryName;

    @BeforeMethod
    public void prepareRemoteDvcsRepositoryAndJiraProjectWithIssue()
    {
        removeOldDvcsRepository();
        jira.backdoor().plugins().disablePlugin("com.atlassian.jira.plugins.jira-development-integration-plugin");
        removeJiraProject();

        createRemoteDvcsRepository();
        createJiraProjectWithIssue();
    }

    @BeforeClass
    public void beforeClass()
    {
        oAuth = loginToDvcsAndGetJiraOAuthCredentials();
    }

    @AfterClass(alwaysRun = true)
    public void afterClass()
    {
        removeOAuth();
        removeRemoteDvcsRepository();
    }

    abstract void removeOldDvcsRepository();
    abstract void removeRemoteDvcsRepository();
    abstract void createRemoteDvcsRepository();

    abstract OAuth loginToDvcsAndGetJiraOAuthCredentials();
    abstract void pushToRemoteDvcsRepository(String pathToRepoZip) throws Exception;

    abstract String getFirstDvcsZipRepoPathToPush();
    abstract String getSecondDvcsZipRepoPathToPush();

    abstract void removeOAuth();

    public String getMissingCommitsRepositoryName()
    {
        if (missingCommitsRepositoryName == null)
        {
            missingCommitsRepositoryName = timestampNameTestResource.randomName(MISSING_COMMITS_REPOSITORY_NAME_PREFIX, MISSING_COMMITS_REPOSITORY_EXPIRATION_DURATION);
        }

        return missingCommitsRepositoryName;
    }

    @Test
    public void commitsIssueTab_ShouldNotMissAnyRelatedCommits() throws Exception
    {
        pushToRemoteDvcsRepository(getFirstDvcsZipRepoPathToPush());

        jira.getTester().gotoUrl(jira.getProductInstance().getBaseUrl() + configureOrganizations.getUrl());
        configureOrganizations.addOrganizationSuccessfully(DVCS_REPO_OWNER, new OAuthCredentials(oAuth.key, oAuth.secret), false);
        AccountsPageAccountRepository repository = configureOrganizations.enableAndSyncRepository(getAccountType(), DVCS_REPO_OWNER, missingCommitsRepositoryName);
        // if synchronization fails, let's try again
        if (repository.getMessage().contains(SYNC_FAILED_MESSAGE))
        {
            repository.synchronize();
        }
        assertThat(repository.getMessage()).doesNotContain(SYNC_FAILED_MESSAGE);

        assertThat(getCommitsForIssue("MC-1", 3)).hasSize(3);

        pushToRemoteDvcsRepository(getSecondDvcsZipRepoPathToPush());

        simulatePostCommitHookCall();
        JiraPageUtils.checkSyncProcessSuccess(jira); // to catch up with soft sync

        assertThat(getCommitsForIssue("MC-1", 5)).hasSize(5);

        // Remove all organizations
        jira.goTo(getConfigureOrganizationsPageClass());
        configureOrganizations.deleteAllOrganizations();
    }

    protected abstract AccountsPageAccount.AccountType getAccountType();

    public String getGitCommand()
    {
        Process process;
        try
        {
         // executing "git" without any arguent on Win OS would wait forever (even if git is correctly placed on PATH)
            // => we need to execute "git" with some argument e.g. "--version"
            process = new ProcessBuilder("git", "--version").start();
            process.waitFor();
            return "git";           // we are on windows
        } catch (Exception e)
        {
            return "/usr/local/git/bin/git";        // we are on mac/*nix
        }
    }

    public String getHgCommand()
    {
        Process process;
        try
        {
            process = new ProcessBuilder("hg", "--version").start();
            process.waitFor();
            return "hg";        // we are on windows
        } catch (Exception e)
        {
            return "/usr/local/bin/hg"; // we are on mac/*nix
        }
    }

    private void removeJiraProject()
    {
        if (JiraPageUtils.projectExists(jira, JIRA_PROJECT_NAME_AND_KEY))
        {
            JiraPageUtils.deleteProject(jira, JIRA_PROJECT_NAME_AND_KEY);
        }
    }

    private void createJiraProjectWithIssue()
    {
        jira.backdoor().project().addProject(JIRA_PROJECT_NAME_AND_KEY, JIRA_PROJECT_NAME_AND_KEY, "admin");
        jira.backdoor().issues().createIssue(JIRA_PROJECT_NAME_AND_KEY, "Missing commits fix demonstration");
    }

    private void simulatePostCommitHookCall() throws IOException
    {
        BitBucketConfigureOrganizationsPage configureOrganizationsPage =
                jira.getPageBinder().navigateToAndBind(BitBucketConfigureOrganizationsPage.class);
        String repositoryId = configureOrganizationsPage.getRepositoryIdFromRepositoryName(getMissingCommitsRepositoryName());

        PostCommitHookCallSimulatingRemoteRestpoint.simulate(jira.getProductInstance().getBaseUrl(), repositoryId);
    }

    protected void executeCommand(File workingDirectory, String... command) throws IOException, InterruptedException
    {
        log.info(Lists.newArrayList(command).toString());
        Process process = new ProcessBuilder(command).directory(workingDirectory)
                                                     .start();
        process.waitFor();
        printStream("ErrorStream", process.getErrorStream());
        printStream("InputStream", process.getInputStream());
        log.info("-----------------------------------------");
    }

    private void printStream(String title, InputStream stream) throws IOException
    {
        log.info(title + ":");
        StringWriter writer = new StringWriter();
        IOUtils.copy(stream, writer, "UTF-8");
        log.info(writer.toString());
    }
}
