package com.atlassian.jira.plugins.dvcs.listener;

import com.atlassian.jira.user.ApplicationUser;

/**
 * Checks to see if this user will receive a group invite to bitbucket
 */
public interface UserInviteChecker
{
    /**
     * Checks to see if this user will receive a group invite to bitbucket
     */
    boolean willReceiveGroupInvite();

}
