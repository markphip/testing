package com.atlassian.jira.plugins.dvcs.smartcommits;

import com.atlassian.crowd.embedded.api.CrowdService;
import com.atlassian.crowd.embedded.api.Query;
import com.atlassian.crowd.model.user.ImmutableUser;
import com.atlassian.crowd.model.user.User;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.mock.component.MockComponentWorker;
import com.atlassian.jira.mock.issue.MockIssue;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.SmartCommitsAnalyticsService;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitCommandType;
import com.atlassian.jira.plugins.dvcs.smartcommits.handlers.CommentHandler;
import com.atlassian.jira.plugins.dvcs.smartcommits.handlers.TransitionHandler;
import com.atlassian.jira.plugins.dvcs.smartcommits.handlers.WorkLogHandler;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.CommitCommands;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.Either;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.MockUserManager;
import com.atlassian.jira.user.util.UserManager;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class DefaultSmartcommitsServiceTest
{

    @Rule
    public final MethodRule initMockito = MockitoJUnit.rule();

    @Mock
    IssueManager issueManager;

    @Mock
    TransitionHandler transitionHandler;

    @Mock
    CommentHandler commentHandler;

    @Mock
    WorkLogHandler workLogHandler;

    @Mock
    JiraAuthenticationContext jiraAuthenticationContext;

    @Mock
    CrowdService crowdService;

    @Mock
    SmartCommitsAnalyticsService analyticsService;

    @Mock
    CommitCommands commands;

    @Mock
    Either handleSuccess;

    @Mock
    Either handleFailure;

    private MockIssue issue;

    private List<CommitCommands.CommitCommand> commandList = new ArrayList<>();

    private final Set<SmartCommitCommandType> smartCommitCommandTypesPresent =
            ImmutableSet.of(SmartCommitCommandType.COMMENT, SmartCommitCommandType.WORKLOG, SmartCommitCommandType.TRANSITION);




    @InjectMocks
    DefaultSmartcommitsService classUnderTest;

    private static final String AUTHOR_EMAIL = "TestUser@Test.com";
    private static final String AUTHOR_NAME = "TestUser";
    private static final String ISSUE_KEY = "Issue-1";

    @Before
    public void setup(){
        MockComponentWorker worker = new MockComponentWorker();
        worker.registerMock(UserManager.class, new MockUserManager());
        ComponentAccessor.initialiseWorker(worker);
        issue= new MockIssue(1,ISSUE_KEY);
        setupMatchingUsers(AUTHOR_NAME);
        setupSuccessfulSmartCommit();

        when(commands.getAuthorEmail()).thenReturn(AUTHOR_EMAIL);
        when(commands.getAuthorName()).thenReturn(AUTHOR_NAME);
        when(commands.getCommands()).thenReturn(commandList);
        when(handleSuccess.hasError()).thenReturn(false);
        when(handleFailure.hasError()).thenReturn(true);
        when(issueManager.getIssueObject(ISSUE_KEY)).thenReturn(issue);
    }

    private void setupMatchingUsers(String... names)
    {
        final List<User> users = new ArrayList<>();

        for (String name : names)
        {
            users.add(new ImmutableUser(-1L, name, name, name + "@somehwere.com", true, "John", "Smith", name));
        }

        when(crowdService.search(Matchers.<Query<User>>any())).thenReturn(users);
    }

    private CommitCommands.CommitCommand buildCommand(String command)
    {
        return new CommitCommands.CommitCommand(ISSUE_KEY, command, null);
    }



    @Test
    public void successfulMulticommandSmartCommitFiresSuccessEvent(){
        classUnderTest.doCommands(commands);

        verify(analyticsService).fireSmartCommitReceived(smartCommitCommandTypesPresent);
        verify(analyticsService).fireSmartCommitSucceeded(smartCommitCommandTypesPresent);
        verifyNoMoreInteractions(analyticsService);
    }

    @Test
    public void timeFailureMulticommandSmartCommitFiresFailureEvent(){
        setupWorkLogHandler(false);

        classUnderTest.doCommands(commands);

        verify(analyticsService).fireSmartCommitReceived(smartCommitCommandTypesPresent);
        verify(analyticsService).fireSmartCommitOperationFailed(SmartCommitCommandType.WORKLOG);
        verify(analyticsService).fireSmartCommitFailed();
        verifyNoMoreInteractions(analyticsService);
    }

    @Test
    public void commentFailureMulticommandSmartCommitFiresFailureEvent(){
        setupCommentHandler(false);

        classUnderTest.doCommands(commands);

        verify(analyticsService).fireSmartCommitReceived(smartCommitCommandTypesPresent);
        verify(analyticsService).fireSmartCommitOperationFailed(SmartCommitCommandType.COMMENT);
        verify(analyticsService).fireSmartCommitFailed();
        verifyNoMoreInteractions(analyticsService);
    }

    private void setupTransitionHandler(Boolean successful){
        Either result = successful ? handleSuccess : handleFailure;
        when(transitionHandler.handle(Matchers.<ApplicationUser>any(),
                Matchers.<MutableIssue>any(),
                Matchers.<String>any(),
                Matchers.<List<String>>any(),
                Matchers.<Date>any())).thenReturn(result);
    }

    private void setupWorkLogHandler(Boolean successful){
        Either result = successful ? handleSuccess : handleFailure;
        when(workLogHandler.handle(Matchers.<ApplicationUser>any(),
                Matchers.<MutableIssue>any(),
                Matchers.<String>any(),
                Matchers.<List<String>>any(),
                Matchers.<Date>any())).thenReturn(result);
    }

    private void setupCommentHandler(Boolean successful){
        Either result = successful ? handleSuccess : handleFailure;
        when(commentHandler.handle(Matchers.<ApplicationUser>any(),
                Matchers.<MutableIssue>any(),
                Matchers.<String>any(),
                Matchers.<List<String>>any(),
                Matchers.<Date>any())).thenReturn(result);
    }

    private void setupSuccessfulSmartCommit(){
            setupCommentHandler(true);
            commandList.add(buildCommand("comment"));
            setupWorkLogHandler(true);
            commandList.add(buildCommand("time"));
            setupTransitionHandler(true);
            commandList.add(buildCommand("transition"));
    }
}