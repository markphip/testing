package com.atlassian.jira.plugins.dvcs.dao.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.QueryHelper;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.IssueToChangesetMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryToChangesetMapping;
import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao;
import com.atlassian.jira.plugins.dvcs.dao.impl.transform.ChangesetTransformer;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileDetails;
import com.atlassian.jira.plugins.dvcs.model.FileData;
import com.atlassian.jira.plugins.dvcs.model.GlobalFilter;
import com.atlassian.jira.plugins.dvcs.util.ActiveObjectsUtils;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.sal.api.transaction.TransactionCallback;
import net.java.ao.EntityStreamCallback;
import net.java.ao.Query;
import net.java.ao.RawEntity;
import net.java.ao.schema.PrimaryKey;
import net.java.ao.schema.Table;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangesetDaoImpl implements ChangesetDao
{
    private static final Logger log = LoggerFactory.getLogger(ChangesetDaoImpl.class);

    private final ActiveObjects activeObjects;
    private final ChangesetTransformer transformer;
    private final QueryHelper queryHelper;

    public ChangesetDaoImpl(ActiveObjects activeObjects, QueryHelper queryHelper)
    {
        this.activeObjects = activeObjects;
        this.queryHelper = queryHelper;
        this.transformer = new ChangesetTransformer(activeObjects);
    }

    private Changeset transform(ChangesetMapping changesetMapping, int defaultRepositoryId)
    {
        return transformer.transform(changesetMapping, defaultRepositoryId, null);
    }

    private Changeset transform(ChangesetMapping changesetMapping)
    {
        return transformer.transform(changesetMapping, 0, null);
    }

    private Changeset transform(ChangesetMapping changesetMapping, String dvcsType)
    {
        return transformer.transform(changesetMapping, 0, dvcsType);
    }

    private List<Changeset> transform(List<ChangesetMapping> changesetMappings)
    {
        List<Changeset> changesets = new ArrayList<Changeset>();

        for (ChangesetMapping changesetMapping : changesetMappings)
        {
            Changeset changeset = transform(changesetMapping);
            if (changeset != null)
            {
                changesets.add(changeset);
            }
        }

        return changesets;
    }

    private List<Changeset> transform(List<ChangesetMapping> changesetMappings, String dvcsType)
    {
        List<Changeset> changesets = new ArrayList<Changeset>();

        for (ChangesetMapping changesetMapping : changesetMappings)
        {
            Changeset changeset = transform(changesetMapping, dvcsType);
            if (changeset != null)
            {
                changesets.add(changeset);
            }
        }

        return changesets;
    }

    @Override
    public void removeAllInRepository(final int repositoryId)
    {

        activeObjects.executeInTransaction(new TransactionCallback<Object>()
        {
            @Override
            public Object doInTransaction()
            {
                // todo: transaction: plugin use SalTransactionManager and there is empty implementation of TransactionSynchronisationManager.
                // todo: Therefore there are only entityCache transactions. No DB transactions.

                // delete association repo - changesets
                Query query = Query.select().where(RepositoryToChangesetMapping.REPOSITORY_ID + " = ?", repositoryId);
                log.debug("deleting repo - changesets associations from RepoToChangeset with id = [ {} ]", new String[]{String.valueOf(repositoryId)});
                ActiveObjectsUtils.delete(activeObjects, RepositoryToChangesetMapping.class, query);

                // delete association issues - changeset
                query = Query.select().where(
                        IssueToChangesetMapping.CHANGESET_ID + " not in  " +
                                "(select " + queryHelper.getSqlColumnName(RepositoryToChangesetMapping.CHANGESET_ID) + " from " + queryHelper.getSqlTableName(RepositoryToChangesetMapping.TABLE_NAME) + ")");
                log.debug("deleting orphaned issue-changeset associations");
                ActiveObjectsUtils.delete(activeObjects, IssueToChangesetMapping.class, query);


                // delete orphaned changesets
                query = Query.select().where(
                        "ID not in  " +
                                "(select " + queryHelper.getSqlColumnName(RepositoryToChangesetMapping.CHANGESET_ID) + " from " + queryHelper.getSqlTableName(RepositoryToChangesetMapping.TABLE_NAME) + ")");
                log.debug("deleting orphaned changesets");
                ActiveObjectsUtils.delete(activeObjects, ChangesetMapping.class, query);

                return null;
            }
        });
    }

    @Override
    public Changeset create(final Changeset changeset, final Set<String> extractedIssues)
    {
        ChangesetMapping changesetMapping = activeObjects.executeInTransaction(new TransactionCallback<ChangesetMapping>()
        {
            @Override
            public ChangesetMapping doInTransaction()
            {
                ChangesetMapping chm = getChangesetMapping(changeset);
                if (chm == null)
                {
                    chm = activeObjects.create(ChangesetMapping.class);
                    fillProperties(changeset, chm);
                    chm.save();
                }

                associateRepositoryToChangeset(chm, changeset.getRepositoryId());
                if (extractedIssues != null)
                {
                    associateIssuesToChangeset(chm, extractedIssues);
                }

                return chm;
            }
        });

        changeset.setId(changesetMapping.getID());

        return changeset;
    }

    @Override
    public Changeset update(final Changeset changeset)
    {
        activeObjects.executeInTransaction(new TransactionCallback<ChangesetMapping>()
        {
            @Override
            public ChangesetMapping doInTransaction()
            {
                ChangesetMapping chm = getChangesetMapping(changeset);
                if (chm != null)
                {
                    fillProperties(changeset, chm);
                    chm.save();
                } else
                {
                    log.warn("Changest with node {} is not exists.", changeset.getNode());
                }
                return chm;
            }
        });

        return changeset;
    }

    private ChangesetMapping getChangesetMapping(Changeset changeset)
    {
        // A Query is little bit more complicated, but:

        // 1. previous implementation did not properly fill RAW_NODE, in some cases it is null, in some other cases it is empty string
        String hasRawNode = "( " + ChangesetMapping.RAW_NODE + " is not null AND " + ChangesetMapping.RAW_NODE + " != '') ";

        // 2. Latest implementation is using full RAW_NODE, but not all records contains it!
        String matchRawNode = ChangesetMapping.RAW_NODE + " = ? ";

        // 3. Previous implementation has used NODE, but it is mix in some cases it is short version, in some cases it is full version
        String matchNode = ChangesetMapping.NODE + " like ? ";

        String shortNode = changeset.getNode().substring(0, 12) + "%";
        ChangesetMapping[] mappings = activeObjects.find(ChangesetMapping.class, "(" + hasRawNode + " AND " + matchRawNode + " ) OR ( NOT "
                + hasRawNode + " AND " + matchNode + " ) ", changeset.getRawNode(), shortNode);

        if (mappings.length > 1)
        {
            log.warn("More changesets with same Node. Same changesets count: {}, Node: {}, Repository: {}", new Object[] { mappings.length,
                    changeset.getNode(), changeset.getRepositoryId() });
        }
        return (ArrayUtils.isNotEmpty(mappings)) ? mappings[0] : null;
    }

    private void fillProperties(Changeset changeset, ChangesetMapping chm)
    {
        // we need to remove null characters '\u0000' because PostgreSQL cannot store String values with such
        // characters
        // todo: remove NULL Chars before call setters
        chm.setNode(changeset.getNode());
        chm.setRawAuthor(ActiveObjectsUtils.stripToLimit(changeset.getRawAuthor(), 255));
        chm.setAuthor(changeset.getAuthor());
        chm.setDate(changeset.getDate());
        chm.setRawNode(changeset.getRawNode());
        chm.setBranch(ActiveObjectsUtils.stripToLimit(changeset.getBranch(), 255));
        chm.setMessage(changeset.getMessage());
        chm.setAuthorEmail(ActiveObjectsUtils.stripToLimit(changeset.getAuthorEmail(), 255));
        chm.setSmartcommitAvailable(changeset.isSmartcommitAvaliable());

        JSONArray parentsJson = new JSONArray();
        for (String parent : changeset.getParents())
        {
            parentsJson.put(parent);
        }

        String parentsData = parentsJson.toString();
        if (parentsData.length() > 255)
        {
            parentsData = ChangesetMapping.TOO_MANY_PARENTS;
        }
        chm.setParentsData(parentsData);

        chm.setFilesData(FileData.toJSON(changeset));
        chm.setFileDetailsJson(ChangesetFileDetails.toJSON(changeset.getFileDetails()));
        chm.setVersion(ChangesetMapping.LATEST_VERSION);
        chm.save();
    }

    private void associateIssuesToChangeset(ChangesetMapping changesetMapping, Set<String> extractedIssues)
    {
        // remove all assoc issues-changeset
        Query query = Query.select().where(IssueToChangesetMapping.CHANGESET_ID + " = ? ", changesetMapping);
        ActiveObjectsUtils.delete(activeObjects, IssueToChangesetMapping.class, query);

        // insert all
        for (String extractedIssue : extractedIssues)
        {
            final Map<String, Object> map = new MapRemovingNullCharacterFromStringValues();
            map.put(IssueToChangesetMapping.ISSUE_KEY, extractedIssue);
            map.put(IssueToChangesetMapping.PROJECT_KEY, parseProjectKey(extractedIssue));
            map.put(IssueToChangesetMapping.CHANGESET_ID, changesetMapping.getID());
            activeObjects.create(IssueToChangesetMapping.class, map);
        }


    }

    private void associateRepositoryToChangeset(ChangesetMapping changesetMapping, int repositoryId)
    {

        RepositoryToChangesetMapping[] mappings = activeObjects.find(RepositoryToChangesetMapping.class,
                RepositoryToChangesetMapping.REPOSITORY_ID + " = ? and " +
                        RepositoryToChangesetMapping.CHANGESET_ID + " = ? ",
                repositoryId,
                changesetMapping);

        if (ArrayUtils.isEmpty(mappings))
        {
            final Map<String, Object> map = new MapRemovingNullCharacterFromStringValues();

            map.put(RepositoryToChangesetMapping.REPOSITORY_ID, repositoryId);
            map.put(RepositoryToChangesetMapping.CHANGESET_ID, changesetMapping);

            activeObjects.create(RepositoryToChangesetMapping.class, map);
        }
    }

    public static String parseProjectKey(String issueKey)
    {
        return issueKey.substring(0, issueKey.indexOf("-"));
    }

    @Override
    public Changeset getByNode(final int repositoryId, final String changesetNode)
    {
        final ChangesetMapping changesetMapping = activeObjects.executeInTransaction(new TransactionCallback<ChangesetMapping>()
        {
            @Override
            public ChangesetMapping doInTransaction()
            {
                Query query = Query.select("ID, *")
                        .from(ChangesetMapping.class)
                        .alias(ChangesetMapping.class, "chm")
                        .alias(RepositoryToChangesetMapping.class, "rtchm")
                        .join(RepositoryToChangesetMapping.class, "chm.ID = rtchm." + RepositoryToChangesetMapping.CHANGESET_ID)
                        .where("chm." + ChangesetMapping.NODE + " = ? AND rtchm." + RepositoryToChangesetMapping.REPOSITORY_ID + " = ? ", changesetNode, repositoryId);


                ChangesetMapping[] mappings = activeObjects.find(ChangesetMapping.class, query);
                return mappings.length != 0 ? mappings[0] : null;
            }
        });

        final Changeset changeset = transform(changesetMapping, repositoryId);

        return changeset;
    }

    @Override
    public List<Changeset> getByIssueKey(final Iterable<String> issueKeys, final boolean newestFirst)
    {
        List<ChangesetMapping> changesetMappings = getChangesetMappingsByIssueKey(issueKeys, newestFirst);

        return transform(changesetMappings);
    }

    @Override
    public List<Changeset> getByIssueKey(Iterable<String> issueKeys, String dvcsType, final boolean newestFirst)
    {
        List<ChangesetMapping> changesetMappings = getChangesetMappingsByIssueKey(issueKeys, newestFirst);

        return transform(changesetMappings, dvcsType);
    }

    private List<ChangesetMapping> getChangesetMappingsByIssueKey(Iterable<String> issueKeys, final boolean newestFirst)
    {
        final GlobalFilter gf = new GlobalFilter();
        gf.setInIssues(issueKeys);
        final String baseWhereClause = new GlobalFilterQueryWhereClauseBuilder(gf).build();
        final List<ChangesetMapping> changesetMappings = activeObjects.executeInTransaction(new TransactionCallback<List<ChangesetMapping>>()
        {
            @Override
            public List<ChangesetMapping> doInTransaction()
            {
                ChangesetMapping[] mappings = activeObjects.find(ChangesetMapping.class,
                        Query.select("ID, *")
                                .alias(ChangesetMapping.class, "CHANGESET")
                                .alias(IssueToChangesetMapping.class, "ISSUE")
                                .join(IssueToChangesetMapping.class, "CHANGESET.ID = ISSUE." + IssueToChangesetMapping.CHANGESET_ID)
                                .where(baseWhereClause)
                                .order(ChangesetMapping.DATE + (newestFirst ? " DESC": " ASC")));

                return Arrays.asList(mappings);
            }
        });

        return changesetMappings;
    }

    @Override
    public List<Changeset> getLatestChangesets(final int maxResults, final GlobalFilter gf)
    {
        if (maxResults <= 0)
        {
            return Collections.emptyList();
        }
        final List<ChangesetMapping> changesetMappings = activeObjects.executeInTransaction(new TransactionCallback<List<ChangesetMapping>>()
        {
            @Override
            public List<ChangesetMapping> doInTransaction()
            {
                String baseWhereClause = new GlobalFilterQueryWhereClauseBuilder(gf).build();
                Query query = Query.select("ID, *")
                        .alias(ChangesetMapping.class, "CHANGESET")
                        .alias(IssueToChangesetMapping.class, "ISSUE")
                        .join(IssueToChangesetMapping.class, "CHANGESET.ID = ISSUE." + IssueToChangesetMapping.CHANGESET_ID)
                        .where(baseWhereClause).limit(maxResults).order(ChangesetMapping.DATE + " DESC");
                ChangesetMapping[] mappings = activeObjects.find(ChangesetMapping.class, query);
                return Arrays.asList(mappings);
            }
        });

        return transform(changesetMappings);
    }

    @Override
    public void forEachLatestChangesetsAvailableForSmartcommitDo(final int repositoryId, final ForEachChangesetClosure closure)
    {
        Query query = createLatestChangesetsAvailableForSmartcommitQuery(repositoryId);
        activeObjects.stream(ChangesetMapping.class, query, new EntityStreamCallback<ChangesetMapping, Integer>()
        {
            @Override
            public void onRowRead(ChangesetMapping mapping)
            {
                closure.execute(mapping);
            }
        });
    }

    private Query createLatestChangesetsAvailableForSmartcommitQuery(int repositoryId)
    {
        return Query.select("*")
                .from(ChangesetMapping.class)
                .alias(ChangesetMapping.class, "chm")
                .alias(RepositoryToChangesetMapping.class, "rtchm")
                .join(RepositoryToChangesetMapping.class, "chm.ID = rtchm." + RepositoryToChangesetMapping.CHANGESET_ID)
                .where("rtchm." + RepositoryToChangesetMapping.REPOSITORY_ID + " = ? and chm."+ChangesetMapping.SMARTCOMMIT_AVAILABLE+" = ? " , repositoryId, Boolean.TRUE)
                .order(ChangesetMapping.DATE + " DESC");
    }

    @Override
    public Set<String> findReferencedProjects(int repositoryId)
    {
        Query query = Query.select(IssueToChangesetMapping.PROJECT_KEY).distinct()
                .alias(ProjectKey.class, "pk")
                .alias(ChangesetMapping.class, "chm")
                .alias(RepositoryToChangesetMapping.class, "rtchm")
                .join(ChangesetMapping.class, "chm.ID = pk." + IssueToChangesetMapping.CHANGESET_ID)
                .join(RepositoryToChangesetMapping.class, "chm.ID = rtchm." + RepositoryToChangesetMapping.CHANGESET_ID)
                .where("rtchm." + RepositoryToChangesetMapping.REPOSITORY_ID + " = ?", repositoryId)
                .order(IssueToChangesetMapping.PROJECT_KEY);


        final Set<String> projectKeys = new HashSet<String>();
        activeObjects.stream(ProjectKey.class, query, new EntityStreamCallback<ProjectKey, String>()
        {
            @Override
            public void onRowRead(ProjectKey mapping)
            {
                projectKeys.add(mapping.getProjectKey());
            }
        });

        return projectKeys;
    }

    @Table("IssueToChangeset")
    static interface ProjectKey extends RawEntity<String>
    {

        @PrimaryKey(IssueToChangesetMapping.PROJECT_KEY)
        String getProjectKey();

        void setProjectKey();
    }

    @Override
    public void markSmartcommitAvailability(int id, boolean available)
    {
        final ChangesetMapping changesetMapping = activeObjects.get(ChangesetMapping.class, id);
        changesetMapping.setSmartcommitAvailable(available);
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {
            @Override
            public Void doInTransaction()
            {
                changesetMapping.save();
                return null;
            }
        });
    }

    @Override
    public int getChangesetCount(final int repositoryId)
    {
        return activeObjects.executeInTransaction(new TransactionCallback<Integer>()
        {
            @Override
            public Integer doInTransaction()
            {
                Query query = Query.select().where(RepositoryToChangesetMapping.REPOSITORY_ID + " = ?", repositoryId);

                return activeObjects.count(RepositoryToChangesetMapping.class, query);
            }
        });
    }

}
