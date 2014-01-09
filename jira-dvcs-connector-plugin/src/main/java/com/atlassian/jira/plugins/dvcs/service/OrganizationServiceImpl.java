package com.atlassian.jira.plugins.dvcs.service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import com.atlassian.jira.plugins.dvcs.dao.OrganizationDao;
import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;

public class OrganizationServiceImpl implements OrganizationService
{

    private final OrganizationDao organizationDao;

    private final DvcsCommunicatorProvider dvcsCommunicatorProvider;

    private final RepositoryService repositoryService;

    public OrganizationServiceImpl(final OrganizationDao organizationDao, final DvcsCommunicatorProvider dvcsCommunicatorProvider,
            final RepositoryService repositoryService)
    {
        this.organizationDao = organizationDao;
        this.dvcsCommunicatorProvider = dvcsCommunicatorProvider;
        this.repositoryService = repositoryService;
    }

    @Override
    public AccountInfo getAccountInfo(final String hostUrl, final String accountName)
    {
        return getAccountInfo(hostUrl, accountName, null);
    }

    @Override
    public AccountInfo getAccountInfo(final String hostUrl, final String accountName, final String dvcsType)
    {
        return dvcsCommunicatorProvider.getAccountInfo(hostUrl, accountName, dvcsType);
    }

    @Override
    public List<Organization> getAll(final boolean loadRepositories)
    {
        final List<Organization> organizations = organizationDao.getAll();

        if (loadRepositories)
        {
            for (final Organization organization : organizations)
            {
                final List<Repository> repositories = repositoryService.getAllByOrganization(organization.getId());
                organization.setRepositories(repositories);
            }
        }
        return organizations;
    }

    @Override
    public List<Organization> getAll(final boolean loadRepositories, final String type)
    {
        final List<Organization> organizations = organizationDao.getAllByType(type);

        if (loadRepositories)
        {
            for (final Organization organization : organizations)
            {
                final List<Repository> repositories = repositoryService.getAllByOrganization(organization.getId());
                organization.setRepositories(repositories);
            }
        }

        return organizations;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getAllCount()
    {
        return organizationDao.getAllCount();
    }

    @Override
    public Organization get(final int organizationId, final boolean loadRepositories)
    {
        final Organization organization = organizationDao.get(organizationId);

        if (loadRepositories && organization != null)
        {
            final List<Repository> repositories = repositoryService.getAllByOrganization(organizationId);
            organization.setRepositories(repositories);
        }

        return organization;
    }

    public Organization saveAsync(final Organization organization)
    {
        return doSave(organization, true);
    }

    public Organization save(final Organization organization)
    {
        return doSave(organization, false);
    }

    public Organization doSave(final Organization organization, final boolean async)
    {
        Organization org = organizationDao.getByHostAndName(organization.getHostUrl(), organization.getName());
        if (org != null)
        {
            // nop;
            // we've already have this organization, don't save another one
            return org;
        }

        //
        // it's brand new organization. save it.
        //
        org = organizationDao.save(organization);

        // sync repository list
        if (async)
        {
            repositoryService.syncRepositoryList(org, false);
        }
        else
        {
            repositoryService.syncRepositoryListAsync(org, false);
        }

        return org;
    }

    @Override
    public void remove(final int organizationId)
    {
        final List<Repository> repositoriesToDelete = repositoryService.getAllByOrganization(organizationId, true);
        organizationDao.remove(organizationId);
        repositoryService.removeRepositories(repositoriesToDelete);
        repositoryService.removeOrphanRepositoriesAsync(repositoriesToDelete);
    }

    @Override
    public void updateCredentials(final int organizationId, final Credential credential)
    {
        final Organization organization = organizationDao.get(organizationId);
        if (organization != null)
        {
            organization.setCredential(credential);
            organizationDao.save(organization);
        }
    }

    @Override
    public void updateCredentialsAccessToken(final int organizationId, final String accessToken)
    {
        final Organization organization = organizationDao.get(organizationId);
        if (organization != null)
        {
            organization.getCredential().setAccessToken(accessToken);
            organizationDao.save(organization);
        }
    }

    @Override
    public void enableAutolinkNewRepos(final int orgId, final boolean autolink)
    {
        final Organization organization = organizationDao.get(orgId);
        if (organization != null)
        {
            organization.setAutolinkNewRepos(autolink);
            organizationDao.save(organization);
        }
    }

    @Override
    public void enableSmartcommitsOnNewRepos(final int id, final boolean enabled)
    {
        final Organization organization = organizationDao.get(id);
        if (organization != null)
        {
            organization.setSmartcommitsOnNewRepos(enabled);
            organizationDao.save(organization);
        }

    }

    @Override
    public List<Organization> getAutoInvitionOrganizations()
    {
        return organizationDao.getAutoInvitionOrganizations();
    }

    @Override
    public List<Organization> getAllByIds(final Collection<Integer> ids)
    {
        if (CollectionUtils.isNotEmpty(ids))
        {
            return organizationDao.getAllByIds(ids);
        }
        else
        {
            return Collections.emptyList();
        }
    }

    @Override
    public void setDefaultGroupsSlugs(final int orgId, final Collection<String> groupsSlugs)
    {
        organizationDao.setDefaultGroupsSlugs(orgId, groupsSlugs);
    }

    @Override
    public Organization findIntegratedAccount()
    {
        return organizationDao.findIntegratedAccount();
    }

    @Override
    public Organization getByHostAndName(final String hostUrl, final String name)
    {
        return organizationDao.getByHostAndName(hostUrl, name);
    }

    @Override
    public DvcsUser getTokenOwner(final int organizationId)
    {
        final Organization organization = get(organizationId, false);
        final DvcsCommunicator communicator = dvcsCommunicatorProvider.getCommunicator(organization.getDvcsType());
        final DvcsUser currentUser = communicator.getTokenOwner(organization);
        return currentUser;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Group> getGroupsForOrganization(final Organization organization)
    {
        return dvcsCommunicatorProvider.getCommunicator(organization.getDvcsType()).getGroupsForOrganization(organization);
    }

    @Override
    public boolean existsOrganizationWithType(final String... types)
    {
        return organizationDao.existsOrganizationWithType(types);
    }
}
