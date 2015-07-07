package com.atlassian.jira.plugins.dvcs.analytics;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.dvcs.analytics.event.DvcsAddUserAnalyticsEvent;
import com.atlassian.jira.plugins.dvcs.analytics.event.DvcsInviteGroupChanged;
import com.atlassian.jira.plugins.dvcs.analytics.event.DvcsUserBitbucketInviteSent;
import com.google.common.base.Preconditions;
import org.springframework.stereotype.Component;
import javax.inject.Inject;

@Component ("analyticsService")
public class AnalyticsServiceImpl implements AnalyticsService
{
    private EventPublisher eventPublisher;

    @Inject
    public AnalyticsServiceImpl(final EventPublisher eventPublisher){
        Preconditions.checkNotNull(eventPublisher);
        this.eventPublisher = eventPublisher;
    }

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
