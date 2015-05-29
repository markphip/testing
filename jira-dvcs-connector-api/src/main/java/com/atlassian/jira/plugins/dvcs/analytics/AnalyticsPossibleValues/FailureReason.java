package com.atlassian.jira.plugins.dvcs.analytics.AnalyticsPossibleValues;

/**
 * Created by gtaylor on 29/05/15.
 */
public enum FailureReason
{
    OAUTH_GENERIC("oauth.generic"),
    OAUTH_SOURCECONTROL("oauth.sourcecontrol"),
    OAUTH_RESPONSE("oauth.response"),
    OAUTH_TOKEN("oauth.token"),
    OAUTH_UNAUTH("oauth.unauth"),
    VALIDATION("validation");

    private String analyticsName;

    FailureReason(String analyticsName){
        this.analyticsName = analyticsName;
    }

    @Override
    public String toString(){
        return this.analyticsName;
    }

}
