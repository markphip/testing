package com.atlassian.jira.plugins.dvcs.sync;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.DefaultProgress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.LinkedIssueService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.service.remote.CachingDvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetPage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketNewChangeset;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.message.BitbucketSynchronizeChangesetMessage;
import com.atlassian.jira.plugins.dvcs.model.Message;

import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.List;


public class BitbucketSynchronizeChangesetMessageConsumerTest
{
    @Mock
    private CachingDvcsCommunicator cachingCommunicator;
    @Mock
    private RepositoryService repositoryService;
    @Mock
    private LinkedIssueService linkedIssueService;
    @Mock
    private MessagingService messagingService;
    @Mock
    private ChangesetService changesetService;
    @Mock
    private BitbucketCommunicator communicator;
    @InjectMocks
    public BitbucketSynchronizeChangesetMessageConsumer messageConsumer;



    private Date lastCommitDate = new Date();
    private Date oldCommitDate = new Date(0);
    private BitbucketNewChangeset newChangeset1;
    private BitbucketNewChangeset newChangeset2;
    private Set<String> referencedProjects = new HashSet<String>();


    private static final BitbucketChangesetPage secondToLastChangesetPage =new BitbucketChangesetPage();

    private static final BitbucketChangesetPage lastChangesetPage = new BitbucketChangesetPage();

    private BitbucketSynchronizeChangesetMessage secondToLastmessage;
    private BitbucketSynchronizeChangesetMessage lastmessage;
    private Message<BitbucketSynchronizeChangesetMessage> message ;


    private Repository repository;

    @BeforeMethod
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        repository = new Repository();
        secondToLastmessage = setUpChangesetMessage(secondToLastChangesetPage);
        lastmessage = setUpChangesetMessage(lastChangesetPage);
        message = new Message<BitbucketSynchronizeChangesetMessage>();


        setUpChangesetPages();
        when(cachingCommunicator.getDelegate()).thenReturn(communicator);
        when(communicator.getNextPage(any(Repository.class),
                any(List.class), any(List.class), any(BitbucketChangesetPage.class))).thenReturn(lastChangesetPage);


    }

    @Test
    public void testOnReceiveLastMessage() throws Exception
    {
        when(communicator.getNextPage(any(Repository.class),
                any(List.class), any(List.class), any(BitbucketChangesetPage.class))).thenReturn(lastChangesetPage);
        when(changesetService.getByNode(anyInt(),anyString())).thenReturn(null);
        when(changesetService.findReferencedProjects(anyInt())).thenReturn(referencedProjects);
        messageConsumer.onReceive(message, secondToLastmessage);
        verify(cachingCommunicator).linkRepository(repository, referencedProjects);


    }

    @Test
    public void testOnReceiveSecondToLastMessage() throws Exception
    {
        when(communicator.getNextPage(any(Repository.class),
                any(List.class), any(List.class), any(BitbucketChangesetPage.class))).thenReturn(secondToLastChangesetPage);

        when(changesetService.getByNode(anyInt(), anyString())).thenReturn(null);
        when(changesetService.findReferencedProjects(anyInt())).thenReturn(referencedProjects);
        messageConsumer.onReceive(message, secondToLastmessage);
        verify(cachingCommunicator, never()).linkRepository(any(Repository.class), any(Set.class));

    }

    @Test
    public void testOnReceiveWhenRepoLastCommitDateNonexistent() throws Exception
    {
        when(changesetService.getByNode(anyInt(), anyString())).thenReturn(null);
        messageConsumer.onReceive(message, secondToLastmessage);
        verify(repositoryService).save(repository);

    }

    @Test
    public void testOnReceiveWhenRepoLastCommitDateOld() throws Exception
    {
        repository.setLastCommitDate(oldCommitDate);
        when(changesetService.getByNode(anyInt(), anyString())).thenReturn(null);
        messageConsumer.onReceive(message, secondToLastmessage);
        verify(repositoryService).save(repository);

    }

    @Test
    public void testOnReceiveWhenRepoLastCommitNonStale() throws Exception
    {
        repository.setLastCommitDate(new Date());
        when(changesetService.getByNode(anyInt(), anyString())).thenReturn(null);
        messageConsumer.onReceive(message, lastmessage);
        verify(repositoryService, never()).save(repository);

    }



    @Test
    public void testOnReceiveWhenChangesetAlreadySeenEarlier() throws Exception
    {
        when(changesetService.getByNode(anyInt(), anyString())).thenReturn(
                new Changeset(1, "a changeset that's already in the db", "", new Date()));
        messageConsumer.onReceive(message, lastmessage);
        verify(changesetService, never()).create(any(Changeset.class), any(Set.class));


    }


    private void setUpChangesetPages(){
        newChangeset1 = new BitbucketNewChangeset();
        newChangeset2 = new BitbucketNewChangeset();
        newChangeset1.setParents(new ArrayList<BitbucketNewChangeset>());
        newChangeset2.setParents(new ArrayList<BitbucketNewChangeset>());
        newChangeset1.setDate(lastCommitDate);
        newChangeset2.setDate(lastCommitDate);
        secondToLastChangesetPage.setNext("a string whose presence indicates that this is not the last page");
        ArrayList<BitbucketNewChangeset> newChangesets1 = new ArrayList<BitbucketNewChangeset>();
        ArrayList<BitbucketNewChangeset> newChangesets2 = new ArrayList<BitbucketNewChangeset>();
        newChangesets1.add(newChangeset1);
        newChangesets2.add(newChangeset2);
        secondToLastChangesetPage.setValues(newChangesets1);

        lastChangesetPage.setValues(newChangesets2);

    }

    private BitbucketSynchronizeChangesetMessage setUpChangesetMessage(BitbucketChangesetPage changesetPage){
        return new BitbucketSynchronizeChangesetMessage(repository,
                new Date(),
                new DefaultProgress(),
                new ArrayList<String>(),
                new ArrayList<String>(),
                changesetPage,
                new HashMap<String,String>(),
                false,
                0,
                false);
    }

}