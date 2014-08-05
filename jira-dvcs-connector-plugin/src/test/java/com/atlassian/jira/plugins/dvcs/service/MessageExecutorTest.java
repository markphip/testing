package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.beehive.ClusterLockService;
import com.atlassian.beehive.SimpleClusterLockService;
import com.atlassian.beehive.compat.ClusterLockServiceFactory;
import com.atlassian.jira.plugins.dvcs.event.RepositorySync;
import com.atlassian.jira.plugins.dvcs.event.RepositorySyncHelper;
import com.atlassian.jira.plugins.dvcs.event.ThreadEventsCaptor;
import com.atlassian.jira.plugins.dvcs.model.DefaultProgress;
import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.message.BaseProgressEnabledMessage;
import com.atlassian.jira.plugins.dvcs.service.message.HasProgress;
import com.atlassian.jira.plugins.dvcs.service.message.MessageAddress;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.service.remote.SyncDisabledHelper;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.githubenterprise.GithubEnterpriseCommunicator;
import com.google.common.util.concurrent.MoreExecutors;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.EnumSet;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.util.ReflectionTestUtils.setField;

/**
 * MessageExecutor test
 */
//@Listeners (MockitoTestNgListener.class)
public class MessageExecutorTest
{
    private static final String ADDRESS_ID = "address";
    private static final String SYNC_TAG = "synchronization-repository-1234";

    private static final MessageAddress<MockPayload> MSG_ADDRESS = new MockAddress();

    @Mock
    private Repository repository;

    private ClusterLockService clusterLockService = new SimpleClusterLockService();

    @Mock
    private MessageConsumer consumer;

    @Mock
    private ClusterLockServiceFactory clusterLockServiceFactory;

    @Mock
    private MessagingService messagingService;

    @Mock
    private RepositorySyncHelper repoSyncHelper;

    @Mock
    private RepositorySync repoSync;

    @Mock
    private ThreadEventsCaptor threadEventsCaptor;

    @Mock
    private Message message;

    @Mock
    private HasProgress payload;

    @Mock
    private Progress progress;

    @Mock
    private SyncDisabledHelper syncDisabledHelper;

    @InjectMocks
    private MessageExecutor messageExecutor;

    @BeforeMethod
    public void setUp() throws Exception
    {
        // create and inject the MessageExecutor
        messageExecutor = new MessageExecutor(MoreExecutors.sameThreadExecutor());
        initMocks(this);
        Mockito.reset(messagingService);
        setField(messageExecutor, "consumers", new MessageConsumer<?>[] { consumer });

        when(consumer.getAddress()).thenReturn(MSG_ADDRESS);
        when(consumer.getParallelThreads()).thenReturn(1);

        when(clusterLockServiceFactory.getClusterLockService()).thenReturn(clusterLockService);
        when(repoSyncHelper.startSync(any(Repository.class), any(EnumSet.class))).thenReturn(repoSync);

        messageExecutor.init();

        when(messagingService.getRepositoryFromMessage(eq(message))).thenReturn(repository);
        when(messagingService.getNextMessageForConsuming(eq(consumer), eq(ADDRESS_ID))).thenReturn(message, null);
        when(messagingService.deserializePayload(eq(message))).thenReturn(payload);
        when(messagingService.getTagForSynchronization(eq(repository))).thenReturn(SYNC_TAG);
        when(payload.getProgress()).thenReturn(progress);
        when(payload.getRepository()).thenReturn(repository);

        when(repository.getId()).thenReturn(1234);
    }

    @AfterMethod
    public void tearDown() throws Exception
    {
        messageExecutor.destroy();
    }

    @Test
    public void immediateMessagesDisablingTest_Bitbucket() throws Exception
    {
        when(repository.getDvcsType()).thenReturn(BitbucketCommunicator.BITBUCKET);
        when(syncDisabledHelper.isBitbucketSyncDisabled()).thenReturn(true);
        messageExecutor.notify(ADDRESS_ID);

        // calling destroy to force internal executor to shutdown and wait for execution to finish for 1 minute
        messageExecutor.destroy();
        verify(messagingService).delayAll(eq(SYNC_TAG));
    }

    @Test
    public void immediateMessagesDisablingTest_GitHub() throws Exception
    {
        when(repository.getDvcsType()).thenReturn(GithubCommunicator.GITHUB);
        when(syncDisabledHelper.isGithubSyncDisabled()).thenReturn(true);
        messageExecutor.notify(ADDRESS_ID);

        // calling destroy to force internal executor to shutdown and wait for execution to finish for 1 minute
        messageExecutor.destroy();
        verify(messagingService).delayAll(eq(SYNC_TAG));
    }

    @Test
    public void immediateMessagesDisablingTest_GitHubEnteprise() throws Exception
    {
        when(repository.getDvcsType()).thenReturn(GithubEnterpriseCommunicator.GITHUB_ENTERPRISE);
        when(syncDisabledHelper.isGithubEnterpriseSyncDisabled()).thenReturn(true);
        messageExecutor.notify(ADDRESS_ID);

        // calling destroy to force internal executor to shutdown and wait for execution to finish for 1 minute
        messageExecutor.destroy();
        verify(messagingService).delayAll(eq(SYNC_TAG));
    }

    @Test
    public void executorShouldTryToEndProgressAfterProcessingSmartCommits() throws Exception
    {
        final MockPayload payload = new MockPayload();
        final Message<MockPayload> message = createMessage();

        when(messagingService.getNextMessageForConsuming(consumer, MSG_ADDRESS.getId())).thenReturn(message, (Message) null);
        when(messagingService.deserializePayload(message)).thenReturn(payload);
        when(messagingService.getRepositoryFromMessage(message)).thenReturn(repository);

        // get the consumer to check the queue
        messageExecutor.notify(MSG_ADDRESS.getId());

        // the executor must store the events before trying to end progress
        InOrder inOrder = Mockito.inOrder(repoSync, messagingService);
        inOrder.verify(repoSync).finish();
        inOrder.verify(messagingService).tryEndProgress(repository, payload.getProgress(), consumer, 0);
    }

    private Message<MockPayload> createMessage()
    {
        Message<MockPayload> message = new Message<MockPayload>();
        message.setAddress(MSG_ADDRESS);
        message.setPayload("{}");
        message.setPayloadType(MockPayload.class);
        message.setTags(new String[] {});
        message.setPriority(0);

        return message;
    }

    private static class MockAddress implements MessageAddress<MockPayload>
    {
        @Override
        public String getId()
        {
            return ADDRESS_ID;
        }

        @Override
        public Class<MockPayload> getPayloadType()
        {
            return MockPayload.class;
        }
    }

    private class MockPayload extends BaseProgressEnabledMessage
    {
        MockPayload()
        {
            super(new DefaultProgress(), 1, true, repository, false);
            getProgress().setSoftsync(true);
        }
    }
}
