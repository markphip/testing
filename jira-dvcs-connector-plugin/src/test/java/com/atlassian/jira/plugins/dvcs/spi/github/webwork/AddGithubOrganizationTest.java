package com.atlassian.jira.plugins.dvcs.spi.github.webwork;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.junit.rules.AvailableInContainer;
import com.atlassian.jira.plugins.dvcs.analytics.AnalyticsPossibleValues.DvcsType;
import com.atlassian.jira.plugins.dvcs.analytics.AnalyticsPossibleValues.FailureReason;
import com.atlassian.jira.plugins.dvcs.analytics.AnalyticsPossibleValues.Outcome;
import com.atlassian.jira.plugins.dvcs.analytics.AnalyticsPossibleValues.Source;
import com.atlassian.jira.plugins.dvcs.analytics.DvcsConfigAddEndedAnalyticsEvent;
import com.atlassian.jira.plugins.dvcs.analytics.DvcsConfigAddStartedAnalyticsEvent;
import com.atlassian.jira.plugins.dvcs.auth.OAuthStore;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubCommunicator;
import com.atlassian.jira.plugins.dvcs.util.TestNGMockComponentContainer;
import com.atlassian.jira.plugins.dvcs.util.TestNGMockHttp;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.sal.api.ApplicationProperties;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import webwork.action.Action;

