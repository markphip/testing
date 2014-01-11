package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.external.ModelVersion;

public class To_14_NewRepositoryColumn implements ActiveObjectsUpgradeTask
{
    private static final Logger log = LoggerFactory.getLogger(To_14_NewRepositoryColumn.class);

    @Override
    public void upgrade(ModelVersion currentVersion, ActiveObjects activeObjects)
    {
        log.info("upgrade [ " + getModelVersion() + " ]");

        activeObjects.migrate(OrganizationMapping.class, RepositoryMapping.class, ChangesetMapping.class,
                IssueToChangesetMapping.class, RepositoryToChangesetMapping.class, BranchHeadMapping.class);

        log.info("upgrade [ " + getModelVersion() + " ]: finished");
    }

    @Override
    public ModelVersion getModelVersion()
    {
        return ModelVersion.valueOf("14");
    }

}