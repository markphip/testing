package com.atlassian.jira.plugins.dvcs.dao.impl.transform;

import java.util.ArrayList;
import java.util.List;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.OrganizationMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryMapping;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileDetail;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileDetails;
import com.atlassian.jira.plugins.dvcs.model.FileData;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.google.common.collect.ImmutableList;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangesetTransformer
{
    public static final Logger log = LoggerFactory.getLogger(ChangesetTransformer.class);
    private final ActiveObjects activeObjects;

    public ChangesetTransformer(final ActiveObjects activeObjects)
    {
        this.activeObjects = activeObjects;
    }

    public Changeset transform(ChangesetMapping changesetMapping, int mainRepositoryId, String dvcsType)
    {

        if (changesetMapping == null)
        {
            return null;
        }

//        log.debug("Changeset transformation: [{}] ", changesetMapping);

        final Changeset changeset = transform(mainRepositoryId, changesetMapping);
        
        List<Integer> repositories = changeset.getRepositoryIds();
        int firstRepository = 0;

        for (RepositoryMapping repositoryMapping : changesetMapping.getRepositories())
        {
            if (repositoryMapping.isDeleted() || !repositoryMapping.isLinked())
            {
                continue;
            }

            if (!StringUtils.isEmpty(dvcsType))
            {
                OrganizationMapping organizationMapping = activeObjects.get(OrganizationMapping.class, repositoryMapping.getOrganizationId());

                if (!dvcsType.equals(organizationMapping.getDvcsType()))
                {
                   continue;
                }
            }

            if (repositories == null)
            {
                repositories = new ArrayList<Integer>();
                changeset.setRepositoryIds(repositories);

                // mark first repository
                firstRepository = repositoryMapping.getID();
            }

            // we found repository that is not fork and no main repository is set on changeset,let's use it
            if (changeset.getRepositoryId() == 0 && !repositoryMapping.isFork())
            {
                changeset.setRepositoryId(repositoryMapping.getID());
            }

            repositories.add(repositoryMapping.getID());
        }

        // no main repository was assigned, let's use the first one
        if (changeset.getRepositoryId() == 0)
        {
            changeset.setRepositoryId(firstRepository);
        }
        return CollectionUtils.isEmpty(changeset.getRepositoryIds())? null : changeset;
    }

    public Changeset transform(int repositoryId, ChangesetMapping changesetMapping)
    {
        if (changesetMapping == null)
        {
            return null;
        }

        final FileData fileData = FileData.from(changesetMapping);

        final Changeset changeset = new Changeset(repositoryId,
                changesetMapping.getNode(),
                changesetMapping.getRawAuthor(),
                changesetMapping.getAuthor(),
                changesetMapping.getDate(),
                changesetMapping.getRawNode(),
                changesetMapping.getBranch(),
                changesetMapping.getMessage(),
                parseParentsData(changesetMapping.getParentsData()),
                fileData.getFiles(),
                fileData.getFileCount(),
                changesetMapping.getAuthorEmail());

        changeset.setId(changesetMapping.getID());
        changeset.setVersion(changesetMapping.getVersion());
        changeset.setSmartcommitAvaliable(changesetMapping.isSmartcommitAvailable());

        // prefer the file details info
        List<ChangesetFileDetail> fileDetails = ChangesetFileDetails.fromJSON(changesetMapping.getFileDetailsJson());
        changeset.setFiles(fileDetails != null ? ImmutableList.<ChangesetFile>copyOf(fileDetails) : changeset.getFiles());
        changeset.setFileDetails(fileDetails);

        return changeset;
    }

    private List<String> parseParentsData(String parentsData)
    {
        if (ChangesetMapping.TOO_MANY_PARENTS.equals(parentsData))
        {
            return null;
        }
        
        List<String> parents = new ArrayList<String>();

        if (StringUtils.isBlank(parentsData))
        {
            return parents;
        }

        try
        {
            JSONArray parentsJson = new JSONArray(parentsData);
            for (int i = 0; i < parentsJson.length(); i++)
            {
                parents.add(parentsJson.getString(i));
            }
        } catch (JSONException e)
        {
            log.error("Failed parsing parents from ParentsJson data.");
        }

        return parents;
    }
}
