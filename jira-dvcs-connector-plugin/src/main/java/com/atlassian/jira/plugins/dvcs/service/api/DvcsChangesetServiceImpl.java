package com.atlassian.jira.plugins.dvcs.service.api;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.model.GlobalFilter;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Map;

public class DvcsChangesetServiceImpl implements DvcsChangesetService
{
    private ChangesetService changesetService;

    public DvcsChangesetServiceImpl(ChangesetService changesetService)
    {
        this.changesetService = changesetService;
    }

    @Override
    public List<Changeset> getChangesets(Repository repository)
    {
        return ImmutableList.copyOf(changesetService.getChangesetsFromDvcs(repository));
    }

    @Override
    public List<Changeset> getChangesets(Iterable<String> issueKeys)
    {
        return ImmutableList.copyOf(changesetService.getByIssueKey(issueKeys));
    }

    @Override
    public List<Changeset> getChangesets(int maxResults, GlobalFilter globalFilter)
    {
        return ImmutableList.copyOf(changesetService.getLatestChangesets(maxResults, globalFilter));
    }

    @Override
    public Changeset getDetailChangeset(Repository repository, Changeset changeset)
    {
        return changesetService.getDetailChangesetFromDvcs(repository, changeset);
    }

    @Override
    public String getChangesetURL(Repository repository, Changeset changeset)
    {
        return changesetService.getCommitUrl(repository, changeset);
    }

    @Override
    public Map<ChangesetFile, String> getFileChangesets(Repository repository, Changeset changeset)
    {
        return changesetService.getFileCommitUrls(repository, changeset);
    }
}