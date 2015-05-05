package com.atlassian.jira.plugins.dvcs.rest;


import com.atlassian.jira.security.JiraAuthenticationContext;

interface JiraAuthenticationContextBridge extends JiraAuthenticationContext
{
    @Override
    UnifiedUser getLoggedInUser();
}