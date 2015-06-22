package com.atlassian.jira.plugins.dvcs.dao.impl.querydsl;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.OrganizationMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryMapping;
import com.atlassian.jira.plugins.dvcs.activity.PullRequestParticipantMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.model.Participant;
import com.atlassian.jira.plugins.dvcs.model.PullRequest;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.java.ao.test.jdbc.NonTransactional;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator.BITBUCKET;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * This is a database integration test that uses the AO database test parent class to provide us with a working database
 * and connection.
 */
public class PullRequestDaoQueryDslGetByIssueKeyTest extends QueryDSLDatabaseTest
{
    @Test
    @NonTransactional
    public void testSimpleSearchMapsProperly() throws Exception
    {
        List<PullRequest> pullRequests = pullRequestDaoQueryDsl.getByIssueKeys(ISSUE_KEYS, BITBUCKET);

        assertThat(pullRequests.size(), equalTo(1));

        assertAgainstDefaultPR(pullRequests.get(0));
    }

    @Test
    @NonTransactional
    public void testSimpleSearchWorksOnNullDVCS() throws Exception
    {
        List<PullRequest> pullRequests = pullRequestDaoQueryDsl.getByIssueKeys(ISSUE_KEYS, null);

        assertThat(pullRequests.size(), equalTo(1));

        assertAgainstDefaultPR(pullRequests.get(0));
    }

    private void assertAgainstDefaultPR(PullRequest pullRequest)
    {
        assertPullRequestMatchesAO(pullRequest);

        assertThat(pullRequest.getIssueKeys(), containsInAnyOrder(ISSUE_KEY));

        assertThat(pullRequest.getParticipants().size(), equalTo(1));
        Participant participant = pullRequest.getParticipants().get(0);
        PullRequestParticipantMapping defaultParticipant = pullRequestMappingWithIssue.getParticipants()[0];

        assertThat(participant.getUsername(), equalTo(defaultParticipant.getUsername()));
        assertThat(participant.isApproved(), equalTo(defaultParticipant.isApproved()));
        assertThat(participant.getRole(), equalTo(defaultParticipant.getRole()));
    }

    @Test
    @NonTransactional
    public void testTwoIssueKeys()
    {
        final String secondKey = "SCN-2";
        pullRequestAOPopulator.associateToIssue(pullRequestMappingWithIssue, secondKey);

        List<PullRequest> pullRequests = pullRequestDaoQueryDsl.getByIssueKeys(Lists.newArrayList(ISSUE_KEY, secondKey), BITBUCKET);

        assertThat(pullRequests.size(), equalTo(1));

        PullRequest pullRequest = pullRequests.get(0);
        assertThat(pullRequest.getIssueKeys(), containsInAnyOrder(ISSUE_KEY, secondKey));
    }

    @Test
    @NonTransactional
    public void testSimpleSearchMapsProperlyAcrossRepositoryAndOrg() throws Exception
    {
        OrganizationMapping org2 = organizationAOPopulator.create("Github", "gitbhu.", "gh fork");
        RepositoryMapping repo2 = repositoryAOPopulator.createRepository(org2, false, true, "fh/fork");
        pullRequestAOPopulator.createPR("something else", "other key", repo2);

        List<PullRequest> pullRequests = pullRequestDaoQueryDsl.getByIssueKeys(ISSUE_KEYS, BITBUCKET);

        assertThat(pullRequests.size(), equalTo(1));
    }

    @Test
    @NonTransactional
    public void testWithTwoParticipants() throws Exception
    {
        final String user2 = "bill";
        pullRequestAOPopulator.createParticipant(user2, true, "someguy", pullRequestMappingWithIssue);

        List<PullRequest> pullRequests = pullRequestDaoQueryDsl.getByIssueKeys(ISSUE_KEYS, BITBUCKET);

        assertThat(pullRequests.size(), equalTo(1));

        PullRequest pullRequest = pullRequests.get(0);

        assertPullRequestMatchesAO(pullRequest);

        final List<Participant> participants = pullRequest.getParticipants();
        assertThat(participants.size(), equalTo(2));
        assertThat(Lists.newArrayList(participants.get(0).getUsername(), participants.get(1).getUsername()), containsInAnyOrder(user2, pullRequestParticipant.getUsername()));
    }

