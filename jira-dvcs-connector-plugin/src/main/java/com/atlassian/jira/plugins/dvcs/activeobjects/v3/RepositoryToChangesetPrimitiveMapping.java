package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.schema.Table;

@Preload
@Table ("RepoToChangeset")
public interface RepositoryToChangesetPrimitiveMapping extends Entity
{


    public static final String TABLE_NAME = "AO_E8B6CC_REPO_TO_CHANGESET";   // todo: is there a nice way in AO to get the table name?
    public static final String REPOSITORY_ID = "REPOSITORY_ID";
    public static final String CHANGESET_ID = "CHANGESET_ID";


    int getRepositoryId();

    void setRepositoryId(int repositoryId);

    int getChangesetId();

    void setChangesetId(int changesetId);
}
