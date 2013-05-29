package com.atlassian.jira.plugins.dvcs.smartcommits;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import net.java.ao.EntityStreamCallback;
import net.java.ao.Query;
import net.java.ao.RawEntity;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao;
import com.atlassian.jira.plugins.dvcs.dao.impl.ChangesetDaoImpl;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.CommitCommands;

@SuppressWarnings("all")
public class SmartcommitOperationTest
{

	CommitMessageParser commitMessageParser = new DefaultCommitMessageParser();

	@Mock
	SmartcommitsService smartcommitsServiceMock;

	@Mock
	ActiveObjects activeObjectsMock;

	ChangesetDao changesetDao;

	SmartcommitOperation operation;

	public SmartcommitOperationTest() {
		super();
	}

	@BeforeMethod
	public void setUp()
    {
        MockitoAnnotations.initMocks(this);

		changesetDao = new ChangesetDaoImpl(activeObjectsMock);

		operation = new SmartcommitOperation(changesetDao, commitMessageParser, smartcommitsServiceMock, null, null, null);

	}

	@SuppressWarnings("unchecked")
	@Test
	public void testRunOperation () {

		Mockito.doAnswer(new Answer<Object>()
		{
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable
			{
				((EntityStreamCallback)invocation.getArguments()[2]).onRowRead(sampleChangesetMapping());
				return null;
			}

		}).when(activeObjectsMock).stream(Mockito.isA(Class.class), Mockito.isA(Query.class), Mockito.isA(EntityStreamCallback.class));

		ChangesetMapping sampleChangesetMapping = (ChangesetMapping) sampleChangesetMapping();

		when (activeObjectsMock.get(eq( ChangesetMapping.class ), eq (1) )).thenReturn(sampleChangesetMapping);

		operation.run();

		verify(smartcommitsServiceMock).doCommands(any(CommitCommands.class));
	}

	private RawEntity sampleChangesetMapping()
	{
		ChangesetMapping changesetMappigMock = Mockito.mock(ChangesetMapping.class);

		when(changesetMappigMock.getID()).thenReturn(1);
		when(changesetMappigMock.getAuthorEmail()).thenReturn("sam@example.com");
		when(changesetMappigMock.getMessage()).thenReturn("HAD-4 #comment mighty comment");

		return changesetMappigMock;
	}


}