    @Test
    @NonTransactional
    public void testWithNoParticipant() throws Exception
    {
        final String secondIssueKey = "IK-2";
        RepositoryPullRequestMapping secondPR = pullRequestAOPopulator.createPR("something else", secondIssueKey, enabledRepository);

        List<PullRequest> pullRequests = pullRequestDaoQueryDsl.getByIssueKeys(Arrays.asList(secondIssueKey), BITBUCKET);

        assertThat(pullRequests.size(), equalTo(1));
        assertThat(pullRequests.get(0).getId(), equalTo(secondPR.getID()));
    }

    @Test
    @NonTransactional
    public void testWithTwoPRsTwoKeys() throws Exception
    {
        final String secondIssueKey = "IK-2";
        RepositoryPullRequestMapping secondPullRequest = pullRequestAOPopulator.createPR("something else", secondIssueKey, enabledRepository);

        List<PullRequest> pullRequests = pullRequestDaoQueryDsl.getByIssueKeys(Arrays.asList(ISSUE_KEY, secondIssueKey), BITBUCKET);

        assertThat(pullRequests.size(), equalTo(2));
        Collection<Integer> pullRequestIds = Collections2.transform(pullRequests, new Function<PullRequest, Integer>()
        {
            @Override
            public Integer apply(@Nullable final PullRequest input)
            {
                return input.getId();
            }
        });
        assertThat(pullRequestIds, containsInAnyOrder(pullRequestMappingWithIssue.getID(), secondPullRequest.getID()));
    }

    private void assertPullRequestMatchesAO(@Nonnull final PullRequest pullRequest)
    {
        assertThat(pullRequest.getRemoteId(), equalTo(pullRequestMappingWithIssue.getRemoteId()));
        assertThat(pullRequest.getRepositoryId(), equalTo(pullRequestMappingWithIssue.getToRepositoryId()));
        assertThat(pullRequest.getName(), equalTo(pullRequestMappingWithIssue.getName()));
        assertThat(pullRequest.getUrl(), equalTo(pullRequestMappingWithIssue.getUrl()));
        assertThat(pullRequest.getStatus().name(), equalTo(pullRequestMappingWithIssue.getLastStatus()));
        assertThat(pullRequest.getCreatedOn(), equalTo(pullRequestMappingWithIssue.getCreatedOn()));
        assertThat(pullRequest.getUpdatedOn(), equalTo(pullRequestMappingWithIssue.getUpdatedOn()));
        assertThat(pullRequest.getAuthor(), equalTo(pullRequestMappingWithIssue.getAuthor()));
        assertThat(pullRequest.getCommentCount(), equalTo(pullRequestMappingWithIssue.getCommentCount()));
        assertThat(pullRequest.getExecutedBy(), equalTo(pullRequestMappingWithIssue.getExecutedBy()));
    }

    @Test
    @NonTransactional
    public void testNoIssueKeysSupplied() throws Exception
    {
        List<PullRequest> pullRequests = pullRequestDaoQueryDsl.getByIssueKeys(ImmutableList.<String>of(), BITBUCKET);
        assertThat(pullRequests.size(), equalTo(0));
    }
    
    @Test
    @NonTransactional
    public void participantsShouldBeSortedInAscendingAlphaOrder()
    {
        // setup
        final String username1 = "username 1";
        final String username2 = "username 2";
        final String username3 = "username 3";
        final List<String> expectedUsernames = Lists.newArrayList(username1, username2, username3);
        
        final String secondIssueKey = "IK-2";
        RepositoryPullRequestMapping secondPR = pullRequestAOPopulator.createPR("something else", secondIssueKey, enabledRepository);
        pullRequestAOPopulator.createParticipant(username2, true, "reviewer", secondPR);
        pullRequestAOPopulator.createParticipant(username3, true, "reviewer", secondPR);
        pullRequestAOPopulator.createParticipant(username1, true, "reviewer", secondPR);

        // execute
        List<PullRequest> pullRequests = pullRequestDaoQueryDsl.getByIssueKeys(Arrays.asList(secondIssueKey), BITBUCKET);
        
        // check there are 3 participants
        final PullRequest pullRequest = pullRequests.get(0);
        final List<Participant> participants = pullRequest.getParticipants();
        assertThat("There should be three participants", participants.size(), is(3));
        
        // check they are ordered correctly
        final List<String> usernames = transformParticipantsToUsernames(pullRequest.getParticipants());
        assertThat("The participants should be in ascending alpha order of username", usernames, is(expectedUsernames));
    }
    
    private List<String> transformParticipantsToUsernames(@Nonnull final List<Participant> participants)
    {
        return Lists.transform(participants, new Function<Participant, String>()
        {
            @Override
            public String apply(final Participant p)
            {
                return p.getUsername();
            }
        });
    }
}