import java.net.URLEncoder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static com.atlassian.jira.plugins.dvcs.webwork.CommonDvcsConfigurationAction.DEFAULT_SOURCE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class AddGithubOrganizationTest {
    private static final String SAMPLE_SOURCE = "devtools";
    private static final String SAMPLE_XSRF_TOKEN = "xsrfToken";
    private static final String SAMPLE_AUTH_URL = "http://authurl.com";

    private final TestNGMockComponentContainer mockComponentContainer = new TestNGMockComponentContainer(this);

    @Mock
    @AvailableInContainer
    private com.atlassian.jira.security.xsrf.XsrfTokenGenerator xsrfTokenGenerator;

    @Mock
    @AvailableInContainer
    private com.atlassian.jira.security.JiraAuthenticationContext jiraAuthenticationContext;

    @Mock
    private I18nHelper i18nHelper;

    @Mock
    @AvailableInContainer
    private com.atlassian.jira.web.action.RedirectSanitiser redirectSanitiser;

    @Mock
    @AvailableInContainer
    private com.atlassian.jira.config.properties.ApplicationProperties jiraApplicationProperties;

    private final TestNGMockHttp mockHttp = TestNGMockHttp.withMockitoMocks();
    private HttpServletRequest request;
    private HttpServletResponse response;

    @Mock
    private ApplicationProperties ap;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private OAuthStore oAuthStore;

    @Mock
    private GithubOAuthUtils githubOAuthUtils;

    @Mock
    private FeatureManager featureManager;

    @Mock
    private GithubCommunicator githubCommunicator;

    private AddGithubOrganization addGithubOrganization;

    private final String githubURL = "https://github.com/";
    private final String badAccountName = "I_AM_SURE_THIS_ACCOUNT_IS_INVALID";

    @BeforeMethod(alwaysRun=true)
    public void setup()
    {
        MockitoAnnotations.initMocks(this);

        mockComponentContainer.beforeMethod();
        mockHttp.beforeMethod();
        request = mockHttp.mockRequest();
        response = mockHttp.mockResponse();

        when(xsrfTokenGenerator.generateToken(request)).thenReturn(SAMPLE_XSRF_TOKEN);
        when(jiraAuthenticationContext.getI18nHelper()).thenReturn(i18nHelper);
        when(redirectSanitiser.makeSafeRedirectUrl(anyString())).then(returnsFirstArg()); // returns the same url
        when(jiraApplicationProperties.getEncoding()).thenReturn("UTF-8");

        when(oAuthStore.getClientId(anyString())).thenReturn("apiKey");
        when(oAuthStore.getSecret(anyString())).thenReturn("apiSecret");

        when(request.getParameter("oauth_verifier")).thenReturn("verifier");
        HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        when(githubOAuthUtils.createGithubRedirectUrl(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(SAMPLE_AUTH_URL);
        when(githubOAuthUtils.requestAccessToken(anyString())).thenReturn(Action.NONE);

        addGithubOrganization = new AddGithubOrganization(ap, eventPublisher, featureManager, oAuthStore, organizationService, githubCommunicator)
        {
            @Override
            GithubOAuthUtils getGithubOAuthUtils() {
                return githubOAuthUtils;
            }
        };
    }

    @AfterMethod
    public void tearDown()
    {
        ComponentAccessor.initialiseWorker(null); // reset
        mockComponentContainer.afterMethod();
        mockHttp.afterMethod();
    }

    @Test
    public void testDoExecuteAnalytics() throws Exception
    {
        addGithubOrganization.setSource(SAMPLE_SOURCE);
        String ret = addGithubOrganization.doExecute();
        assertThat(ret, equalTo(Action.NONE));
        verify(eventPublisher).publish(new DvcsConfigAddStartedAnalyticsEvent(Source.DEVTOOLS, DvcsType.GITHUB));
        verifyNoMoreInteractions(eventPublisher);
        verify(response).sendRedirect(eq(SAMPLE_AUTH_URL + URLEncoder.encode("&t=2", "utf8")));
        verifyNoMoreInteractions(response);
    }
    @Test
    public void testDoExecuteAnalyticsDefaultSource() throws Exception
    {
        addGithubOrganization.setSource(null);
        String ret = addGithubOrganization.doExecute();
        assertThat(ret, equalTo(Action.NONE));
        verify(eventPublisher).publish(new DvcsConfigAddStartedAnalyticsEvent(Source.UNKNOWN, DvcsType.GITHUB));
        verifyNoMoreInteractions(eventPublisher);
        verify(response).sendRedirect(eq(SAMPLE_AUTH_URL + URLEncoder.encode("&t=2", "utf8")));
        verifyNoMoreInteractions(response);
    }

    @Test
    public void testDoFinishAnalytics() throws Exception
    {
        addGithubOrganization.setSource(SAMPLE_SOURCE);
        String ret = addGithubOrganization.doFinish();
        assertThat(ret, equalTo(Action.NONE));
        verify(eventPublisher).publish(new DvcsConfigAddEndedAnalyticsEvent(Source.DEVTOOLS, DvcsType.GITHUB, Outcome.SUCCEEDED, null));
        verifyNoMoreInteractions(eventPublisher);
        verify(response).sendRedirect(eq("ConfigureDvcsOrganizations.jspa?atl_token=" + SAMPLE_XSRF_TOKEN + "&source=" + SAMPLE_SOURCE));
        verifyNoMoreInteractions(response);
    }

    @Test
    public void testDoFinishAnalyticsDefaultSource() throws Exception
    {
        addGithubOrganization.setSource(null);
        String ret = addGithubOrganization.doFinish();
        assertThat(ret, equalTo(Action.NONE));
        verify(eventPublisher).publish(new DvcsConfigAddEndedAnalyticsEvent(Source.UNKNOWN, DvcsType.GITHUB, Outcome.SUCCEEDED, null));
        verifyNoMoreInteractions(eventPublisher);
        verify(response).sendRedirect(eq("ConfigureDvcsOrganizations.jspa?atl_token=" + SAMPLE_XSRF_TOKEN)); // source parameter skipped
        verifyNoMoreInteractions(response);
    }

    @Test
    public void testDoFinishAnalyticsErrorGeneric() throws Exception
    {
        addGithubOrganization.setSource(SAMPLE_SOURCE);
        when(organizationService.save(any(Organization.class))).thenThrow(Exception.class);
        String ret = addGithubOrganization.doFinish();
        assertThat(ret, equalTo(Action.INPUT));
        verify(eventPublisher).publish(new DvcsConfigAddEndedAnalyticsEvent(Source.DEVTOOLS, DvcsType.GITHUB, Outcome.FAILED, FailureReason.OAUTH_GENERIC));
        verifyNoMoreInteractions(eventPublisher);
        verifyNoMoreInteractions(response);
    }

    @Test
    public void testDoFinishAnalyticsErrorSourceControl() throws Exception
    {
        addGithubOrganization.setSource(SAMPLE_SOURCE);
        reset(organizationService);
        when(organizationService.save(any(Organization.class))).thenThrow(SourceControlException.class);
        String ret = addGithubOrganization.doFinish();
        assertThat(ret, equalTo(Action.INPUT));
        verify(eventPublisher).publish(new DvcsConfigAddEndedAnalyticsEvent(Source.DEVTOOLS, DvcsType.GITHUB, Outcome.FAILED, FailureReason.OAUTH_SOURCECONTROL));
        verifyNoMoreInteractions(eventPublisher);
        verifyNoMoreInteractions(response);
    }

    @Test
    public void testDoValidationAnalyticsError() throws Exception
    {
        addGithubOrganization.setSource(SAMPLE_SOURCE);
        addGithubOrganization.setOrganization(null); // cause validation error
        addGithubOrganization.doValidation();
        verify(eventPublisher).publish(new DvcsConfigAddEndedAnalyticsEvent(Source.DEVTOOLS, DvcsType.GITHUB, Outcome.FAILED, FailureReason.VALIDATION));
        verifyNoMoreInteractions(eventPublisher);
    }

    @Test
    public void testDoValidationAnalyticsNoError() throws Exception
    {
        addGithubOrganization.setUrl(SAMPLE_SOURCE);
        addGithubOrganization.setOrganization("org");
        when(githubCommunicator.isUsernameCorrect(SAMPLE_SOURCE, "org")).thenReturn(true);
        addGithubOrganization.doValidation();
        verifyNoMoreInteractions(eventPublisher);
    }

    @Test
    public void testDisablingUserValidationDarkFeature()
    {
        addGithubOrganization.setSource(SAMPLE_SOURCE);
        addGithubOrganization.setOrganization("org");
        addGithubOrganization.setUrl(githubURL);
        when(featureManager.isEnabled(AddGithubOrganization.DISABLE_USERNAME_VALIDATION)).thenReturn(true);

        addGithubOrganization.doValidation();

        verifyNoMoreInteractions(eventPublisher);
    }

    @Test()
    public void addOrganizationInvalidAccount(){
        addGithubOrganization.setOrganization(badAccountName);
        addGithubOrganization.setUrl(githubURL);
        when(githubCommunicator.isUsernameCorrect(githubURL, badAccountName)).thenReturn(false);
        when(featureManager.isEnabled(AddGithubOrganization.DISABLE_USERNAME_VALIDATION)).thenReturn(false);

        addGithubOrganization.doValidation();

        assertThat(addGithubOrganization.getErrorMessages(), hasSize(1));
        assertThat(addGithubOrganization.getErrorMessages().iterator().next(), is("Invalid user/team account."));
    }
}
