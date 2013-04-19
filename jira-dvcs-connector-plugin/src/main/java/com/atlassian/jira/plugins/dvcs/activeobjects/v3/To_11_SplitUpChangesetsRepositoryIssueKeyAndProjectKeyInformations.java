package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import net.java.ao.DBParam;
import net.java.ao.EntityStreamCallback;
import net.java.ao.Query;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.external.ModelVersion;
import com.atlassian.sal.api.transaction.TransactionCallback;

/**
 * Realizes migration of issue key and project key from one table: {@link ChangesetMapping} into the {@link ChangesetMapping} and a
 * {@link IssueToChangesetMapping}.
 * 
 * For more information see BBC-415.
 * 
 * @author stanislav-dvorscak
 * 
 */
// suppress deprecation - we want to have migrators stable as much as possible
@SuppressWarnings("deprecation")
public class To_11_SplitUpChangesetsRepositoryIssueKeyAndProjectKeyInformations implements ActiveObjectsUpgradeTask
{

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(To_11_SplitUpChangesetsRepositoryIssueKeyAndProjectKeyInformations.class);

    /**
     * @see #getModelVersion()
     */
    private static final ModelVersion MODEL_VERSION = ModelVersion.valueOf("11");

    /**
     * {@inheritDoc}
     */
    @Override
    public ModelVersion getModelVersion()
    {
        return MODEL_VERSION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void upgrade(ModelVersion currentVersion, final ActiveObjects ao)
    {
        logger.debug("upgrade [ " + getModelVersion() + " ]");

        Query query = Query.select().order(ChangesetMapping.RAW_NODE + " ASC ID ASC ");
        ao.stream(ChangesetMapping.class, query, new EntityStreamCallback<ChangesetMapping, Integer>()
        {

            private ChangesetMapping uniqueChangeset;

            @Override
            public void onRowRead(final ChangesetMapping current)
            {
                ao.executeInTransaction(new TransactionCallback<Void>()
                {

                    @Override
                    public Void doInTransaction()
                    {
                        // false if current changeset is consider to be unique, otherwise it is duplicate
                        boolean isDuplicate = uniqueChangeset == null || !uniqueChangeset.getRawNode().equals(current.getRawNode());

                        // if it is unique changeset - it updates cursor for current valid unique changeset
                        if (!isDuplicate)
                        {
                            uniqueChangeset = current;
                        }

                        if (StringUtils.isBlank(current.getProjectKey()) || StringUtils.isBlank(current.getIssueKey()))
                        {
                            throw new IllegalStateException(
                                    "Critical error, I am unable to continue in migration! Invalid data on changeset: "
                                            + current.getRawNode() + " issue key or project key is empty!");
                        }

                        // project key and issue key relations are added on unique changeset
                        ao.create(IssueToChangesetMapping.class, //
                                new DBParam(IssueToChangesetMapping.CHANGESET_ID, uniqueChangeset.getID()), // foreign key to unique
                                                                                                            // changeset - current will be
                                                                                                            // maybe deleted
                                new DBParam(IssueToChangesetMapping.PROJECT_KEY, current.getProjectKey()), //
                                new DBParam(IssueToChangesetMapping.ISSUE_KEY, current.getIssueKey()) //
                        );

                        // removes old information - mark that it was already proceed
                        uniqueChangeset.setProjectKey(null);
                        uniqueChangeset.setIssueKey(null);

                        if (current.getRepositoryId() == 0)
                        {
                            throw new IllegalStateException(
                                    "Critical error, I am unable to continue in migration! Invalid data on changeset: "
                                            + current.getRawNode() + " repository id is not provided!");
                        }

                        // repository relation is added on unique changeset
                        ao.create(RepositoryToChangesetMapping.class, //
                                new DBParam(RepositoryToChangesetMapping.REPOSITORY_ID, uniqueChangeset.getRepositoryId()), //
                                new DBParam(RepositoryToChangesetMapping.CHANGESET_ID, uniqueChangeset) //
                        );
                        uniqueChangeset.setRepositoryId(0);

                        // if current changeset is not unique, means duplicate will be removed
                        if (isDuplicate)
                        {
                            ao.delete(uniqueChangeset);
                        }

                        // updates migrated entity
                        uniqueChangeset.save();

                        return null;
                    }

                });
            }

        });
    }
}
