package com.atlassian.jira.plugins.dvcs.analytics;

import com.atlassian.event.api.EventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component ("analyticsService")
public class AnalyticsServiceImpl implements AnalyticsService
{
    @Autowired
    private EventPublisher eventPublisher;

    @Override
    public void publishInviteGroupChange(final int inviteGroupsEnabled)
    {
        DvcsInviteGroupChanged dvcsInviteGroupChanged = new DvcsInviteGroupChanged(inviteGroupsEnabled);
        eventPublisher.publish(dvcsInviteGroupChanged);
    }

    @Override
    public void publishUserCreatedThatHasInvite()
    {
        DvcsAddUserAnalyticsEvent dvcsAddUserAnalyticsEvent = new DvcsAddUserAnalyticsEvent();
        eventPublisher.publish(dvcsAddUserAnalyticsEvent);

    }

    @Override
    public void publishInviteSent()
    {
        DvcsUserBitbucketInviteSent dvcsUserInviteEmailSent = new DvcsUserBitbucketInviteSent();
        eventPublisher.publish(dvcsUserInviteEmailSent);
    }
}
