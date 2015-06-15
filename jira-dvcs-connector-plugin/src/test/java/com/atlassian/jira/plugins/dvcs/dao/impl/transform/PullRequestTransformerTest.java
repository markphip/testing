package com.atlassian.jira.plugins.dvcs.dao.impl.transform;

import com.atlassian.jira.plugins.dvcs.activity.PullRequestParticipantMapping;
import com.atlassian.jira.plugins.dvcs.model.Participant;
import com.atlassian.jira.plugins.dvcs.model.PullRequest;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.util.RepositoryPullRequestMappingMock;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;
import javax.annotation.Nonnull;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class PullRequestTransformerTest
{
    private static final long REMOTE_ID = 234567L;
    private static final int REPO_ID = 123456;
    private static final String REPO_LAST_STATUS = "OPEN";

    private static final Participant PARTICIPANT_1 = new Participant("participant 1", true, "some role");
    private static final Participant PARTICIPANT_2 = new Participant("participant 2", false, "some other role");
    private static final Participant PARTICIPANT_3 = new Participant("participant 3", true, "some other role");
    
    private RepositoryPullRequestMappingMock repositoryPullRequestMappingMock;

    @Mock private PullRequestParticipantMapping participantMapping1;
    @Mock private PullRequestParticipantMapping participantMapping2;
    @Mock private PullRequestParticipantMapping participantMapping3;
    
    @Mock private Repository repository;
    
    @Mock private RepositoryService repositoryService;
    
    @InjectMocks
    private PullRequestTransformer pullRequestTransformer;
    
    
    @Before
    public void setup()
    {
        initMocks(this);
        
        repositoryPullRequestMappingMock = mockPullRequestMapping();
        
        setupParticipant(participantMapping1, PARTICIPANT_1);
        setupParticipant(participantMapping2, PARTICIPANT_2);
        setupParticipant(participantMapping3, PARTICIPANT_3);
    }

    private void setupParticipant(@Nonnull final PullRequestParticipantMapping participantMapping,
            @Nonnull final Participant participant)
    {
        when(participantMapping.getUsername()).thenReturn(participant.getUsername());
        when(participantMapping.getRole()).thenReturn(participant.getRole());
        when(participantMapping.isApproved()).thenReturn(participant.isApproved());
        when(participantMapping.getPullRequest()).thenReturn(repositoryPullRequestMappingMock);
    }
    
    @Test
    public void transformShouldReturnPRsWithSortedParticipants()
    {
        // setup
        repositoryPullRequestMappingMock.setParticipants(getParticipantsOutOfAlphaOrder());
        
        // execute
        final PullRequest pullRequest = pullRequestTransformer.transform(repositoryPullRequestMappingMock, false);
        
        // check
        final List<Participant> expected = Lists.newArrayList(PARTICIPANT_1, PARTICIPANT_2, PARTICIPANT_3);
        assertThat("The participants are in ascending order", pullRequest.getParticipants(), is(expected));
    }

    private PullRequestParticipantMapping[] getParticipantsOutOfAlphaOrder()
    {
        return new PullRequestParticipantMapping[]
        {
                participantMapping3,
                participantMapping1,
                participantMapping2
        };
    }

    @Nonnull
    private RepositoryPullRequestMappingMock mockPullRequestMapping()
    {
        final RepositoryPullRequestMappingMock mock = new RepositoryPullRequestMappingMock();

        mock.setRemoteId(REMOTE_ID);
        mock.setLastStatus(REPO_LAST_STATUS);
        mock.setToRepositoryId(REPO_ID);
        when(repositoryService.get(REPO_ID)).thenReturn(repository);
        
        return mock;
    }
}
