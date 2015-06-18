package com.atlassian.jira.plugins.dvcs.analytics.event;

public enum FailureReason
{
    OAUTH_GENERIC("oauth.generic"),
    OAUTH_SOURCECONTROL("oauth.sourcecontrol"),
    OAUTH_RESPONSE("oauth.response"),
    OAUTH_TOKEN("oauth.token"),
    OAUTH_UNAUTH("oauth.unauth"),
    VALIDATION("validation");

    private String failureReason;

    FailureReason(String failureReason)
    {
        this.failureReason = failureReason;
    }

    @Override
    public String toString()
    {
        return this.failureReason;
    }

}
