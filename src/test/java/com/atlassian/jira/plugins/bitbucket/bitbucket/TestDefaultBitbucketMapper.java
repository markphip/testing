package com.atlassian.jira.plugins.bitbucket.bitbucket;

import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v1.IssueMapping;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v1.ProjectMapping;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.Encryptor;
import com.atlassian.jira.plugins.bitbucket.api.impl.DefaultRepositoryPersister;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.BitbucketCommunicator;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.BitbucketRepository;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.RepositoryUri;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl.BitbucketRepositoryManager;
import com.atlassian.sal.api.transaction.TransactionCallback;

/**
 * Unit tests for {@link DefaultRepositoryPersister}
 */
public class TestDefaultBitbucketMapper
{
    private static final String URI = "https://bitbucket.org/owner/slug";
	@Mock
    ActiveObjects activeObjects;
    @Mock
    BitbucketCommunicator bitbucket;
    @Mock
    ProjectMapping projectMapping;
    @Mock
    IssueMapping issueMapping;
    @Mock
    BitbucketRepository repository;
    @Mock
    Changeset changeset;
    @Mock
    Encryptor encryptor;

    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        when(projectMapping.getRepositoryUri()).thenReturn(URI);

        when(activeObjects.find(ProjectMapping.class, "PROJECT_KEY = ?", "JST")).thenReturn(
                new ProjectMapping[]{projectMapping});
        when(activeObjects.create(eq(ProjectMapping.class), anyMap())).thenReturn(projectMapping);
        when(repository.getOwner()).thenReturn("owner");
        when(repository.getSlug()).thenReturn("slug");
        when(repository.getRepositoryUrl()).thenReturn("https://bitbucket.org/owner/slug");
        when(changeset.getRepositoryUrl()).thenReturn("https://bitbucket.org/owner/slug");
        when(changeset.getBranch()).thenReturn("default");
        when(changeset.getNode()).thenReturn("1");
        //noinspection unchecked
        when(activeObjects.executeInTransaction(isA(TransactionCallback.class))).thenAnswer(
                new Answer<Object>()
                {
                    public Object answer(InvocationOnMock invocationOnMock) throws Throwable
                    {
                        return ((TransactionCallback) invocationOnMock.getArguments()[0]).doInTransaction();
                    }
                }
        );
        when(encryptor.encrypt(anyString(), anyString(), anyString())).thenAnswer(
                new Answer<String>()
                {
                    public String answer(InvocationOnMock invocationOnMock) throws Throwable
                    {
                        return StringUtils.reverse(String.valueOf(invocationOnMock.getArguments()[0]));
                    }
                }
        );
        when(encryptor.decrypt(anyString(), anyString(), anyString())).thenAnswer(
                new Answer<String>()
                {
                    public String answer(InvocationOnMock invocationOnMock) throws Throwable
                    {
                        return StringUtils.reverse(String.valueOf(invocationOnMock.getArguments()[0]));
                    }
                }
        );
    }

    @Test
    public void testAddAnonymousRepositoryCreatesValidMap()
    {
        new DefaultRepositoryPersister(activeObjects).
                addRepository("JST", RepositoryUri.parse(URI).getRepositoryUrl(), null, null);
        verify(activeObjects, times(1)).create(eq(ProjectMapping.class),
                argThat(new ArgumentMatcher<Map<String, Object>>()
                {
                    @Override
					public boolean matches(Object o)
                    {
                        //noinspection unchecked
                        Map<String, Object> map = (Map<String, Object>) o;
                        return map.get("REPOSITORY_URI").equals(URI) &&
                                map.get("PROJECT_KEY").equals("JST") &&
                                !map.containsKey("USERNAME") && !map.containsKey("PASSWORD");
                    }
                }));
    }

    @Test
    public void testAddAuthentictedRepositoryCreatesValidMap()
    {
        new DefaultRepositoryPersister(activeObjects).
                addRepository("JST", RepositoryUri.parse(URI).getRepositoryUrl(), "user", "pass");
        verify(activeObjects, times(1)).create(eq(ProjectMapping.class),
                argThat(new ArgumentMatcher<Map<String, Object>>()
                {
                    @Override
					public boolean matches(Object o)
                    {
                        //noinspection unchecked
                        Map<String, Object> map = (Map<String, Object>) o;
                        return map.get("REPOSITORY_URI").equals(URI) &&
                                map.get("PROJECT_KEY").equals("JST") &&
                                map.get("USERNAME").equals("user") &&
                                map.containsKey("PASSWORD");
                    }
                }));
    }

    @Test
    public void testPasswordNotStoredInPlainText()
    {
    	new BitbucketRepositoryManager(new DefaultRepositoryPersister(activeObjects), bitbucket, encryptor)
    		.addRepository("JST", RepositoryUri.parse(URI).getRepositoryUrl(), "user", "pass");
        verify(activeObjects, times(1)).create(eq(ProjectMapping.class),
                argThat(new ArgumentMatcher<Map<String, Object>>()
                {
                    @Override
					public boolean matches(Object o)
                    {
                        //noinspection unchecked
                        Map<String, Object> map = (Map<String, Object>) o;
                        return !map.get("PASSWORD").equals("pass");
                    }
                }));
        verify(encryptor, times(1)).encrypt("pass", "JST", "https://bitbucket.org/owner/slug");
    }

    @Test
    public void testRemoveRepositoryAlsoRemovesIssues()
    {
        when(activeObjects.find(ProjectMapping.class,
                "PROJECT_KEY = ? and REPOSITORY_URI = ?",
                "JST", URI)).thenReturn(new ProjectMapping[]{projectMapping}
        );
        when(activeObjects.find(IssueMapping.class,
                "PROJECT_KEY = ? and REPOSITORY_URI = ?",
                "JST", URI)).thenReturn(new IssueMapping[]{issueMapping}
        );
        new DefaultRepositoryPersister(activeObjects).removeRepository("JST",
                RepositoryUri.parse(URI).getRepositoryUrl());
        verify(activeObjects, times(1)).find(ProjectMapping.class,
                "PROJECT_KEY = ? and REPOSITORY_URI = ?", "JST", URI);
        verify(activeObjects, times(1)).find(IssueMapping.class,
                "PROJECT_KEY = ? and REPOSITORY_URI = ?", "JST", URI);
        verify(activeObjects, times(1)).delete(projectMapping);
        verify(activeObjects, times(1)).delete(issueMapping);
    }

    @Test
    public void testGetChangesets()
    {
        when(activeObjects.find(ProjectMapping.class,
                "PROJECT_KEY = ? and REPOSITORY_URI = ?", "JST", URI)).
                thenReturn(new ProjectMapping[]{projectMapping});
        when(activeObjects.find(IssueMapping.class,
                "ISSUE_ID = ?", "JST-1")).thenReturn(new IssueMapping[]{issueMapping});
        when(issueMapping.getNode()).thenReturn("1");
        when(issueMapping.getRepositoryUri()).thenReturn(URI);
        new DefaultRepositoryPersister(activeObjects).getIssueMappings("JST-1");
//        verify(activeObjects, times(1)).find(ProjectMapping.class,
//                "PROJECT_KEY = ? and REPOSITORY_URI = ?", "JST", "owner/slug");
        verify(activeObjects, times(1)).find(IssueMapping.class,
                "ISSUE_ID = ?", "JST-1");
//        verify(bitbucket, times(1)).getChangeset(argThat(new ArgumentMatcher<Authentication>()
//        {
//            @Override
//            public boolean matches(Object o)
//            {
//                return o == Authentication.ANONYMOUS;
//            }
//        }), eq("owner"), eq("slug"), eq("1"));
    }

    @Test
    public void testGetChangesetsOnAuthenticatedRepository()
    {
        when(activeObjects.find(ProjectMapping.class,
                "PROJECT_KEY = ? and REPOSITORY_URI = ?",
                "JST", URI)).thenReturn(new ProjectMapping[]{projectMapping});
        when(activeObjects.find(IssueMapping.class,
                "ISSUE_ID = ?", "JST-1")).thenReturn(new IssueMapping[]{issueMapping});
        when(projectMapping.getUsername()).thenReturn("user");
        when(projectMapping.getPassword()).thenReturn("ssap");
        when(issueMapping.getNode()).thenReturn("1");
        when(issueMapping.getRepositoryUri()).thenReturn(URI);
        new DefaultRepositoryPersister(activeObjects).getIssueMappings("JST-1");
//        verify(activeObjects, times(1)).find(ProjectMapping.class,
//                "PROJECT_KEY = ? and REPOSITORY_URI = ?",
//                "JST", "owner/slug");
        verify(activeObjects, times(1)).find(IssueMapping.class,
                "ISSUE_ID = ?", "JST-1");
//        verify(bitbucket, times(1)).getChangeset(argThat(new ArgumentMatcher<Authentication>()
//        {
//            @Override
//            public boolean matches(Object o)
//            {
//                BasicAuthentication auth = (BasicAuthentication) o;
//                return auth.getUsername().equals("user") && auth.getPassword().equals("pass");
//            }
//        }), eq("owner"), eq("slug"), eq("1"));
    }

    @Test
    public void testAddChangesetToSameBranch()
    {
        when(activeObjects.find(ProjectMapping.class,
            "PROJECT_KEY = ? and REPOSITORY_URI = ?",
            "JST", URI)).thenReturn(new ProjectMapping[]{projectMapping});

        new DefaultRepositoryPersister(activeObjects).addChangeset("JST-1", changeset);
        verify(activeObjects, times(1)).create(eq(IssueMapping.class),
                argThat(new ArgumentMatcher<Map<String, Object>>()
                {
                    @Override
					public boolean matches(Object o)
                    {
                        //noinspection unchecked
                        Map<String, Object> map = (Map<String, Object>) o;
                        return map.get("REPOSITORY_URI").equals(URI) &&
                                map.get("PROJECT_KEY").equals("JST") &&
                                map.get("NODE").equals("1") &&
                                map.get("ISSUE_ID").equals("JST-1");
                    }
                }));
    }
}
