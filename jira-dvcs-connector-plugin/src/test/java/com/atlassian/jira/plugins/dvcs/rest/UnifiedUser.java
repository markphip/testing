package com.atlassian.jira.plugins.dvcs.rest;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.user.ApplicationUser;

interface UnifiedUser extends User, ApplicationUser
{
}

