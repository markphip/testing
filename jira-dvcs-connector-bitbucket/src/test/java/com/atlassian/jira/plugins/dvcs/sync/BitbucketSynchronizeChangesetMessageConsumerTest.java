package com.atlassian.jira.plugins.dvcs.sync;

import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.LinkedIssueService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.service.message.MessageAddress;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.service.remote.CachingDvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetPage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BitbucketRequestException;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.message.BitbucketSynchronizeChangesetMessage;
import com.google.common.collect.Lists;
import org.fest.assertions.api.Assertions;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testing BitbucketSynchronizeChangesetMessageConsumer
 */
public class BitbucketSynchronizeChangesetMessageConsumerTest
{
    @InjectMocks
    private BitbucketSynchronizeChangesetMessageConsumer consumer;

    @Mock
    private CachingDvcsCommunicator cachingCommunicator;
    @Mock
    private BitbucketCommunicator bitbucketCommunicator;
    @Mock
    private ChangesetService changesetService;
    @Mock
    private RepositoryService repositoryService;
    @Mock
    private LinkedIssueService linkedIssueService;
    @Mock
    private MessagingService messagingService;

    @Mock
    private Repository repository;

    @Captor
    private ArgumentCaptor<BitbucketSynchronizeChangesetMessage> messageCaptor;

    @BeforeMethod
    public void initializeMocks()
    {
        MockitoAnnotations.initMocks(this);
        when(cachingCommunicator.getDelegate()).thenReturn(bitbucketCommunicator);
    }

    @Test
    public void testConsumer_include()
    {
        List<String> include = Lists.newArrayList("1234567890");
        List<String> exclude = Lists.newArrayList("9876543210");
        Map<String, String> nodesToBranches = new HashMap<String, String>();
        
        BitbucketSynchronizeChangesetMessage payload = new BitbucketSynchronizeChangesetMessage(repository, null,
                null, include, exclude, null,
                nodesToBranches, true, 0);

        Message<BitbucketSynchronizeChangesetMessage> message = new Message<BitbucketSynchronizeChangesetMessage>();

        BitbucketRequestException.NotFound_404 e = new BitbucketRequestException.NotFound_404();
        e.setContent("{\"error\": {\"message\": \"Not found\"},\"data\": {\"shas\": [\"1234567890\"]}}");
        when(bitbucketCommunicator.getNextPage(any(Repository.class), anyListOf(String.class), anyListOf(String.class), any(BitbucketChangesetPage.class))).thenThrow(e);
        
        consumer.onReceive(message, payload);

        verify(messagingService).publish(any(MessageAddress.class), messageCaptor.capture(), anyInt(), (String[]) anyVararg());

        Assertions.assertThat(messageCaptor.getValue().getInclude()).doesNotContain("1234567890");
        Assertions.assertThat(messageCaptor.getValue().getExclude()).contains("9876543210");
    }

    @Test
    public void testConsumer_exclude()
    {
        List<String> include = Lists.newArrayList("1234567890");
        List<String> exclude = Lists.newArrayList("9876543210");
        Map<String, String> nodesToBranches = new HashMap<String, String>();

        BitbucketSynchronizeChangesetMessage payload = new BitbucketSynchronizeChangesetMessage(repository, null,
                null, include, exclude, null,
                nodesToBranches, true, 0);

        Message<BitbucketSynchronizeChangesetMessage> message = new Message<BitbucketSynchronizeChangesetMessage>();

        BitbucketRequestException.NotFound_404 e = new BitbucketRequestException.NotFound_404();
        e.setContent("{\"error\": {\"message\": \"Not found\"},\"data\": {\"shas\": [\"9876543210\"]}}");
        when(bitbucketCommunicator.getNextPage(any(Repository.class), anyListOf(String.class), anyListOf(String.class), any(BitbucketChangesetPage.class))).thenThrow(e);

        consumer.onReceive(message, payload);

        verify(messagingService).publish(any(MessageAddress.class), messageCaptor.capture(), anyInt(), (String[]) anyVararg());

        Assertions.assertThat(messageCaptor.getValue().getInclude()).contains("1234567890");
        Assertions.assertThat(messageCaptor.getValue().getExclude()).doesNotContain("9876543210");
    }
}
