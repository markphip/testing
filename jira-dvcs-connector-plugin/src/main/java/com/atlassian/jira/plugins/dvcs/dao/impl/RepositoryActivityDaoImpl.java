package com.atlassian.jira.plugins.dvcs.dao.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.java.ao.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryCommitActivityMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryCommitCommentActivityMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryCommitCommitActivityMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryCommitIssueKeyMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryCommitMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryDomainMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestActivityMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestCommentActivityMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestIssueKeyMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestUpdateActivityMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestUpdateActivityMapping.Status;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestUpdateActivityToCommitMapping;
import com.atlassian.jira.plugins.dvcs.model.GlobalFilter;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.util.ActiveObjectsUtils;
import com.atlassian.jira.plugins.dvcs.util.IssueKeyExtractor;
import com.atlassian.jira.plugins.dvcs.util.ao.QueryTemplate;
import com.atlassian.jira.plugins.dvcs.util.ao.query.criteria.QueryCriterion;
import com.atlassian.sal.api.transaction.TransactionCallback;

/**
 * 
 * DefaultRepositoryActivityDao
 * 
 * 
 * <br />
 * <br />
 * Created on 15.1.2013, 15:17:03 <br />
 * <br />
 * 
 * @author jhocman@atlassian.com
 * 
 */
