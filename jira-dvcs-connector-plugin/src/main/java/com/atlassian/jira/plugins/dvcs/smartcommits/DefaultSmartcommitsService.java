package com.atlassian.jira.plugins.dvcs.smartcommits;

import com.atlassian.crowd.embedded.api.CrowdService;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.crowd.search.EntityDescriptor;
import com.atlassian.crowd.search.builder.QueryBuilder;
import com.atlassian.crowd.search.builder.Restriction;
import com.atlassian.crowd.search.query.entity.EntityQuery;
import com.atlassian.crowd.search.query.entity.restriction.constants.UserTermKeys;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.issue.worklog.Worklog;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.SmartCommitsAnalyticsService;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitCommandType;
import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event.SmartCommitFailure;
import com.atlassian.jira.plugins.dvcs.smartcommits.handlers.CommentHandler;
import com.atlassian.jira.plugins.dvcs.smartcommits.handlers.TransitionHandler;
import com.atlassian.jira.plugins.dvcs.smartcommits.handlers.WorkLogHandler;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.CommandsResults;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.CommandsResults.CommandResult;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.CommitCommands;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.CommitHookHandlerError;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.Either;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.ApplicationUsers;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.ws.rs.core.CacheControl;

import static com.google.common.base.Preconditions.checkNotNull;

