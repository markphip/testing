package com.atlassian.jira.plugins.dvcs.spi.bitbucket;

import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.model.Branch;
import com.atlassian.jira.plugins.dvcs.model.BranchHead;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.BranchService;
import com.atlassian.jira.plugins.dvcs.service.ChangesetCache;
import com.atlassian.jira.plugins.dvcs.service.message.MessageAddress;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.service.remote.BranchedChangesetIterator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.JsonParsingException;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketAccount;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketBranch;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketBranchesAndTags;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangeset;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetPage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetWithDiffstat;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketGroup;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketNewChangeset;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketServiceEnvelope;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketServiceField;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BitbucketRequestException;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.ServiceRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.URLPathFormatter;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.linker.BitbucketLinker;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.message.BitbucketSynchronizeActivityMessage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.message.BitbucketSynchronizeChangesetMessage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.message.oldsync.OldBitbucketSynchronizeCsetMsg;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.ratelimit.RateLimiter;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.ratelimit.RateLimiterBuilder;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers.ChangesetTransformer;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers.DetailedChangesetTransformer;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers.GroupTransformer;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers.NewChangesetIterableAdapter;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers.RepositoryTransformer;
import com.atlassian.jira.plugins.dvcs.sync.BitbucketSynchronizeActivityMessageConsumer;
import com.atlassian.jira.plugins.dvcs.sync.BitbucketSynchronizeChangesetMessageConsumer;
import com.atlassian.jira.plugins.dvcs.sync.OldBitbucketSynchronizeCsetMsgConsumer;
import com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag;
import com.atlassian.jira.plugins.dvcs.util.DvcsConstants;
import com.atlassian.jira.plugins.dvcs.util.Retryer;
import com.atlassian.plugin.PluginAccessor;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.annotation.Resource;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class BitbucketCommunicator implements DvcsCommunicator
{
    private static final Logger log = LoggerFactory.getLogger(BitbucketCommunicator.class);

    private static final int CHANGESET_LIMIT = Integer.getInteger("bitbucket.request.changeset.limit", 15);

    private static final int MAX_CONCURRENT_DIFFSTAT_REQUESTS_LIMIT = Integer.getInteger("bitbucket.concurrent.diffstat.requests.limit", 10);
    private static final int MAX_DIFFSTAT_REQUESTS_PER_HOUR_LIMIT = Integer.getInteger("bitbucket.diffstat.requests.per.hour.limit", 500);

    public static final String BITBUCKET = "bitbucket";

    private final BitbucketLinker bitbucketLinker;
    private final String pluginVersion;
    private final BitbucketClientBuilderFactory bitbucketClientBuilderFactory;

    @Resource
    private BranchService branchService;

    private final ChangesetCache changesetCache;

    @Resource
    private MessagingService messagingService;

    private final RateLimiter rateLimiter;

    public BitbucketCommunicator(@Qualifier("defferedBitbucketLinker") BitbucketLinker bitbucketLinker,
            PluginAccessor pluginAccessor, BitbucketClientBuilderFactory bitbucketClientBuilderFactory,
            ChangesetCache changesetCache)
    {
        this.bitbucketLinker = bitbucketLinker;
        this.bitbucketClientBuilderFactory = bitbucketClientBuilderFactory;
        this.pluginVersion = DvcsConstants.getPluginVersion(pluginAccessor);
        this.changesetCache = changesetCache;
        this.rateLimiter = new RateLimiterBuilder().maxConcurrency(MAX_CONCURRENT_DIFFSTAT_REQUESTS_LIMIT)
                                                   .queryLimit(MAX_DIFFSTAT_REQUESTS_PER_HOUR_LIMIT)
                                                   .timeLength(1, TimeUnit.HOURS)
                                                   .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDvcsType()
    {
        return BITBUCKET;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AccountInfo getAccountInfo(String hostUrl, String accountName)
    {
        try
        {
            BitbucketRemoteClient remoteClient = bitbucketClientBuilderFactory.noAuthClient(hostUrl).build();

            // just to call the rest
            remoteClient.getAccountRest().getUser(accountName);
            return new AccountInfo(BitbucketCommunicator.BITBUCKET);
        } catch (BitbucketRequestException e)
        {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Repository> getRepositories(Organization organization)
    {
        try
        {
            BitbucketRemoteClient remoteClient = bitbucketClientBuilderFactory.forOrganization(organization).cached().build();
            List<BitbucketRepository> repositories = remoteClient.getRepositoriesRest().getAllRepositories(
                    organization.getName());
            return RepositoryTransformer.fromBitbucketRepositories(repositories);
        } catch (BitbucketRequestException.Unauthorized_401 e)
        {
            log.debug("Invalid credentials", e);
            throw new SourceControlException.UnauthorisedException("Invalid credentials", e);
        } catch ( BitbucketRequestException.BadRequest_400 e)
        {
            // We received bad request status code and we assume that an invalid OAuth is the cause
            throw new SourceControlException.UnauthorisedException("Invalid credentials");
        } catch (BitbucketRequestException e)
        {
            log.debug(e.getMessage(), e);
            throw new SourceControlException(e.getMessage(), e);
        } catch (JsonParsingException e)
        {
            log.debug(e.getMessage(), e);
            if (organization.isIntegratedAccount())
            {
                throw new SourceControlException.UnauthorisedException("Unexpected response was returned back from server side. Check that all provided information of account '"
                        + organization.getName() + "' is valid. Basically it means: unexisting account or invalid key/secret combination.", e);
            }
            throw new SourceControlException.InvalidResponseException("The response could not be parsed. This is most likely caused by invalid credentials.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Changeset getChangeset(Repository repository, String node)
    {
        try
        {
            // get the changeset
            BitbucketRemoteClient remoteClient = bitbucketClientBuilderFactory.forRepository(repository).build();
            BitbucketChangeset bitbucketChangeset = remoteClient.getChangesetsRest().getChangeset(repository.getOrgName(),
                            repository.getSlug(), node);

            Changeset fromBitbucketChangeset = ChangesetTransformer.fromBitbucketChangeset(repository.getId(), bitbucketChangeset);
            return fromBitbucketChangeset;
        } catch (BitbucketRequestException e)
        {
            log.debug(e.getMessage(), e);
            throw new SourceControlException("Could not get changeset [" + node + "] from " + repository.getRepositoryUrl(), e);
        } catch (JsonParsingException e)
        {
            log.debug(e.getMessage(), e);
            throw new SourceControlException.InvalidResponseException("Could not get changeset [" + node + "] from " + repository.getRepositoryUrl(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Changeset getDetailChangeset(Repository repository, Changeset changeset)
    {
        rateLimiter.aquire();
        try
        {
            // get the commit statistics for changeset
            BitbucketRemoteClient remoteClient = bitbucketClientBuilderFactory.forRepository(repository).build();
            List<BitbucketChangesetWithDiffstat> changesetDiffStat = remoteClient.getChangesetsRest().getChangesetDiffStat(repository.getOrgName(),
                    repository.getSlug(), changeset.getNode(), Changeset.MAX_VISIBLE_FILES);
            // merge it all
            return DetailedChangesetTransformer.fromChangesetAndBitbucketDiffstats(changeset, changesetDiffStat);
        } catch (BitbucketRequestException e)
        {
            log.debug(e.getMessage(), e);
            throw new SourceControlException("Could not get detailed changeset [" + changeset.getNode() + "] from " + repository.getRepositoryUrl(), e);
        } catch (JsonParsingException e)
        {
            log.debug(e.getMessage(), e);
            throw new SourceControlException.InvalidResponseException("Could not get changeset [" + changeset.getNode() + "] from " + repository.getRepositoryUrl(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterable<Changeset> getChangesets(final Repository repository)
    {
        try
        {
            //remote branch list
            final List<Branch> newBranches = getBranches(repository);
            log.debug("Current branch heads for repository [{}]: {}", repository.getId(), newBranches);
            if (newBranches.isEmpty())
            {
                // this can happen only when empty repository
                return Collections.emptyList();
            }
            //local branch head list
            List<BranchHead> oldBranchHeads = branchService.getListOfBranchHeads(repository);
            log.debug("Previous branch heads for repository [{}]: {}", repository.getId(), oldBranchHeads);
            Iterable<Changeset> result = null;

            // if we don't have any previous heads, but we there are some changesets, we will use old synchronization
            if (oldBranchHeads.isEmpty() && !changesetCache.isEmpty(repository.getId()))
            {
                log.info("No previous branch heads were found, switching to old changeset synchronization for repository [{}].", repository.getId());
                result = new Iterable<Changeset>()
                {
                    @Override
                    public Iterator<Changeset> iterator()
                    {
                        return new BranchedChangesetIterator(changesetCache, BitbucketCommunicator.this, repository, newBranches);
                    }

                };
            } else
            {

                List<String> includeNodes = extractBranchHeadsFromBranches(newBranches);
                List<String> excludeNodes = extractBranchHeads(oldBranchHeads);
                if (includeNodes != null && excludeNodes != null)
                {
                    includeNodes.removeAll(excludeNodes);
                }

                // Do we have new heads?
                if (includeNodes == null || !includeNodes.isEmpty())
                {
                    Map<String, String> changesetBranch = new HashMap<String, String>();
                    for (Branch branch : newBranches)
                    {
                        for (BranchHead branchHead : branch.getHeads())
                        {
                            changesetBranch.put(branchHead.getHead(), branchHead.getName());
                        }
                    }

                    BitbucketRemoteClient remoteClient = bitbucketClientBuilderFactory.forRepository(repository).build();
                    Iterable<BitbucketNewChangeset> bitbucketChangesets =
                            remoteClient.getChangesetsRest().getChangesets(repository.getOrgName(),
                                                                           repository.getSlug(),
                                                                           includeNodes,
                                                                           excludeNodes,
                                                                           changesetBranch,
                                                                           CHANGESET_LIMIT);

                    result = new NewChangesetIterableAdapter(repository, bitbucketChangesets);
                } else
                {
                    log.debug("No new changesets detected for repository [{}].", repository.getId());
                    result = Collections.emptyList();
                }

            }

            branchService.updateBranchHeads(repository, newBranches, oldBranchHeads);

            return result;
        }
        catch (BitbucketRequestException e)
        {
            log.debug(e.getMessage(), e);
            throw new SourceControlException("Could not get result", e);
        }
    }

    public BitbucketChangesetPage getChangesetsForPage(int page, Repository repository, List<String> includeNodes, List<String> excludeNodes)
    {
        BitbucketRemoteClient remoteClient = bitbucketClientBuilderFactory.forRepository(repository).build();
        return remoteClient.getChangesetsRest().getChangesetsForPage(page, repository.getOrgName(), repository.getSlug(), CHANGESET_LIMIT,
                includeNodes, excludeNodes);
    }

    private List<String> extractBranchHeadsFromBranches(List<Branch> branches)
    {
        List<String> result = new ArrayList<String>();
        for (Branch branch : branches)
        {
            for (BranchHead branchHead : branch.getHeads())
            {
                result.add(branchHead.getHead());
            }
        }

        return result;
    }

    private List<String> extractBranchHeads(List<BranchHead> branchHeads)
    {
        List<String> result = new ArrayList<String>();
        for (BranchHead branchHead : branchHeads)
        {
            result.add(branchHead.getHead());
        }

        return result;
    }

    @Override
    public List<Branch> getBranches(Repository repository)
    {
        List<Branch> branches = new ArrayList<Branch>();
        BitbucketBranchesAndTags branchesAndTags = retrieveBranchesAndTags(repository);

        List<BitbucketBranch> bitbucketBranches = branchesAndTags.getBranches();
        for (BitbucketBranch bitbucketBranch : bitbucketBranches)
        {
            List<String> bitbucketHeads = bitbucketBranch.getHeads();
            List<BranchHead> heads = new ArrayList<BranchHead>();

            for (String head : bitbucketHeads)
            {
                // make sure "default" branch is first in the list
                if (bitbucketBranch.isMainbranch())
                {
                    heads.add(0, new BranchHead(bitbucketBranch.getName(), head));
                } else
                {
                    heads.add(new BranchHead(bitbucketBranch.getName(), head));
                }
            }

            Branch branch = new Branch(bitbucketBranch.getName());
            branch.setRepositoryId(repository.getId());
            branch.setHeads(heads);
            branches.add(branch);
        }

        return branches;
    }

    private BitbucketBranchesAndTags retrieveBranchesAndTags(final Repository repository)
    {
        return new Retryer<BitbucketBranchesAndTags>().retry(new Callable<BitbucketBranchesAndTags>()
        {
            @Override
            public BitbucketBranchesAndTags call() throws Exception
            {
                return getBranchesAndTags(repository);
            }
        });
    }

    private BitbucketBranchesAndTags getBranchesAndTags(Repository repository)
    {
        try
        {
            BitbucketRemoteClient remoteClient = bitbucketClientBuilderFactory.forRepository(repository).cached().build();
            // Using undocumented https://api.bitbucket.org/1.0/repositories/atlassian/jira-bitbucket-connector/branches-tags
            return remoteClient.getBranchesAndTagsRemoteRestpoint().getBranchesAndTags(repository.getOrgName(),repository.getSlug());
        } catch (BitbucketRequestException e)
        {
            log.debug("Could not retrieve list of branches", e);
            throw new SourceControlException("Could not retrieve list of branches", e);
        } catch (JsonParsingException e)
        {
            log.debug("The response could not be parsed", e);
            throw new SourceControlException.InvalidResponseException("Could not retrieve list of branches", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setupPostcommitHook(Repository repository, String postCommitUrl)
    {
        BitbucketRemoteClient remoteClient = bitbucketClientBuilderFactory.forRepository(repository).closeIdleConnections().build();

        try
        {
            if (!hookDoesExist(repository, postCommitUrl, remoteClient, ServiceRemoteRestpoint.SERVICE_TYPE_POST)) {
	            remoteClient.getServicesRest().addPOSTService(repository.getOrgName(), // owner
	                    repository.getSlug(), postCommitUrl);
            }
        } catch (BitbucketRequestException e)
        {
            throw new SourceControlException.PostCommitHookRegistrationException("Could not add postcommit hook", e);
        }
        try
        {
            if (!hookDoesExist(repository, postCommitUrl, remoteClient, ServiceRemoteRestpoint.SERVICE_TYPE_PULL_REQUEST_POST)) {
	            remoteClient.getServicesRest().addPullRequestPOSTService(repository.getOrgName(), // owner
	                    repository.getSlug(), postCommitUrl);
            }


        } catch (BitbucketRequestException e)
        {
            throw new SourceControlException.PostCommitHookRegistrationException("Could not add pull request postcommit hook", e);
        }
    }

	private boolean hookDoesExist(Repository repository, String postCommitUrl,
            BitbucketRemoteClient remoteClient, String type)
    {
	    List<BitbucketServiceEnvelope> services = remoteClient.getServicesRest().getAllServices(
	            repository.getOrgName(), // owner
	            repository.getSlug());
	    for (BitbucketServiceEnvelope bitbucketServiceEnvelope : services)
	    {
            String serviceType = bitbucketServiceEnvelope.getService().getType();
            if (type.equals(serviceType))
            {
                for (BitbucketServiceField serviceField : bitbucketServiceEnvelope.getService().getFields())
                {
                    boolean fieldNameIsUrl = serviceField.getName().equals("URL");
                    boolean fieldValueIsRequiredPostCommitUrl = serviceField.getValue().equals(postCommitUrl);

                    if (fieldNameIsUrl && fieldValueIsRequiredPostCommitUrl)
                    {
                        return true;
                    }
                }
            }
	    }
	    return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void linkRepository(Repository repository, Set<String> withProjectkeys)
    {
        try
        {
            bitbucketLinker.linkRepository(repository, withProjectkeys);
        } catch (Exception e)
        {
            log.warn("Failed to link repository " + repository.getName() + " : " + e.getClass() + " :: "
                    + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void linkRepositoryIncremental(Repository repository, Set<String> withPossibleNewProjectkeys)
    {
        try
        {
            bitbucketLinker.linkRepositoryIncremental(repository, withPossibleNewProjectkeys);
        } catch (Exception e)
        {
            log.warn("Failed to do incremental repository linking " + repository.getName() + " : " + e.getClass()
                    + " :: " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removePostcommitHook(Repository repository, String postCommitUrl)
    {
        try
        {
            bitbucketLinker.unlinkRepository(repository);
            BitbucketRemoteClient remoteClient = bitbucketClientBuilderFactory.forRepository(repository).build();
            List<BitbucketServiceEnvelope> services = remoteClient.getServicesRest().getAllServices(
                    repository.getOrgName(), // owner
                    repository.getSlug());

            for (BitbucketServiceEnvelope bitbucketServiceEnvelope : services)
            {
                for (BitbucketServiceField serviceField : bitbucketServiceEnvelope.getService().getFields())
                {
                    boolean fieldNameIsUrl = serviceField.getName().equals("URL");
                    boolean fieldValueIsRequiredPostCommitUrl = serviceField.getValue().equals(postCommitUrl);

                    if (fieldNameIsUrl && fieldValueIsRequiredPostCommitUrl)
                    {
                        remoteClient.getServicesRest().deleteService(repository.getOrgName(), // owner
                                repository.getSlug(), bitbucketServiceEnvelope.getId());
                    }
                }
            }
        } catch (BitbucketRequestException e)
        {
            throw new SourceControlException.PostCommitHookRegistrationException("Could not remove postcommit hook", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCommitUrl(Repository repository, Changeset changeset)
    {
        return MessageFormat.format("{0}/{1}/{2}/changeset/{3}?dvcsconnector={4}", repository.getOrgHostUrl(),
                repository.getOrgName(), repository.getSlug(), changeset.getNode(), pluginVersion);
    }

    @Override
    public String getBranchUrl(Repository repository, Branch branch)
    {
        return MessageFormat.format("{0}/{1}/{2}/branch/{3}", repository.getOrgHostUrl(),
                repository.getOrgName(), repository.getSlug(), branch.getName());
    }

    @Override
    public String getCreatePullRequestUrl(Repository repository, String sourceSlug, final String sourceBranch, String destinationSlug, final String destinationBranch, String eventSource)
    {
        return URLPathFormatter.format("{0}/{1}/{2}/pull-request/new?source={3}/{4}&dest={5}/{6}&event_source={7}", repository.getOrgHostUrl(),
                repository.getOrgName(), repository.getSlug(), sourceSlug, sourceBranch, destinationSlug, destinationBranch, eventSource);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFileCommitUrl(Repository repository, Changeset changeset, String file, int index)
    {
        return MessageFormat.format("{0}#chg-{1}", getCommitUrl(repository, changeset), file);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DvcsUser getUser(Repository repository, String author)
    {
        BitbucketRemoteClient remoteClient = bitbucketClientBuilderFactory.forRepository(repository).build();
        BitbucketAccount bitbucketAccount = remoteClient.getAccountRest().getUser(author);
        String username = bitbucketAccount.getUsername();
        String fullName = bitbucketAccount.getFirstName() + " " + bitbucketAccount.getLastName();
        String avatar = bitbucketAccount.getAvatar();
        return new DvcsUser(username, fullName, null, avatar, repository.getOrgHostUrl() + "/" + username);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DvcsUser getTokenOwner(Organization organization)
    {
        BitbucketRemoteClient remoteClient = bitbucketClientBuilderFactory.forOrganization(organization).build();
        BitbucketAccount bitbucketAccount = remoteClient.getAccountRest().getCurrentUser();
        String username = bitbucketAccount.getUsername();
        String fullName = bitbucketAccount.getFirstName() + " " + bitbucketAccount.getLastName();
        String avatar = bitbucketAccount.getAvatar();
        return new DvcsUser(username, fullName, null, avatar, organization.getHostUrl() + "/" + username);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Group> getGroupsForOrganization(Organization organization)
    {
        try
        {
            BitbucketRemoteClient remoteClient = bitbucketClientBuilderFactory.forOrganization(organization).build();
            List<BitbucketGroup> groups = remoteClient.getGroupsRest().getGroups(organization.getName()); // owner

            return GroupTransformer.fromBitbucketGroups(groups);

        } catch (BitbucketRequestException.Forbidden_403 e)
        {
            log.debug("Could not get groups for organization [" + organization.getName() + "]");
            throw new SourceControlException.Forbidden_403(e);

        } catch (BitbucketRequestException e)
        {
            log.debug("Could not get groups for organization [" + organization.getName() + "]");
            throw new SourceControlException(e);

        } catch (JsonParsingException e)
        {
            log.debug(e.getMessage(), e);
            throw new SourceControlException.InvalidResponseException("Could not parse response [" + organization.getName()
                    + "]. This is most likely caused by invalid credentials.", e);

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supportsInvitation(Organization organization)
    {
        return organization.getDvcsType().equalsIgnoreCase(BITBUCKET);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void inviteUser(Organization organization, Collection<String> groupSlugs, String userEmail)
    {
        try
        {
            BitbucketRemoteClient remoteClient = bitbucketClientBuilderFactory.forOrganization(organization).build();
            for (String groupSlug : groupSlugs)
            {
                log.debug("Going invite " + userEmail + " to group " + groupSlug + " of bitbucket organization "
                        + organization.getName());
                remoteClient.getAccountRest().inviteUser(organization.getName(), userEmail, organization.getName(),
                        groupSlug);
            }
        } catch (BitbucketRequestException exception)
        {
            log.warn("Failed to invite user {} to organization {}. Response HTTP code {}", new Object[] { userEmail,
                    organization.getName(), exception.getClass().getName() });
        }
    }

    @Override
    public void startSynchronisation(Repository repo, EnumSet<SynchronizationFlag> flags, int auditId)
    {
        boolean softSync = flags.contains(SynchronizationFlag.SOFT_SYNC);
        boolean changestesSync = flags.contains(SynchronizationFlag.SYNC_CHANGESETS);
        boolean pullRequestSync = flags.contains(SynchronizationFlag.SYNC_PULL_REQUESTS);

        if (changestesSync)
        {
            // sync csets
            BranchFilterInfo filterNodes = getFilterNodes(repo);
            processBitbucketCsetSync(repo, softSync, filterNodes, auditId);

            branchService.updateBranchHeads(repo, filterNodes.newBranches, filterNodes.oldHeads);
            branchService.updateBranches(repo, filterNodes.newBranches);
        }
        // sync pull requests
        if (pullRequestSync)
        {
            processBitbucketPrSync(repo, softSync, auditId);
        }
    }

    protected BranchFilterInfo getFilterNodes(Repository repository)
    {
        List<Branch> newBranches = getBranches(repository);
        List<BranchHead> oldBranches = branchService.getListOfBranchHeads(repository);

        List<String> exclude = extractBranchHeads(oldBranches);

        BranchFilterInfo filter = new BranchFilterInfo(newBranches, oldBranches, exclude);
        return filter;
    }

    public static String getApiUrl(String hostUrl)
    {
        return hostUrl + "/!api/1.0";
    }


    private static class BranchFilterInfo
    {
        private List<Branch> newBranches;
        private List<BranchHead> oldHeads;
        private List<String> oldHeadsHashes;

        public BranchFilterInfo(List<Branch> newBranches, List<BranchHead> oldHeads, List<String> oldHeadsHashes)
        {
            super();
            this.newBranches = newBranches;
            this.oldHeads = oldHeads;
            this.oldHeadsHashes = oldHeadsHashes;
        }
    }

    private void processBitbucketCsetSync(Repository repository, boolean softSync, BranchFilterInfo filterNodes, int auditId)
    {
        List<Branch> newBranches = filterNodes.newBranches;

        if (filterNodes.oldHeads.isEmpty() && !changesetCache.isEmpty(repository.getId()))
        {
            log.info("No previous branch heads were found, switching to old changeset synchronization for repository [{}].", repository.getId());
            Date synchronizationStartedAt = new Date();
            for (Branch branch : newBranches)
            {
                for (BranchHead branchHead : branch.getHeads())
                {
                    OldBitbucketSynchronizeCsetMsg message = new OldBitbucketSynchronizeCsetMsg(repository, //
                            branchHead.getName(), branchHead.getHead(), //
                            synchronizationStartedAt, //
                            null, softSync, auditId);
                    MessageAddress<OldBitbucketSynchronizeCsetMsg> key = messagingService.get( //
                            OldBitbucketSynchronizeCsetMsg.class, //
                            OldBitbucketSynchronizeCsetMsgConsumer.KEY //
                    );
                    messagingService.publish(key, message, softSync ? MessagingService.SOFTSYNC_PRIORITY: MessagingService.DEFAULT_PRIORITY, messagingService.getTagForSynchronization(repository), messagingService.getTagForAuditSynchronization(auditId));
                }
            }
        } else
        {
            if (CollectionUtils.isEmpty(getInclude(filterNodes))) {
                log.debug("No new changesets detected for repository [{}].", repository.getSlug());
                return;
            }
            MessageAddress<BitbucketSynchronizeChangesetMessage> key = messagingService.get(
                    BitbucketSynchronizeChangesetMessage.class,
                    BitbucketSynchronizeChangesetMessageConsumer.KEY
            );
            Date synchronizationStartedAt = new Date();

            BitbucketSynchronizeChangesetMessage message = new BitbucketSynchronizeChangesetMessage(repository, synchronizationStartedAt,
                    (Progress) null, createInclude(filterNodes), filterNodes.oldHeadsHashes, 1, asNodeToBranches(filterNodes.newBranches), softSync, auditId);

            messagingService.publish(key, message, softSync ? MessagingService.SOFTSYNC_PRIORITY: MessagingService.DEFAULT_PRIORITY, messagingService.getTagForSynchronization(repository), messagingService.getTagForAuditSynchronization(auditId));
        }
    }

    private List<String> createInclude(BranchFilterInfo filterNodes)
    {
        List<String> newHeadsNodes = extractBranchHeadsFromBranches(filterNodes.newBranches);
        if (newHeadsNodes != null && filterNodes.oldHeadsHashes != null)
        {
            newHeadsNodes.removeAll(filterNodes.oldHeadsHashes);
        }
        return newHeadsNodes;
    }

    protected void processBitbucketPrSync(Repository repo, boolean softSync, int auditId)
    {
        MessageAddress<BitbucketSynchronizeActivityMessage> key = messagingService.get( //
                BitbucketSynchronizeActivityMessage.class, //
                BitbucketSynchronizeActivityMessageConsumer.KEY //
        );
        messagingService.publish(key, new BitbucketSynchronizeActivityMessage(repo, softSync, repo.getActivityLastSync(), auditId), softSync? MessagingService.SOFTSYNC_PRIORITY : MessagingService.DEFAULT_PRIORITY,
                messagingService.getTagForSynchronization(repo), messagingService.getTagForAuditSynchronization(auditId));
    }

    private Collection<String> getInclude(BranchFilterInfo filterNodes)
    {
        List<String> newNodes = extractBranchHeadsFromBranches(filterNodes.newBranches);
        if (newNodes != null && filterNodes.oldHeadsHashes != null)
        {
            newNodes.removeAll(filterNodes.oldHeadsHashes);
        }
        return newNodes;
    }


    private Map<String, String> asNodeToBranches(List<Branch> list)
    {
        Map<String, String> changesetBranch = new HashMap<String, String>();
        for (Branch branch : list)
        {
            for (BranchHead branchHead : branch.getHeads())
            {
                changesetBranch.put(branchHead.getHead(), branch.getName());
            }
        }
        return changesetBranch;
    }
}
