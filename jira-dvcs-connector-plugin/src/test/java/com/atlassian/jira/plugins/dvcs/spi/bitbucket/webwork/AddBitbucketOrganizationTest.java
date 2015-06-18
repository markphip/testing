package com.atlassian.jira.plugins.dvcs.spi.bitbucket.webwork;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.junit.rules.AvailableInContainer;
import com.atlassian.jira.plugins.dvcs.analytics.event.DvcsType;
import com.atlassian.jira.plugins.dvcs.analytics.event.Outcome;
import com.atlassian.jira.plugins.dvcs.analytics.event.FailureReason;
import com.atlassian.jira.plugins.dvcs.analytics.event.Source;
import com.atlassian.jira.plugins.dvcs.analytics.event.DvcsConfigAddEndedAnalyticsEvent;
import com.atlassian.jira.plugins.dvcs.analytics.event.DvcsConfigAddStartedAnalyticsEvent;
import com.atlassian.jira.plugins.dvcs.auth.OAuthStore;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.HttpClientProvider;
import com.atlassian.jira.plugins.dvcs.util.TestNGMockComponentContainer;
import com.atlassian.jira.plugins.dvcs.util.TestNGMockHttp;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.xsrf.XsrfTokenGenerator;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.web.action.RedirectSanitiser;
import com.atlassian.sal.api.ApplicationProperties;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.scribe.exceptions.OAuthException;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import webwork.action.Action;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class AddBitbucketOrganizationTest
{
    private static final String SAMPLE_SOURCE = "devtools";
    private static final String SAMPLE_XSRF_TOKEN = "xsrfToken";
    private static final String SAMPLE_AUTH_URL = "http://authurl.com";

    private final TestNGMockComponentContainer mockComponentContainer = new TestNGMockComponentContainer(this);

    @Mock
    @AvailableInContainer
    private XsrfTokenGenerator xsrfTokenGenerator;

    @Mock
    @AvailableInContainer
    private JiraAuthenticationContext jiraAuthenticationContext;

    @Mock
    private I18nHelper i18nHelper;

    @Mock
    @AvailableInContainer
    private RedirectSanitiser redirectSanitiser;

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
    private OAuthService oAuthService;

    @Mock
    private HttpClientProvider httpClientProvider;

    @Mock
    private AccountInfo accountInfo;

    private AddBitbucketOrganization addBitbucketOrganization;
    private static final String testingURL = "localhost:8890";
    
    @BeforeMethod (alwaysRun = true)
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

        when(oAuthStore.getClientId(anyString())).thenReturn("apiKey");
        when(oAuthStore.getSecret(anyString())).thenReturn("apiSecret");

        when(request.getParameter("oauth_verifier")).thenReturn("verifier");
        HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        Token requestToken = mock(Token.class);
        when(session.getAttribute(AddBitbucketOrganization.SESSION_KEY_REQUEST_TOKEN)).thenReturn(requestToken);
//        when(requestToken.getToken()).thenReturn("requestToken");
//        when(requestToken.getSecret()).thenReturn("secret");

        final Token accessToken = mock(Token.class);
        when(accessToken.getToken()).thenReturn("accessToken");
        when(accessToken.getSecret()).thenReturn("accessSecret");
        when(oAuthService.getAccessToken(eq(requestToken), any(Verifier.class))).thenReturn(accessToken);
        when(oAuthService.getRequestToken()).thenReturn(requestToken);
        when(oAuthService.getAuthorizationUrl(eq(requestToken))).thenReturn(SAMPLE_AUTH_URL);

        addBitbucketOrganization = new AddBitbucketOrganization(ap, eventPublisher, oAuthStore, organizationService, httpClientProvider)
        {
            @Override
            OAuthService createOAuthScribeService()
            {
                return oAuthService;
            }
        };
        addBitbucketOrganization.setUrl("http://url.com");
    }

    @AfterMethod
    public void tearDown()
    {
        ComponentAccessor.initialiseWorker(null); // reset
        mockComponentContainer.afterMethod();
        mockHttp.afterMethod();
        System.clearProperty(BitbucketRemoteClient.BITBUCKET_TEST_URL_CONFIGURATION);
    }

    @Test
    public void testDoExecuteAnalytics() throws Exception
    {
        addBitbucketOrganization.setSource(SAMPLE_SOURCE);
        String ret = addBitbucketOrganization.doExecute();
        assertThat(ret, equalTo(Action.NONE));
        verify(eventPublisher).publish(new DvcsConfigAddStartedAnalyticsEvent(Source.DEVTOOLS, DvcsType.BITBUCKET));
        verifyNoMoreInteractions(eventPublisher);
        verify(response).sendRedirect(eq(SAMPLE_AUTH_URL));
        verifyNoMoreInteractions(response);
    }

    @Test
    public void testDoExecuteAnalyticsDefaultSource() throws Exception
    {
        addBitbucketOrganization.setSource(null);
        String ret = addBitbucketOrganization.doExecute();
        assertThat(ret, equalTo(Action.NONE));
        verify(eventPublisher).publish(new DvcsConfigAddStartedAnalyticsEvent(Source.UNKNOWN, DvcsType.BITBUCKET));
        verifyNoMoreInteractions(eventPublisher);
        verify(response).sendRedirect(eq(SAMPLE_AUTH_URL));
        verifyNoMoreInteractions(response);
    }

    @Test
    public void testDoExecuteAnalyticsError() throws Exception
    {
        addBitbucketOrganization.setSource(SAMPLE_SOURCE);
        reset(oAuthService);
        when(oAuthService.getRequestToken()).thenThrow(OAuthException.class);
        String ret = addBitbucketOrganization.doExecute();
        assertThat(ret, equalTo(Action.INPUT));
        verify(eventPublisher).publish(new DvcsConfigAddStartedAnalyticsEvent(Source.DEVTOOLS, DvcsType.BITBUCKET));
        verify(eventPublisher).publish(new DvcsConfigAddEndedAnalyticsEvent(Source.DEVTOOLS, DvcsType.BITBUCKET, Outcome.FAILED, FailureReason.OAUTH_TOKEN));
        verifyNoMoreInteractions(eventPublisher);
        verifyNoMoreInteractions(response);
    }

    @Test
    public void testDoFinishAnalytics() throws Exception
    {
        addBitbucketOrganization.setSource(SAMPLE_SOURCE);
        String ret = addBitbucketOrganization.doFinish();
        assertThat(ret, equalTo(Action.NONE));
        verify(eventPublisher).publish(new DvcsConfigAddEndedAnalyticsEvent(Source.DEVTOOLS, DvcsType.BITBUCKET, Outcome.SUCCEEDED, null));
        verifyNoMoreInteractions(eventPublisher);
        verify(response).sendRedirect(eq("ConfigureDvcsOrganizations.jspa?atl_token=" + SAMPLE_XSRF_TOKEN + "&source=" + SAMPLE_SOURCE));
        verifyNoMoreInteractions(response);
    }

    @Test
    public void testDoFinishAnalyticsDefaultSource() throws Exception
    {
        addBitbucketOrganization.setSource(null);
        String ret = addBitbucketOrganization.doFinish();
        assertThat(ret, equalTo(Action.NONE));
        verify(eventPublisher).publish(new DvcsConfigAddEndedAnalyticsEvent(Source.UNKNOWN, DvcsType.BITBUCKET, Outcome.SUCCEEDED, null));
        verifyNoMoreInteractions(eventPublisher);
        verify(response).sendRedirect(eq("ConfigureDvcsOrganizations.jspa?atl_token=" + SAMPLE_XSRF_TOKEN)); // source parameter skipped
        verifyNoMoreInteractions(response);
    }

    @Test
    public void testDoFinishAnalyticsErrorUnauth() throws Exception
    {
        addBitbucketOrganization.setSource(SAMPLE_SOURCE);
        when(organizationService.save(any(Organization.class))).thenThrow(SourceControlException.UnauthorisedException.class);
        String ret = addBitbucketOrganization.doFinish();
        assertThat(ret, equalTo(Action.INPUT));
        verify(eventPublisher).publish(new DvcsConfigAddEndedAnalyticsEvent(Source.DEVTOOLS, DvcsType.BITBUCKET, Outcome.FAILED, FailureReason.OAUTH_UNAUTH));
        verifyNoMoreInteractions(eventPublisher);
        verifyNoMoreInteractions(response);
    }

    @Test
    public void testDoFinishAnalyticsErrorSourceControl() throws Exception
    {
        addBitbucketOrganization.setSource(SAMPLE_SOURCE);
        reset(organizationService);
        when(organizationService.save(any(Organization.class))).thenThrow(SourceControlException.class);
        String ret = addBitbucketOrganization.doFinish();
        assertThat(ret, equalTo(Action.INPUT));
        verify(eventPublisher).publish(new DvcsConfigAddEndedAnalyticsEvent(Source.DEVTOOLS, DvcsType.BITBUCKET, Outcome.FAILED, FailureReason.OAUTH_SOURCECONTROL));
        verifyNoMoreInteractions(eventPublisher);
        verifyNoMoreInteractions(response);
    }

    @Test
    public void testDoValidationAnalyticsError() throws Exception
    {
        addBitbucketOrganization.setSource(SAMPLE_SOURCE);
        addBitbucketOrganization.setOrganization(null); // cause validation error
        addBitbucketOrganization.doValidation();
        verify(eventPublisher).publish(new DvcsConfigAddEndedAnalyticsEvent(Source.DEVTOOLS, DvcsType.BITBUCKET, Outcome.FAILED, FailureReason.VALIDATION));
        verifyNoMoreInteractions(eventPublisher);
    }

    @Test
    public void testDoValidationAnalyticsNoError() throws Exception
    {
        addBitbucketOrganization.setSource(SAMPLE_SOURCE);
        addBitbucketOrganization.setOrganization("org");
        final AccountInfo accountInfo = mock(AccountInfo.class);
        when(organizationService.getAccountInfo(anyString(), anyString(), Mockito.eq(BitbucketCommunicator.BITBUCKET))).thenReturn(accountInfo);
        addBitbucketOrganization.doValidation();
//        verify(eventPublisher).publish(new DvcsConfigAddEndedAnalyticsEvent(Source.DEVTOOLS, EVENT_TYPE_BITBUCKET, OUTCOME_FAILED, VALIDATION));
        verifyNoMoreInteractions(eventPublisher);
    }

    @Test
    public void testExpectedAnalyticsWhenCtkState() throws Exception
    {
        setupForCtkStateTest();
        String response = addBitbucketOrganization.doExecute();
        assertThat(response, equalTo(Action.NONE));

        verify(eventPublisher).publish(new DvcsConfigAddStartedAnalyticsEvent(Source.UNKNOWN, DvcsType.BITBUCKET));
        verify(eventPublisher).publish(new DvcsConfigAddEndedAnalyticsEvent(Source.UNKNOWN, DvcsType.BITBUCKET, Outcome.FAILED, null));
        verifyNoMoreInteractions(eventPublisher);
    }

    @Test
    public void testValidationWhenCtkState() throws Exception
    {
        setupForCtkStateTest();
        addBitbucketOrganization.doValidation();
        assertThat(addBitbucketOrganization.getUrl(), equalTo(testingURL));
    }

    private void setupForCtkStateTest()
    {
        String url = System.setProperty(BitbucketRemoteClient.BITBUCKET_TEST_URL_CONFIGURATION, testingURL);
        addBitbucketOrganization = new AddBitbucketOrganization(ap, eventPublisher, oAuthStore, organizationService, httpClientProvider)
        {
            @Override
            OAuthService createOAuthScribeService()
            {
                return oAuthService;
            }
        };
        addBitbucketOrganization.setOrganization("org");
        when(organizationService.getAccountInfo(testingURL, "org", BitbucketCommunicator.BITBUCKET)).thenReturn(accountInfo);
    }

}
