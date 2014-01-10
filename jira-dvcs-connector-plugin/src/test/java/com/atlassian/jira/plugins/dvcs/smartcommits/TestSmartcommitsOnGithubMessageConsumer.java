package com.atlassian.jira.plugins.dvcs.smartcommits;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.LinkedIssueService;
import com.atlassian.jira.plugins.dvcs.service.LinkedIssueServiceImpl;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.github.message.SynchronizeChangesetMessage;
import com.atlassian.jira.plugins.dvcs.sync.GithubSynchronizeChangesetMessageConsumer;

public final class TestSmartcommitsOnGithubMessageConsumer
{
    @Mock
    private Repository repositoryMock;

    @Mock
    private Message<SynchronizeChangesetMessage> messageMock;

    @Mock
    private SynchronizeChangesetMessage payloadMock;

    @Mock
    private RepositoryService repositoryServiceMock;

    @Mock
    private DvcsCommunicatorProvider communicatorProviderMock;

    @Spy
    private final LinkedIssueService linkedIssueServiceSpy = new LinkedIssueServiceImpl();

    @Mock
    private ChangesetService changesetServiceMock;

    @Mock
    private MessagingService messagingServiceMock;

    @Mock
    private GithubCommunicator communicatorMock;

    @Mock
    private Progress progressMock;

    @Captor
    private ArgumentCaptor<Changeset> savedChangesetCaptor;

    @InjectMocks
    private GithubSynchronizeChangesetMessageConsumer consumer;

    private Changeset changesetWithJIRAIssueMock;

    @BeforeMethod
    private void initializeMocks()
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void foundIssueKey_ShouldMarkSmartcommit() throws InterruptedException
    {
        prepare("message MES-123 text");
        when(repositoryMock.isSmartcommitsEnabled()).thenReturn(true);
        when(communicatorMock.getDetailChangeset(repositoryMock, changesetWithJIRAIssueMock)).thenReturn(changesetWithJIRAIssueMock);

        consumer.onReceive(messageMock, payloadMock);

        verify(changesetServiceMock).create(savedChangesetCaptor.capture(), anySetOf(String.class));
        assertTrue("Smart commit should be available.", savedChangesetCaptor.getValue().isSmartcommitAvaliable());
    }

    @Test
    public void notFoundIssueKey_ShouldNotMarkSmartcommit() throws InterruptedException
    {
        prepare("message text no issue key");
        when(repositoryMock.isSmartcommitsEnabled()).thenReturn(true);

        consumer.onReceive(messageMock, payloadMock);

        verify(changesetServiceMock).create(savedChangesetCaptor.capture(), anySetOf(String.class));
        assertFalse("Smart commit should not be available.", savedChangesetCaptor.getValue().isSmartcommitAvaliable());
    }

    @Test
    public void issueKeyFoundSmartcommitsDisabled_ShouldNotMarkSmartcommit() throws InterruptedException
    {
        prepare("message MESS-123 issue key");
        when(repositoryMock.isSmartcommitsEnabled()).thenReturn(false);

        consumer.onReceive(messageMock, payloadMock);

        verify(changesetServiceMock).create(savedChangesetCaptor.capture(), anySetOf(String.class));
        assertTrue("Smart commit should not be available.", (savedChangesetCaptor.getValue().isSmartcommitAvaliable() == null)
                || !savedChangesetCaptor.getValue().isSmartcommitAvaliable());
    }

    protected void prepare(final String msg)
    {
        when(communicatorProviderMock.getCommunicator(anyString())).thenReturn(communicatorMock);
        when(payloadMock.getRepository()).thenReturn(repositoryMock);
        changesetWithJIRAIssueMock = changesetWithMessage(msg);
        when(communicatorMock.getChangeset(eq(repositoryMock), anyString())).thenReturn(changesetWithJIRAIssueMock);
        when(communicatorMock.getDetailChangeset(eq(repositoryMock), any(Changeset.class))).thenReturn(changesetWithJIRAIssueMock);
        when(payloadMock.isSoftSync()).thenReturn(true);
        when(payloadMock.getProgress()).thenReturn(progressMock);

    }

    private Changeset changesetWithMessage(final String msg)
    {
        return new Changeset(0, "node", msg, new Date());
    }

}