@ExportAsService (SmartcommitsService.class)
@Component
public class DefaultSmartcommitsService implements SmartcommitsService
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(DefaultSmartcommitsService.class);

    private final CacheControl NO_CACHE;

    private final TransitionHandler transitionHandler;
    private final CommentHandler commentHandler;
    private final WorkLogHandler workLogHandler;

    private final IssueManager issueManager;
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final CrowdService crowdService;
    private final SmartCommitsAnalyticsService analyticsService;
    private final I18nHelper i18nHelper;

    private final String NO_EMAIL_IN_CHANGESET = "com.atlassian.jira.plugins.dvcs.smartcommits.email.missing";
    private final String CANT_FIND_USER = "com.atlassian.jira.plugins.dvcs.smartcommits.email.cant.find.user";
    private final String MULTIPLE_USER_MATCHES = "com.atlassian.jira.plugins.dvcs.smartcommits.email.multiple.matches";
    private final String NO_COMMANDS = "com.atlassian.jira.plugins.dvcs.smartcommits.no.commands";
    private final String ISSUE_NOT_FOUND = "com.atlassian.jira.plugins.dvcs.smartcommits.commands.issue.not.found";
    private final String INVALID_COMMAND = "com.atlassian.jira.plugins.dvcs.smartcommits.commands.invalid";

    @Autowired
    public DefaultSmartcommitsService(@ComponentImport IssueManager issueManager,
            @Qualifier ("smartcommitsTransitionsHandler") TransitionHandler transitionHandler,
            @Qualifier ("smartcommitsCommentHandler") CommentHandler commentHandler,
            @Qualifier ("smartcommitsWorklogHandler") WorkLogHandler workLogHandler,
            @ComponentImport JiraAuthenticationContext jiraAuthenticationContext,
            @ComponentImport CrowdService crowdService,
            final SmartCommitsAnalyticsService analyticsService,
            final I18nHelper i18nHelper)
    {
        this.analyticsService = checkNotNull(analyticsService);
        this.crowdService = checkNotNull(crowdService);

        NO_CACHE = new CacheControl();
        NO_CACHE.setNoCache(true);

        this.issueManager = checkNotNull(issueManager);
        this.transitionHandler = transitionHandler;
        this.commentHandler = commentHandler;
        this.workLogHandler = workLogHandler;
        this.jiraAuthenticationContext = checkNotNull(jiraAuthenticationContext);
        this.i18nHelper = i18nHelper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CommandsResults doCommands(CommitCommands commands)
    {
        Set<SmartCommitCommandType> commandTypesPresent = commandTypesInCommands(commands);
        analyticsService.fireSmartCommitReceived(commandTypesPresent);
        CommandsResults results = new CommandsResults();

        //
        // recognise user and auth user by email
        //
        String authorEmail = commands.getAuthorEmail();
        String authorName = commands.getAuthorName();
        if (StringUtils.isBlank(authorEmail))
        {


            results.addGlobalError(i18nHelper.getText(NO_EMAIL_IN_CHANGESET));
            analyticsService.fireSmartCommitFailed(SmartCommitFailure.NO_EMAIL);
            return results;
        }

        //
        // Fetch user by email
        //
        List<ApplicationUser> users = getUserByEmailOrNull(authorEmail, authorName);
        if (users.isEmpty())
        {
            results.addGlobalError(i18nHelper.getText(CANT_FIND_USER, authorEmail));
            analyticsService.fireSmartCommitFailed(SmartCommitFailure.UNABLE_TO_MAP_TO_JIRA_USER);
            return results;
        }
        else if (users.size() > 1)
        {

            results.addGlobalError(i18nHelper.getText(MULTIPLE_USER_MATCHES, authorEmail));
            analyticsService.fireSmartCommitFailed(SmartCommitFailure.MULTIPLE_JIRA_USERS_FOR_EMAIL);
            return results;
        }

        ApplicationUser user = users.get(0);

        //
        // Authenticate user
        //
        jiraAuthenticationContext.setLoggedInUser(user);

        if (CollectionUtils.isEmpty(commands.getCommands()))
        {
            results.addGlobalError(i18nHelper.getText(NO_COMMANDS));
            return results;
        }

        //
        // finally we can process commands
        //
        log.debug("Processing commands : " + commands);

        processCommands(commands, results, user);

        log.debug("Processing commands results : " + results);

        if (results.hasErrors())
        {
            analyticsService.fireSmartCommitFailed();
        }
        else
        {
            analyticsService.fireSmartCommitSucceeded(commandTypesPresent);
        }

        return results;
    }

    private void processCommands(CommitCommands commands, CommandsResults results, ApplicationUser user)
    {
        for (CommitCommands.CommitCommand command : commands.getCommands())
        {
            CommandType commandType = CommandType.getCommandType(command.getCommandName());
            //
            // init command result
            //
            CommandResult commandResult = new CommandResult();
            results.addResult(command, commandResult);

            MutableIssue issue = issueManager.getIssueObject(command.getIssueKey());
            if (issue == null)
            {
                commandResult.addError(i18nHelper.getText(ISSUE_NOT_FOUND, command.getIssueKey()));
                continue;
            }

            switch (commandType)
            {
                // -----------------------------------------------------------------------------------------------
                // Log Work
                // -----------------------------------------------------------------------------------------------
                case LOG_WORK:
                    Either<CommitHookHandlerError, Worklog> logResult = workLogHandler.handle(user, issue,
                            command.getCommandName(), command.getArguments(), commands.getCommitDate());

                    if (logResult.hasError())
                    {
                        analyticsService.fireSmartCommitOperationFailed(SmartCommitCommandType.LOG_WORK);
                        commandResult.addError(logResult.getError() + "");
                    }
                    break;
                // -----------------------------------------------------------------------------------------------
                // Comment
                // -----------------------------------------------------------------------------------------------
                case COMMENT:
                    Either<CommitHookHandlerError, Comment> commentResult = commentHandler.handle(user, issue,
                            command.getCommandName(), command.getArguments(), commands.getCommitDate());

                    if (commentResult.hasError())
                    {
                        analyticsService.fireSmartCommitOperationFailed(SmartCommitCommandType.COMMENT);
                        commandResult.addError(commentResult.getError() + "");
                    }
                    break;
                // -----------------------------------------------------------------------------------------------
                // Transition
                // -----------------------------------------------------------------------------------------------
                case TRANSITION:
                    Either<CommitHookHandlerError, Issue> transitionResult = transitionHandler.handle(user, issue,
                            command.getCommandName(), command.getArguments(), commands.getCommitDate());

                    if (transitionResult.hasError())
                    {
                        commandResult.addError(transitionResult.getError() + "");
                    }
                    break;

                default:
                    commandResult.addError(i18nHelper.getText(INVALID_COMMAND,command.getCommandName()));
            }
        }
    }

    private List<ApplicationUser> getUserByEmailOrNull(String email, String name)
    {
        return ApplicationUsers.from(getCrowdUserByEmailOrNull(email, name));
    }

    private List<User> getCrowdUserByEmailOrNull(String email, String name)
    {
        try
        {
            List<User> users = Lists.newArrayList();
            EntityQuery<User> query = QueryBuilder.queryFor(User.class, EntityDescriptor.user())
                    .with(Restriction.on(UserTermKeys.EMAIL).exactlyMatching(email)).returningAtMost(EntityQuery.ALL_RESULTS);

            Iterable<User> user = crowdService.search(query);
            Iterator<User> iterator = user.iterator();
            User firstShouldBeOneUser = iterator.next();
            users.add(firstShouldBeOneUser);
            log.debug("Found {} by email {}", new Object[] { firstShouldBeOneUser.getName(), firstShouldBeOneUser.getEmailAddress() });

            if (iterator.hasNext())
            {
                // try to find map user according the name
                while (iterator.hasNext())
                {
                    User nextUser = iterator.next();
                    if (nextUser.getName().equals(name))
                    {
                        return Collections.singletonList(nextUser);
                    }
                    users.add(nextUser);
                }
                log.warn("Found more than one user by email {} but no one is {}.", new Object[] { email, name });
                return users;
            }

            return Collections.singletonList(firstShouldBeOneUser);

        }
        catch (Exception e)
        {
            log.warn("User not found by email {}.", email);
            return Collections.EMPTY_LIST;
        }
    }

    private Set<SmartCommitCommandType> commandTypesInCommands(CommitCommands commitCommands)
    {
        List<CommitCommands.CommitCommand> commands = commitCommands.getCommands();

        Set<SmartCommitCommandType> commandTypesPresent = new HashSet<>();
        for (CommitCommands.CommitCommand command : commands)
        {
            String commandName = CommandType.getCommandType(command.getCommandName()).getName();
            SmartCommitCommandType smartCommitCommandType = SmartCommitCommandType.valueOf(commandName.toUpperCase());
            commandTypesPresent.add(smartCommitCommandType);
        }
        return commandTypesPresent;
    }

}
