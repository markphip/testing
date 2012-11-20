package com.atlassian.jira.plugins.dvcs.upgrade;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.linker.BitbucketLinker;
import com.atlassian.sal.api.message.Message;
import com.atlassian.sal.api.upgrade.PluginUpgradeTask;

/**
 * For Bitbucket.
 */
public class To_02_ProjectBasedRepositoryLinksUpgradeTask implements PluginUpgradeTask
{
    private static final Logger log = LoggerFactory.getLogger(To_02_ProjectBasedRepositoryLinksUpgradeTask.class);
    private final BitbucketLinker linker;//TODO should not depend on Bitbucket module => BBC-331
    private final RepositoryService repositoryService;
    private final ChangesetService changesetService;

    public To_02_ProjectBasedRepositoryLinksUpgradeTask(@Qualifier("defferedBitbucketLinker") BitbucketLinker linker,
            RepositoryService repositoryService, ChangesetService changesetService)
    {
        this.linker = linker;
        this.repositoryService = repositoryService;
        this.changesetService = changesetService;
    }

    // -------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------
    // Upgrade
    // -------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------

    @Override
    public Collection<Message> doUpgrade() throws Exception
    {
        doUpgradeInternal();
        return Collections.emptyList();
    }

    private void doUpgradeInternal()
    {
        List<Repository> allRepositories = repositoryService.getAllRepositories();
        for (Repository repository : allRepositories)
        {
            try
            {
                if (repository.isLinked())
                {
                    log.debug("LINKING {} repository.", repository.getName());
                    linker.linkRepository(repository,
                            changesetService.findReferencedProjects(repository.getId()));
                }
            } catch (Exception e)
            {
                log.warn("Failed to link repository {}. " + e.getMessage(), repository.getName());
            }
        }
    }

    // -------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------

    @Override
    public int getBuildNumber()
    {
        return 2;
    }

    @Override
    public String getShortDescription()
    {
        return "Upgrades the repository links at Bitbucket with custom handlers (regexp) base on project keys present int this JIRA instance.";
    }

    @Override
    public String getPluginKey()
    {
        return "com.atlassian.jira.plugins.jira-bitbucket-connector-plugin";
    }

}
