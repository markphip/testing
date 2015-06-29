package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.SmartCommitsAnalyticsService;
import com.atlassian.jira.plugins.dvcs.dao.OrganizationDao;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;


import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class OrganizationServiceImplTest
{
    @Mock
    Organization organization;

    @Mock
    OrganizationDao organizationDao;

    @Mock
    SmartCommitsAnalyticsService smartCommitsAnalyticsService;

    @InjectMocks
    OrganizationServiceImpl classUnderTest;

    private final int orgId = 1;

    @Rule
    public final MethodRule initMockito = MockitoJUnit.rule();


    @Test
    public void testEnableSmartcommitsOnNewRepos() throws Exception
    {
        when(organizationDao.get(orgId)).thenReturn(organization);
        when(organizationDao.save(organization)).thenReturn(organization);

        classUnderTest.enableSmartcommitsOnNewRepos(1, true);
        verify(smartCommitsAnalyticsService).fireSmartCommitAutoEnabledConfigChange(orgId, true);
    }

}