package com.atlassian.jira.plugins.dvcs.analytics.smartcommits.event;

public enum SmartCommitFailure
{
    AMBIGIOUS_TRANSITION("ambiguousMatch"),
    NO_MATCHING_TRANSITION("noMatch"),
    NO_VALID_TRANSITION_STATUSES("issueHasNotTransitions"),
    NO_VALID_TRANSITION_COMMAND("noCommandProvided"),
    NO_EMAIL("noEmail"),
    UNABLE_TO_MAP_TO_JIRA_USER("cantMapEmailToJiraUser"),
    MULTIPLE_JIRA_USERS_FOR_EMAIL("foundMultipleUserWithEmail");

    private String smartCommitFailure;

    SmartCommitFailure(String smartCommitFailure)
    {
        this.smartCommitFailure = smartCommitFailure;
    }

    @Override
    public String toString()
    {
        return smartCommitFailure;
    }
}
