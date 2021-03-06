package com.atlassian.jira.plugins.dvcs.dao.impl.querydsl;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.BranchMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.OrganizationMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryMapping;
import com.atlassian.jira.plugins.dvcs.model.Branch;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.java.ao.test.jdbc.NonTransactional;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;

import static com.atlassian.jira.plugins.dvcs.dao.impl.DAOConstants.MAXIMUM_ENTITIES_PER_ISSUE_KEY;
import static com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator.BITBUCKET;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * This is a database integration test that uses a working database and connection.
 */
public class BranchDaoQueryDslGetByIssueKeyTest extends QueryDSLDatabaseTest
{
    @Test
    public void testCallsAOWhenDarkFeatureIsUnavailable()
    {
        when(queryDslFeatureHelper.isRetrievalUsingQueryDslDisabled()).thenReturn(true);
        final List<Branch> returnList = ImmutableList.of();
        when(branchDao.getBranchesForIssue(ISSUE_KEYS, BITBUCKET)).thenReturn(returnList);

        branchDaoQueryDsl.getBranchesForIssue(ISSUE_KEYS, BITBUCKET);

        verify(branchDao).getBranchesForIssue(eq(ISSUE_KEYS), eq(BITBUCKET));
    }

    @Test
    @NonTransactional
    public void testSimpleSearchMapsProperly() throws Exception
    {
        List<Branch> branches = branchDaoQueryDsl.getBranchesForIssue(ISSUE_KEYS, BITBUCKET);

        assertThat(branches.size(), equalTo(1));

        checkForDefaultBranchMapping(branches.get(0));
    }

    @Test
    @NonTransactional
    public void testSimpleSearchWithNoDVCSType() throws Exception
    {
        List<Branch> branches = branchDaoQueryDsl.getBranchesForIssue(ISSUE_KEYS, null);

        assertThat(branches.size(), equalTo(1));

        checkForDefaultBranchMapping(branches.get(0));
    }

    private void checkForDefaultBranchMapping(Branch retrievedBranch)
    {
        assertThat(retrievedBranch.getRepositoryId(), equalTo(branchMappingWithIssue.getRepository().getID()));
        assertThat(retrievedBranch.getName(), equalTo(branchMappingWithIssue.getName()));

        assertThat(retrievedBranch.getIssueKeys(), containsInAnyOrder(ISSUE_KEY));
    }

    @Test
    @NonTransactional
    public void testTwoIssueKeys()
    {
        final String secondKey = "SCN-2";
        branchAOPopulator.associateWithIssue(branchMappingWithIssue, secondKey);

        List<Branch> branches = branchDaoQueryDsl.getBranchesForIssue(Lists.newArrayList(ISSUE_KEY, secondKey), BITBUCKET);

        assertThat(branches.size(), equalTo(1));

        Branch branch = branches.get(0);
        assertThat(branch.getIssueKeys(), containsInAnyOrder(ISSUE_KEY, secondKey));
    }

    @Test
    @NonTransactional
    public void testSimpleSearchMapsProperlyAcrossRepositoryAndOrg() throws Exception
    {
        OrganizationMapping org2 = organizationAOPopulator.create("Github", "gitbhu.", "gh fork");
        RepositoryMapping repo2 = repositoryAOPopulator.createRepository(org2, false, true, "fh/fork");
        branchAOPopulator.createBranch(branchMappingWithIssue.getName(), "other key", repo2);

        List<Branch> branches = branchDaoQueryDsl.getBranchesForIssue(ISSUE_KEYS, BITBUCKET);

        assertThat(branches.size(), equalTo(1));
        assertThat(branches.get(0).getId(), equalTo(branchMappingWithIssue.getID()));
    }

    @Test
    @NonTransactional
    public void testWithTwoBranchesTwoKeys() throws Exception
    {
        final String secondIssueKey = "IK-2";
        BranchMapping secondBranch = branchAOPopulator.createBranch("something else", secondIssueKey, enabledRepository);

        List<Branch> branches = branchDaoQueryDsl.getBranchesForIssue(Arrays.asList(ISSUE_KEY, secondIssueKey), BITBUCKET);

        assertThat(branches.size(), equalTo(2));

        Collection<Integer> branchIds = Collections2.transform(branches, new Function<Branch, Integer>()
        {
            @Override
            public Integer apply(@Nullable final Branch input)
            {
                return input.getId();
            }
        });

        assertThat(branchIds, containsInAnyOrder(branchMappingWithIssue.getID(), secondBranch.getID()));
    }

    @Test
    @NonTransactional
    public void testWithMoreBranchesThanLimit() throws Exception
    {
        for (int i = 0; i < MAXIMUM_ENTITIES_PER_ISSUE_KEY; i++)
        {
            branchAOPopulator.createBranch(ISSUE_KEY + "_branch_no_" + i, ISSUE_KEY, enabledRepository);
        }

        List<Branch> branches = branchDaoQueryDsl.getBranchesForIssue(Arrays.asList(ISSUE_KEY), BITBUCKET);

        assertThat(branches.size(), equalTo(MAXIMUM_ENTITIES_PER_ISSUE_KEY));
    }

    @Test
    @NonTransactional
    public void testEmptyIssueKeys() throws Exception
    {
        List<Branch> branches = branchDaoQueryDsl.getBranchesForIssue(ImmutableList.<String>of(), BITBUCKET);
        assertThat(branches.size(), equalTo(0));
    }
}