public class RepositoryActivityDaoImpl implements RepositoryActivityDao
{

    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryActivityDaoImpl.class);

    /**
     * Injected {@link ActiveObjects} dependency.
     */
    private final ActiveObjects activeObjects;

    /**
     * Collection of all tables, which holds commit related activities.
     */
    @SuppressWarnings("unchecked")
    private static final Class<RepositoryCommitActivityMapping>[] ALL_COMMIT_ACTIVITY_TABLES = new Class[] {
            RepositoryCommitCommitActivityMapping.class, RepositoryCommitCommentActivityMapping.class };

    /**
     * Collection of all tables, which holds pull request related activities.
     */
    @SuppressWarnings("unchecked")
    private static final Class<RepositoryPullRequestActivityMapping>[] ALL_PULL_REQUEST_ACTIVITY_TABLES = new Class[] { //
    //
            RepositoryPullRequestUpdateActivityMapping.class, //
            RepositoryPullRequestCommentActivityMapping.class, //
    };

    public RepositoryActivityDaoImpl(ActiveObjects activeObjects)
    {
        super();
        this.activeObjects = activeObjects;
    }

    @Override
    public RepositoryActivityMapping saveActivity(final Repository domain, final Map<String, Object> activity)
    {
        return activeObjects.executeInTransaction(new TransactionCallback<RepositoryActivityMapping>()
        {
            @Override
            @SuppressWarnings("unchecked")
            public RepositoryActivityMapping doInTransaction()
            {
                activity.put(RepositoryDomainMapping.DOMAIN, domain.getId());
                return activeObjects.create(
                        (Class<? extends RepositoryActivityMapping>) activity.remove(RepositoryActivityMapping.ENTITY_TYPE), activity);
            }

        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void linkCommit(Repository domain, RepositoryPullRequestUpdateActivityMapping activity, RepositoryCommitMapping commit)
    {
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put(RepositoryPullRequestUpdateActivityToCommitMapping.DOMAIN, domain.getId());
        params.put(RepositoryPullRequestUpdateActivityToCommitMapping.ACTIVITY, activity.getID());
        params.put(RepositoryPullRequestUpdateActivityToCommitMapping.COMMIT, commit.getID());
        activeObjects.create(RepositoryPullRequestUpdateActivityToCommitMapping.class, params);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unlinkCommit(Repository domain, RepositoryPullRequestUpdateActivityMapping activity, RepositoryCommitMapping commit)
    {
        Query query = Query.select();
        query.where(RepositoryPullRequestUpdateActivityToCommitMapping.ACTIVITY + " = ? AND "
                + RepositoryPullRequestUpdateActivityToCommitMapping.COMMIT + " = ? ", activity.getID(), commit.getID());
        ActiveObjectsUtils.delete(activeObjects, RepositoryPullRequestUpdateActivityToCommitMapping.class, query);
    }

    @Override
    public RepositoryPullRequestMapping savePullRequest(final Repository domain, final Map<String, Object> request)
    {
        return activeObjects.executeInTransaction(new TransactionCallback<RepositoryPullRequestMapping>()
        {
            @Override
            public RepositoryPullRequestMapping doInTransaction()
            {
                request.put(RepositoryDomainMapping.DOMAIN, domain.getId());
                return activeObjects.create(RepositoryPullRequestMapping.class, request);
            }

        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateCommitIssueKeys(Repository domain)
    {
        // finds currently presented issue keys
        Set<String> currentIssueKeys = new HashSet<String>();
        Set<String> existingIssueKeys = new HashSet<String>();

        for (RepositoryCommitMapping commit : activeObjects.find(RepositoryCommitMapping.class,
                Query.select().where(RepositoryCommitMapping.DOMAIN + " = ? ", domain.getId())))
        {
            existingIssueKeys.addAll(getExistingIssueKeysMapping(domain, commit));
            currentIssueKeys.addAll(IssueKeyExtractor.extractIssueKeys(commit.getMessage()));

            for (RepositoryCommitCommentActivityMapping commentActivity : commit.getCommitCommentActivities())
            {
                currentIssueKeys.addAll(IssueKeyExtractor.extractIssueKeys(commentActivity.getMessage()));
            }

            Set<String> addedIssueKeys = new HashSet<String>();
            addedIssueKeys.addAll(currentIssueKeys);
            addedIssueKeys.removeAll(existingIssueKeys);

            Set<String> removedIssueKeys = new HashSet<String>();
            removedIssueKeys.addAll(existingIssueKeys);
            removedIssueKeys.removeAll(currentIssueKeys);

            for (String issueKey : removedIssueKeys)
            {
                activeObjects.delete(activeObjects.find(
                        RepositoryCommitIssueKeyMapping.class,
                        Query.select().where(
                                RepositoryCommitIssueKeyMapping.DOMAIN + " = ? AND " + RepositoryCommitIssueKeyMapping.COMMIT + " = ? AND "
                                        + RepositoryCommitIssueKeyMapping.ISSUE_KEY + " = ? ", domain.getId(), commit.getID(), issueKey)));
            }

            // adds remaining - newly presented issue keys
            Map<String, Object> params = new HashMap<String, Object>();
            for (String issueKey : addedIssueKeys)
            {
                params.put(RepositoryCommitIssueKeyMapping.DOMAIN, domain.getId());
                params.put(RepositoryCommitIssueKeyMapping.COMMIT, commit.getID());
                params.put(RepositoryCommitIssueKeyMapping.ISSUE_KEY, issueKey);
                activeObjects.create(RepositoryCommitIssueKeyMapping.class, params);
                params.clear();
            }

            currentIssueKeys.clear();
            existingIssueKeys.clear();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updatePullRequestIssueKeys(Repository domain, int pullRequestId)
    {
        RepositoryPullRequestMapping repositoryPullRequestMapping = findRequestById(pullRequestId);
        Set<String> existingIssueKeys = getExistingIssueKeysMapping(domain, pullRequestId);

        Set<String> currentIssueKeys = new HashSet<String>();
        currentIssueKeys.addAll(IssueKeyExtractor.extractIssueKeys(repositoryPullRequestMapping.getName(),
                repositoryPullRequestMapping.getDescription()));

        // commits
        {
            Query query = Query.select();
            query.where(
                    RepositoryDomainMapping.DOMAIN + " = ? AND " + RepositoryPullRequestUpdateActivityMapping.PULL_REQUEST_ID + " = ? ",
                    domain.getId(), pullRequestId);
            for (RepositoryPullRequestUpdateActivityMapping updateActivity : activeObjects.find(
                    RepositoryPullRequestUpdateActivityMapping.class, query))
            {
                for (RepositoryCommitMapping commit : updateActivity.getCommits())
                {
                    currentIssueKeys.addAll(IssueKeyExtractor.extractIssueKeys(commit.getMessage()));
                }
            }
        }

        // comments
        for (RepositoryPullRequestCommentActivityMapping comment : getPullRequestComments(domain, repositoryPullRequestMapping))
        {
            currentIssueKeys.addAll(IssueKeyExtractor.extractIssueKeys(comment.getMessage()));
        }

        // updates information to reflect current state
        Set<String> addedIssueKeys = new HashSet<String>();
        addedIssueKeys.addAll(currentIssueKeys);
        addedIssueKeys.removeAll(existingIssueKeys);

        Set<String> removedIssueKeys = new HashSet<String>();
        removedIssueKeys.addAll(existingIssueKeys);
        removedIssueKeys.removeAll(currentIssueKeys);

        // adds news one
        for (String issueKeyToAdd : addedIssueKeys)
        {
            Map<String, Object> issueKeyMapping = asIssueKeyMapping(issueKeyToAdd, repositoryPullRequestMapping.getID());
            issueKeyMapping.put(RepositoryDomainMapping.DOMAIN, domain.getId());
            activeObjects.create(RepositoryPullRequestIssueKeyMapping.class, issueKeyMapping);
        }

        // removes canceled
        for (String issueKeyToRemove : removedIssueKeys)
        {
            activeObjects.delete(activeObjects.find(
                    RepositoryPullRequestIssueKeyMapping.class,
                    Query.select().where(
                            RepositoryDomainMapping.DOMAIN + " = ? AND " + RepositoryPullRequestIssueKeyMapping.PULL_REQUEST_ID
                                    + " = ? AND " + RepositoryPullRequestIssueKeyMapping.ISSUE_KEY + " = ? ", domain.getId(),
                            repositoryPullRequestMapping.getID(), issueKeyToRemove)));
        }
    }

    private Set<String> getExistingIssueKeysMapping(Repository domain, RepositoryCommitMapping commitMapping)
    {
        Query query = Query
                .select()
                .from(RepositoryCommitIssueKeyMapping.class)
                .where(RepositoryDomainMapping.DOMAIN + " = ? AND " + RepositoryCommitIssueKeyMapping.COMMIT + " = ? ", domain.getId(),
                        commitMapping);
        RepositoryCommitIssueKeyMapping[] mappings = activeObjects.find(RepositoryCommitIssueKeyMapping.class, query);
        Set<String> issueKeys = new java.util.HashSet<String>();
        for (RepositoryCommitIssueKeyMapping repositoryCommitIssueKeyMapping : mappings)
        {
            issueKeys.add(repositoryCommitIssueKeyMapping.getIssueKey());
        }
        return issueKeys;
    }

    @Override
    public Set<String> getExistingIssueKeysMapping(Repository domain, Integer pullRequestId)
    {
        Query query = Query
                .select()
                .from(RepositoryPullRequestIssueKeyMapping.class)
                .where(RepositoryDomainMapping.DOMAIN + " = ? AND " + RepositoryPullRequestIssueKeyMapping.PULL_REQUEST_ID + " = ? ",
                        domain.getId(), pullRequestId);
        RepositoryPullRequestIssueKeyMapping[] mappings = activeObjects.find(RepositoryPullRequestIssueKeyMapping.class, query);
        Set<String> issueKeys = new java.util.HashSet<String>();
        for (RepositoryPullRequestIssueKeyMapping repositoryPullRequestIssueKeyMapping : mappings)
        {
            issueKeys.add(repositoryPullRequestIssueKeyMapping.getIssueKey());
        }
        return issueKeys;
    }

    protected Map<String, Object> asIssueKeyMapping(String issueKey, int pullRequestId)
    {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RepositoryPullRequestIssueKeyMapping.ISSUE_KEY, issueKey);
        map.put(RepositoryPullRequestIssueKeyMapping.PULL_REQUEST_ID, pullRequestId);
        return map;
    }

    @Override
    public RepositoryPullRequestMapping findRequestById(int localId)
    {
        return activeObjects.get(RepositoryPullRequestMapping.class, localId);
    }

    @Override
    public RepositoryPullRequestMapping findRequestByRemoteId(Repository domain, long remoteId)
    {
        Query query = Query
                .select()
                .from(RepositoryPullRequestMapping.class)
                .where(RepositoryPullRequestMapping.REMOTE_ID + " = ? AND " + RepositoryPullRequestMapping.DOMAIN + " = ?", remoteId,
                        domain.getId());

        RepositoryPullRequestMapping[] found = activeObjects.find(RepositoryPullRequestMapping.class, query);
        return found.length == 1 ? found[0] : null;
    }

    @Override
    public List<RepositoryActivityMapping> getRepositoryActivityForIssue(String issueKey)
    {
        List<RepositoryActivityMapping> ret = new ArrayList<RepositoryActivityMapping>();

        // processes commits
        for (final Class<RepositoryCommitActivityMapping> activityTable : ALL_COMMIT_ACTIVITY_TABLES)
        {
            List<Integer> commitIds = findRelatedCommits(issueKey);
            for (Integer commitId : commitIds)
            {
                Query query = new QueryTemplate()
                {

                    @Override
                    protected void build()
                    {
                        // from activity
                        alias(activityTable, "ACTIVITY");

                        // join commit
                        alias(RepositoryCommitMapping.class, "COMMIT");
                        join(RepositoryCommitMapping.class, column(activityTable, RepositoryCommitActivityMapping.COMMIT), "ID");

                        // activity.id = :commitId
                        where(eq(column(RepositoryCommitMapping.class, "ID"), parameter("commitId")));
                    }

                }.toQuery(Collections.<String, Object> singletonMap("commitId", commitId));

                ret.addAll(Arrays.asList(activeObjects.find(activityTable, query)));
            }
        }

        // processes pull requests
        for (final Class<RepositoryPullRequestActivityMapping> activityTable : ALL_PULL_REQUEST_ACTIVITY_TABLES)
        {
            List<Integer> pullRequestIds = findRelatedPullRequests(issueKey);
            for (Integer pullRequestId : pullRequestIds)
            {
                final Query query = new QueryTemplate()
                {

                    @Override
                    protected void build()
                    {
                        // from activity
                        alias(activityTable, "ACTIVITY");

                        // where activity.pullRequestId = :pullRequestId
                        where(eq(column(activityTable, RepositoryPullRequestActivityMapping.PULL_REQUEST_ID), parameter("pullRequestId")));
                    }

                }.toQuery(Collections.<String, Object> singletonMap("pullRequestId", pullRequestId));

                ret.addAll(Arrays.asList(activeObjects.find(activityTable, query)));
            }
        }

        return sort(ret);
    }

    // FIXME: in progress it is not finished yet
    public void getRepositoryActivityByFilter(final GlobalFilter filter)
    {
        List<RepositoryActivityMapping> result = new LinkedList<RepositoryActivityMapping>();

        for (final Class<RepositoryCommitActivityMapping> activityTable : ALL_COMMIT_ACTIVITY_TABLES)
        {
            result.addAll(Arrays.asList(activeObjects.find(activityTable, new QueryTemplate()
            {

                @Override
                protected void build()
                {
                    // from activity
                    alias(activityTable, "ACTIVITY");

                    // join commit table
                    alias(RepositoryCommitMapping.class, "COMMIT");
                    join(RepositoryCommitMapping.class, column(activityTable, RepositoryCommitActivityMapping.COMMIT), "ID");

                    // join issue key mapping
                    alias(RepositoryCommitIssueKeyMapping.class, "ISSUE_KEY");
                    join(RepositoryCommitIssueKeyMapping.class, column(RepositoryCommitMapping.class, "ID"), RepositoryCommitIssueKeyMapping.COMMIT);

                    // where conditions
                    List<QueryCriterion> and = new LinkedList<QueryCriterion>();

                    if (filter.getInProjects() != null)
                    {
                        List<QueryCriterion> or = new LinkedList<QueryCriterion>();
                        for (String inProject : filter.getInProjects()) {
                            or.add(like(column(RepositoryCommitIssueKeyMapping.class, RepositoryCommitIssueKeyMapping.ISSUE_KEY), parameter("inProject", inProject + "-%")));
                        }

                        if (!or.isEmpty())
                        {
                            and.add(or(or.toArray(new QueryCriterion[or.size()])));
                        }
                    }

                    if (filter.getInIssues() != null)
                    {
                        List<QueryCriterion> or = new LinkedList<QueryCriterion>();
                        for (String inIssue : filter.getInIssues()) {
                            or.add(eq(column(RepositoryCommitIssueKeyMapping.class, RepositoryCommitIssueKeyMapping.ISSUE_KEY), parameter("inIssueKey", inIssue)));
                        }

                        if (!or.isEmpty())
                        {
                            and.add(or(or.toArray(new QueryCriterion[or.size()])));
                        }
                    }

                    if (filter.getInUsers() != null)
                    {
                        List<QueryCriterion> or = new LinkedList<QueryCriterion>();
                        
                        if (!or.isEmpty())
                        {
                            and.add(or(or.toArray(new QueryCriterion[or.size()])));
                        }
                    }

                    and(and.toArray(new QueryCriterion[and.size()]));
                }

            }.toQuery(Collections.<String, Object> emptyMap()))));
        }
    }

    private List<Integer> findRelatedCommits(String issueKey)
    {
        List<Integer> prIds = new ArrayList<Integer>();
        final Query query = Query.select().from(RepositoryCommitIssueKeyMapping.class)
                .where(RepositoryCommitIssueKeyMapping.ISSUE_KEY + " = ?", issueKey.toUpperCase());

        RepositoryCommitIssueKeyMapping[] mappings = activeObjects.find(RepositoryCommitIssueKeyMapping.class, query);
        for (RepositoryCommitIssueKeyMapping issueKeyMapping : mappings)
        {
            prIds.add(issueKeyMapping.getCommit().getID());
        }
        return prIds;
    }

    private List<Integer> findRelatedPullRequests(String issueKey)
    {
        List<Integer> prIds = new ArrayList<Integer>();
        final Query query = Query.select().from(RepositoryPullRequestIssueKeyMapping.class)
                .where(RepositoryPullRequestIssueKeyMapping.ISSUE_KEY + " = ?", issueKey.toUpperCase());

        RepositoryPullRequestIssueKeyMapping[] mappings = activeObjects.find(RepositoryPullRequestIssueKeyMapping.class, query);
        for (RepositoryPullRequestIssueKeyMapping issueKeyMapping : mappings)
        {
            prIds.add(issueKeyMapping.getPullRequestId());
        }
        return prIds;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void removeAll(Repository domain)
    {
        for (Class<? extends RepositoryDomainMapping> entityType : new Class[] { RepositoryPullRequestUpdateActivityToCommitMapping.class,
                RepositoryPullRequestIssueKeyMapping.class, RepositoryPullRequestUpdateActivityMapping.class,
                RepositoryPullRequestCommentActivityMapping.class, RepositoryPullRequestMapping.class,
                RepositoryCommitCommentActivityMapping.class, RepositoryCommitCommitActivityMapping.class,
                RepositoryCommitIssueKeyMapping.class, RepositoryCommitMapping.class, })
        {
            ActiveObjectsUtils.delete(activeObjects, entityType,
                    Query.select().where(RepositoryDomainMapping.DOMAIN + " = ? ", domain.getId()));
        }
    }

    @Override
    public RepositoryCommitMapping saveCommit(final Repository domain, final Map<String, Object> commit)
    {
        return activeObjects.executeInTransaction(new TransactionCallback<RepositoryCommitMapping>()
        {
            @Override
            public RepositoryCommitMapping doInTransaction()
            {
                commit.put(RepositoryDomainMapping.DOMAIN, domain.getId());
                return activeObjects.create(RepositoryCommitMapping.class, commit);
            }

        });
    }

    @Override
    public RepositoryCommitMapping getCommit(Repository domain, int pullRequesCommitId)
    {
        return activeObjects.get(RepositoryCommitMapping.class, pullRequesCommitId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RepositoryPullRequestUpdateActivityMapping getPullRequestActivityByRemoteId(Repository domain,
            RepositoryPullRequestMapping pullRequest, String remoteId)
    {
        Query query = Query.select().from(RepositoryPullRequestUpdateActivityMapping.class);
        query.where(RepositoryDomainMapping.DOMAIN + " = ? AND " + RepositoryPullRequestUpdateActivityMapping.PULL_REQUEST_ID + " = ? AND "
                + RepositoryPullRequestUpdateActivityMapping.REMOTE_ID + " = ? ", domain.getId(), pullRequest.getID(), remoteId);

        RepositoryPullRequestUpdateActivityMapping[] founded = activeObjects.find(RepositoryPullRequestUpdateActivityMapping.class, query);
        if (founded.length == 1)
        {
            return founded[0];

        } else if (founded.length == 0)
        {
            return null;

        } else
        {
            LOGGER.error("There are multiple records with same pull request ID and remote ID! Pull request ID: " + pullRequest.getID()
                    + " Remote ID: " + remoteId + "First one will be used!");
            return founded[0];
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<RepositoryPullRequestUpdateActivityMapping> getPullRequestActivityByStatus(Repository domain,
            RepositoryPullRequestMapping pullRequest, Status status)
    {
        Query query = Query.select().from(RepositoryPullRequestUpdateActivityMapping.class);
        query.where(RepositoryDomainMapping.DOMAIN + " = ? AND " + RepositoryPullRequestUpdateActivityMapping.PULL_REQUEST_ID + " = ? AND "
                + RepositoryPullRequestUpdateActivityMapping.STATUS + " = ? ", domain.getId(), pullRequest.getID(), status);
        return Arrays.asList(activeObjects.find(RepositoryPullRequestUpdateActivityMapping.class, query));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<RepositoryPullRequestCommentActivityMapping> getPullRequestComments(Repository domain,
            RepositoryPullRequestMapping pullRequest)
    {
        Query query = Query.select().from(RepositoryPullRequestCommentActivityMapping.class);
        query.where(RepositoryDomainMapping.DOMAIN + " = ? AND " + RepositoryPullRequestCommentActivityMapping.PULL_REQUEST_ID + " = ? ",
                domain.getId(), pullRequest.getID());

        RepositoryPullRequestCommentActivityMapping[] founded = activeObjects
                .find(RepositoryPullRequestCommentActivityMapping.class, query);
        return Arrays.asList(founded);
    }

    @Override
    public void updateActivityStatus(Repository domain, int activityId, Status status)
    {
        RepositoryPullRequestUpdateActivityMapping activity = activeObjects.get(RepositoryPullRequestUpdateActivityMapping.class,
                activityId);
        activity.setStatus(status);
        activity.save();
    }

    @Override
    public RepositoryCommitMapping getCommitByNode(Repository domain, int pullRequestId, String node)
    {
        Query query = Query
                .select()
                .alias(RepositoryCommitMapping.class, "COMMIT")
                .alias(RepositoryPullRequestUpdateActivityToCommitMapping.class, "PR_UPDATE_TO_COMMIT")
                .alias(RepositoryPullRequestUpdateActivityMapping.class, "PR_UPDATE")
                .join(RepositoryPullRequestUpdateActivityToCommitMapping.class,
                        "COMMIT.ID = PR_UPDATE_TO_COMMIT." + RepositoryPullRequestUpdateActivityToCommitMapping.COMMIT)
                .join(RepositoryPullRequestUpdateActivityMapping.class,
                        "PR_UPDATE_TO_COMMIT." + RepositoryPullRequestUpdateActivityToCommitMapping.ACTIVITY + " = PR_UPDATE.ID")
                .where("COMMIT." + RepositoryDomainMapping.DOMAIN + " = ? AND PR_UPDATE."
                        + RepositoryPullRequestUpdateActivityMapping.PULL_REQUEST_ID + " = ? AND COMMIT." + RepositoryCommitMapping.NODE
                        + " = ?", domain.getId(), pullRequestId, node);

        RepositoryCommitMapping[] found = activeObjects.find(RepositoryCommitMapping.class, query);
        return found.length == 1 ? found[0] : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RepositoryCommitMapping getCommitByNode(Repository repository, String node)
    {
        Query query = Query.select().alias(RepositoryCommitMapping.class, "COMMIT")
                .alias(RepositoryCommitCommitActivityMapping.class, "ACTIVITY")
                .join(RepositoryCommitCommitActivityMapping.class, "COMMIT.ID = ACTIVITY." + RepositoryCommitActivityMapping.COMMIT)
                .where("ACTIVITY." + RepositoryActivityMapping.REPOSITORY_ID + " = ? AND COMMIT." //
                        + RepositoryCommitMapping.NODE + " = ?", repository.getId(), node);

        RepositoryCommitMapping[] found = activeObjects.find(RepositoryCommitMapping.class, query);
        if (found.length == 0)
        {
            return null;

        } else if (found.length == 1)
        {
            return found[0];

        } else
        {
            throw new IllegalStateException("Multiple commits for a same Commit Node and Repository ID. Repository ID: "
                    + repository.getId() + " Commit Node: " + node);

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<RepositoryCommitCommentActivityMapping> getCommitComments(Repository domain, RepositoryCommitMapping commit)
    {
        RepositoryCommitCommentActivityMapping[] founded = activeObjects.find(
                RepositoryCommitCommentActivityMapping.class,
                Query.select().where(
                        RepositoryCommitCommentActivityMapping.DOMAIN + " = ? AND " + RepositoryCommitCommentActivityMapping.COMMIT
                                + " = ? ", domain.getId(), commit.getID()));
        return Arrays.asList(founded);
    }

    // --------------------------------------------------------------------------------------------------------------------
    // --------------------------------------------------------------------------------------------------------------------
    // private helpers
    // --------------------------------------------------------------------------------------------------------------------
    // --------------------------------------------------------------------------------------------------------------------

    private List<RepositoryActivityMapping> sort(List<RepositoryActivityMapping> sortable)
    {
        Collections.sort(sortable, new Comparator<RepositoryActivityMapping>()
        {
            @Override
            public int compare(RepositoryActivityMapping o1, RepositoryActivityMapping o2)
            {
                try
                {
                    return -o1.getLastUpdatedOn().compareTo(o2.getLastUpdatedOn());
                } catch (NullPointerException e)
                {
                    return 0;
                }
            }

        });

        return sortable;
    }
}