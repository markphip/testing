package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.message.HasProgress;
import com.atlassian.jira.plugins.dvcs.service.message.MessageAddress;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.service.remote.SyncDisabledHelper;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MessageExecutor test
 */
public class MessageExecutorTest
{
    private static final String ADDRESS_ID = "address";

    @InjectMocks
    private MessageExecutor messageExecutor;

    @Mock
    private MessageConsumer messageConsumer;

    @Mock
    private MessageAddress messageAddress;

    @Mock
    private MessagingService messagingService;

    @Mock
    private Message message;

    @Mock
    private HasProgress payload;

    @Mock
    private Progress progress;

    @Mock
    private Repository repository;

    @Mock
    private SyncDisabledHelper syncDisabledHelper;

    @BeforeMethod
    public void init()
    {
        MockitoAnnotations.initMocks(this);

        when(messageAddress.getId()).thenReturn(ADDRESS_ID);

        when(messageConsumer.getAddress()).thenReturn(messageAddress);
        when(messageConsumer.getParallelThreads()).thenReturn(1);
        MessageConsumer<?>[] consumers = new MessageConsumer[] { messageConsumer };

        ReflectionTestUtils.setField(messageExecutor, "consumers", consumers);
        messageExecutor.init();

        when(messagingService.getNextMessageForConsuming(eq(messageConsumer), eq(ADDRESS_ID))).thenReturn(message, null);
        when(messagingService.deserializePayload(eq(message))).thenReturn(payload);
        when(messagingService.getTagForSynchronization(eq(repository))).thenReturn("synchronization-repository-1234");
        when(payload.getProgress()).thenReturn(progress);
        when(payload.getRepository()).thenReturn(repository);

        when(repository.getDvcsType()).thenReturn("bitbucket");
        when(repository.getId()).thenReturn(1234);
    }

    @Test
    public void immediateMessagesDisablingTest() throws Exception
    {
        when(syncDisabledHelper.isBitbucketSyncDisabled()).thenReturn(true);
        messageExecutor.notify(ADDRESS_ID);

        // calling destroy to force internal executor to shutdown and wait for execution to finish for 1 minute
        messageExecutor.destroy();
        verify(messagingService).disableAll(eq("synchronization-repository-1234"));
    }
}
