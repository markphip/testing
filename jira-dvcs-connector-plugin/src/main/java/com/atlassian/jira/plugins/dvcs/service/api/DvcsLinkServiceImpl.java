package com.atlassian.jira.plugins.dvcs.service.api;

import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.scheduler.DvcsScheduler;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import org.apache.commons.lang.StringUtils;

import java.util.Date;
import java.util.List;
import javax.annotation.Nonnull;

import static java.util.Collections.unmodifiableList;

public class DvcsLinkServiceImpl implements DvcsLinkService
{
    private final OrganizationService organizationService;
    private final DvcsScheduler dvcsScheduler;

    public DvcsLinkServiceImpl(OrganizationService organizationService, DvcsScheduler dvcsScheduler)
    {
        this.organizationService = organizationService;
        this.dvcsScheduler = dvcsScheduler;
    }

    @Override
    public Organization getDvcsLink(boolean loadRepositories, int organizationId)
    {
        return organizationService.get(organizationId, loadRepositories);
    }

    @Override
    public List<Organization> getDvcsLinks(boolean loadRepositories)
    {
        return unmodifiableList(organizationService.getAll(loadRepositories));
    }

    @Override
    public List<Organization> getDvcsLinks(boolean loadRepositories, @Nonnull String applicationType)
    {
        if (StringUtils.isEmpty(applicationType))
        {
            throw new IllegalArgumentException("'applicationType' is null or empty");
        }
        return unmodifiableList(organizationService.getAll(loadRepositories, applicationType));
    }

    @Override
    public Date getNextSyncTime()
    {
        return new Date(dvcsScheduler.getNextSchedule());
    }
}
