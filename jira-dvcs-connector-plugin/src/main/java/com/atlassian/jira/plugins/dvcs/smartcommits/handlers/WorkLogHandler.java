package com.atlassian.jira.plugins.dvcs.smartcommits.handlers;

import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.issue.worklog.WorklogInputParametersImpl;
import com.atlassian.jira.bc.issue.worklog.WorklogResult;
import com.atlassian.jira.bc.issue.worklog.WorklogService;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.worklog.Worklog;
import com.atlassian.jira.plugins.dvcs.smartcommits.CommandType;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.CommitHookHandlerError;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.Either;
import com.atlassian.jira.util.lang.Pair;

public class WorkLogHandler implements CommandHandler<Worklog>
{

    private static CommandType CMD_TYPE = CommandType.LOG_WORK;

    private WorklogService worklogService;

    private Pattern IN_WORKLOG_PATTERN = Pattern.compile("((\\d+(w|d|h|m)\\s*)+)");

    public WorkLogHandler(WorklogService worklogService)
    {
        this.worklogService = worklogService;
    }

    @Override
	public CommandType getCommandType()
    {
        return CMD_TYPE;
    }

    @Override
	public Either<CommitHookHandlerError, Worklog> handle(User user, MutableIssue issue, String commandName,
            List<String> args, Date commitDate)
    {

        JiraServiceContextImpl jiraServiceContext = new JiraServiceContextImpl(user);

        String worklog = args.get(0);
        Pair<String, String> durationAndComment = splitWorklogToDurationAndComment(worklog);

        WorklogResult result = worklogService.validateCreate(
                jiraServiceContext,
                WorklogInputParametersImpl.builder().issue(issue)
                        .timeSpent(durationAndComment.first())
                        .comment(durationAndComment.second()).startDate(commitDate != null ? commitDate : new Date()).build());

        if (!jiraServiceContext.getErrorCollection().hasAnyErrors())
        {

            return Either.value(worklogService.createAndAutoAdjustRemainingEstimate(jiraServiceContext, result, true));

        } else
        {

            return Either.error(CommitHookHandlerError.fromErrorCollection(CMD_TYPE.getName(), issue.getKey(),
                    jiraServiceContext.getErrorCollection()));

        }
    }

    private Pair<String, String> splitWorklogToDurationAndComment(String worklog)
    {

        String worklogDuration = null;
        String worklogComment = "";

        Matcher matcher = IN_WORKLOG_PATTERN.matcher(worklog);
        matcher.find();

        worklogDuration = matcher.group(0).trim();

        String comment = matcher.replaceAll("");
        if (StringUtils.isNotBlank(comment))
        {
            worklogComment = comment.trim();
        }
        else
        {
            worklogComment = "";
        }

        return Pair.of(worklogDuration.trim(), worklogComment.trim());
    }

}
