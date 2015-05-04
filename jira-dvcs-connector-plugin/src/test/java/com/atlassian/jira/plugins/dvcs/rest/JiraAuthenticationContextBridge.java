package com.atlassian.jira.plugins.dvcs.rest;


import com.atlassian.jira.security.JiraAuthenticationContext;

/**
 *
 * Used for testing only
 */
interface JiraAuthenticationContextBridge extends JiraAuthenticationContext
{
    @Override
    UnifiedUser getLoggedInUser();
}