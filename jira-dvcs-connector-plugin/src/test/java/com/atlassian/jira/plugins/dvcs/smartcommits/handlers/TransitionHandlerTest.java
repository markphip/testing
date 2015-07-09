package com.atlassian.jira.plugins.dvcs.smartcommits.handlers;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.issue.IssueInputParametersImpl;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.SmartCommitsAnalyticsService;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitCommandType;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitFailure;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.workflow.JiraWorkflow;
import com.atlassian.jira.workflow.WorkflowManager;
import com.google.common.collect.ImmutableList;
import com.opensymphony.workflow.loader.ActionDescriptor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


public class TransitionHandlerTest
{
    @Mock
    private IssueService issueService;

    @Mock
    private WorkflowManager workflowManager;

    @Mock
    private JiraAuthenticationContext jiraAuthenticationContext;

    @Mock
    private SmartCommitsAnalyticsService analyticsService;

    @Mock
    private ApplicationUser user;

    @Mock
    private MutableIssue issue;

    @Mock
    private I18nHelper i18nHelper;

    @Mock
    private JiraWorkflow jiraWorkflow;

    @Mock
    private ActionDescriptor firstActionDescriptor;

    @Mock
    private ActionDescriptor secondActionDescriptor;

    @Mock
    private IssueService.TransitionValidationResult transitionResult;

    @Mock
    private IssueService.IssueResult issueResult;

    @Mock
    private com.atlassian.jira.util.ErrorCollection errorCollection;

    private String commandName = "Done";

    private List<String> args = ImmutableList.of(commandName);

    private Date commitDate;

    private static final String ISSUE_KEY = "ISSUE-1";
    private static final long ISSUE_ID = 1;
    private static final int ACTION_ID = 1;

    @InjectMocks
    TransitionHandler classUnderTest;

    @Rule
    public final MethodRule initMockito = MockitoJUnit.rule();

    @Before
    public void setup()
    {
        when(issue.getKey()).thenReturn(ISSUE_KEY);
        when(firstActionDescriptor.getId()).thenReturn(ACTION_ID);
        when(secondActionDescriptor.getId()).thenReturn(ACTION_ID);
        when(jiraAuthenticationContext.getI18nHelper()).thenReturn(i18nHelper);
        when(workflowManager.getWorkflow(issue)).thenReturn(jiraWorkflow);
        when(issueResult.getErrorCollection()).thenReturn(errorCollection);
        when(issueService.transition(any(ApplicationUser.class), any(IssueService.TransitionValidationResult.class))).thenReturn(issueResult);
        when(transitionResult.getIssue()).thenReturn(issue);
    }

    @Test
    public void illegalCommandName()
    {
        commandName = "";
        classUnderTest.handle(user, issue, commandName, args, commitDate);

        verify(analyticsService).fireSmartCommitOperationFailed(SmartCommitCommandType.TRANSITION, SmartCommitFailure.NO_VALID_TRANSITION_COMMAND);
        verifyNoMoreInteractions(analyticsService);
    }


    @Test
    public void noValidActionsForIssue()
    {
        when(jiraWorkflow.getAllActions()).thenReturn(new ArrayList<>());

        classUnderTest.handle(user, issue, commandName, args, commitDate);

        verify(analyticsService).fireSmartCommitOperationFailed(SmartCommitCommandType.TRANSITION, SmartCommitFailure.NO_VALID_TRANSITION_STATUSES);
        verifyNoMoreInteractions(analyticsService);
    }

    @Test
    public void noMatchingActions()
    {
        when(jiraWorkflow.getAllActions()).thenReturn(ImmutableList.of(firstActionDescriptor));
        when(firstActionDescriptor.getName()).thenReturn("");
        setupValidTransitions();

        classUnderTest.handle(user, issue, commandName, args, commitDate);

        verify(analyticsService).fireSmartCommitOperationFailed(SmartCommitCommandType.TRANSITION, SmartCommitFailure.NO_MATCHING_TRANSITION);
        verifyNoMoreInteractions(analyticsService);
    }

    @Test
    public void multipleMatchingActions()
    {
        when(jiraWorkflow.getAllActions()).thenReturn(ImmutableList.of(firstActionDescriptor, secondActionDescriptor));
        setupTwoMatchingDescriptors();
        setupValidTransitions();

        classUnderTest.handle(user, issue, commandName, args, commitDate);

        verify(analyticsService).fireSmartCommitOperationFailed(SmartCommitCommandType.TRANSITION, SmartCommitFailure.AMBIGIOUS_TRANSITION);
        verifyNoMoreInteractions(analyticsService);
    }

    @Test
    public void exactlyOneMatchingActionError()
    {
        when(jiraWorkflow.getAllActions()).thenReturn(ImmutableList.of(firstActionDescriptor, secondActionDescriptor));
        setupOneMatchingDescriptor();
        setupValidTransitions();
        when(issueResult.isValid()).thenReturn(false);

        classUnderTest.handle(user, issue, commandName, args, commitDate);

        verify(analyticsService).fireSmartCommitOperationFailed(SmartCommitCommandType.TRANSITION);
        verifyNoMoreInteractions(analyticsService);
    }

    @Test
    public void exactlyOneMatchingActionSuccess()
    {
        when(jiraWorkflow.getAllActions()).thenReturn(ImmutableList.of(firstActionDescriptor, secondActionDescriptor));
        setupOneMatchingDescriptor();
        setupValidTransitions();
        when(issueResult.isValid()).thenReturn(true);

        classUnderTest.handle(user, issue, commandName, args, commitDate);

        verify(analyticsService).fireSmartCommitTransitionReceived(issue);
        verifyNoMoreInteractions(analyticsService);
    }

    private void setupOneMatchingDescriptor()
    {
        when(firstActionDescriptor.getName()).thenReturn("Done");
        when(secondActionDescriptor.getName()).thenReturn("TODO");
    }

    private void setupTwoMatchingDescriptors()
    {
        when(firstActionDescriptor.getName()).thenReturn("DoneDone");
        when(secondActionDescriptor.getName()).thenReturn("DoneNot");
    }

    private void setupValidTransitions()
    {
        when(issueService.validateTransition(any(ApplicationUser.class), any(Long.class), any(Integer.class), any(IssueInputParametersImpl.class))).thenReturn(transitionResult);
        when(transitionResult.isValid()).thenReturn(true);
    }

}

