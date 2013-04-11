package com.atlassian.jira.plugins.dvcs.spi.githubenterprise.webwork;

import static com.atlassian.jira.plugins.dvcs.spi.githubenterprise.GithubEnterpriseCommunicator.GITHUB_ENTERPRISE;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.auth.OAuthStore;
import com.atlassian.jira.plugins.dvcs.auth.OAuthStore.Host;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException.InvalidResponseException;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.spi.github.webwork.GithubOAuthUtils;
import com.atlassian.jira.plugins.dvcs.spi.githubenterprise.GithubEnterpriseCommunicator;
import com.atlassian.jira.plugins.dvcs.util.CustomStringUtils;
import com.atlassian.jira.plugins.dvcs.util.SystemUtils;
import com.atlassian.jira.plugins.dvcs.webwork.CommonDvcsConfigurationAction;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.sal.api.ApplicationProperties;

public class AddGithubEnterpriseOrganization extends CommonDvcsConfigurationAction
{
    private static final long serialVersionUID = 7672281234704330946L;

    private final Logger log = LoggerFactory.getLogger(AddGithubEnterpriseOrganization.class);

	private String organization;

	private String oauthClientIdGhe;
	private String oauthSecretGhe;
	private String oauthRequiredGhe;

	// sent by GH on the way back
	private String code;
	private String url;

	private String accessToken = "";

	private final OrganizationService organizationService;
	private final GithubOAuthUtils githubOAuthUtils;

    private final OAuthStore oAuthStore;

    public AddGithubEnterpriseOrganization(OrganizationService organizationService,
            OAuthStore oAuthStore, ApplicationProperties applicationProperties)
	{
		this.organizationService = organizationService;
        this.oAuthStore = oAuthStore;
        this.githubOAuthUtils = new GithubOAuthUtils(applicationProperties.getBaseUrl(), oAuthStore.getClientId(GITHUB_ENTERPRISE), oAuthStore.getSecret(GITHUB_ENTERPRISE));
	}

	@Override
	@RequiresXsrfCheck
	protected String doExecute() throws Exception
	{
        if (isOAuthConfigurationRequired())
        {
            configureOAuth();
        }
		
		// then continue
		return redirectUserToGithub();

	}

	private void configureOAuth()
	{
        oAuthStore.store(new Host(GITHUB_ENTERPRISE, url), oauthClientIdGhe, oauthSecretGhe);
        githubOAuthUtils.setClientId(oauthClientIdGhe);
        githubOAuthUtils.setSecret(oauthSecretGhe);
	}

	private String redirectUserToGithub()
	{
		String githubAuthorizeUrl = githubOAuthUtils.createGithubRedirectUrl("AddGithubEnterpriseOrganization",
		        url, getXsrfToken(), organization, getAutoLinking(), getAutoSmartCommits());

		return SystemUtils.getRedirect(this, githubAuthorizeUrl, true);
	}

	@Override
	protected void doValidation()
	{
	    
        if (StringUtils.isNotBlank(oauthRequiredGhe))
        {
            if (StringUtils.isBlank(oauthClientIdGhe) || StringUtils.isBlank(oauthSecretGhe))
            {
                addErrorMessage("Missing credentials.");
            }
        } else
        {
            // load saved GitHub Enterprise url
            url = oAuthStore.getUrl(GITHUB_ENTERPRISE);
        }
        
        if (StringUtils.isBlank(url) || StringUtils.isBlank(organization))
        {
            addErrorMessage("Please provide both url and organization parameters.");
        }
        
        if (!SystemUtils.isValid(url))
        {
            addErrorMessage("Please provide valid GitHub host URL.");
        }
        
        if (url.endsWith("/"))
        {
            url = StringUtils.chop(url);
            
        }
	    
//TODO validation of account is disabled because of private mode 
//        AccountInfo accountInfo = organizationService.getAccountInfo(url, organization);
//        if (accountInfo == null)
//        {
//            addErrorMessage("Invalid user/team account.");
//        }
		
	}
	
    protected boolean isOAuthConfigurationRequired()
    {
        return StringUtils.isNotBlank(oauthRequiredGhe);
    }

	public String doFinish()
	{
		try
		{
			accessToken = requestAccessToken();

		} catch (InvalidResponseException ire)
		{
		    addErrorMessage(ire.getMessage() + " Possibly bug in releases of GitHub Enterprise prior to 11.10.290.");
		    return INPUT;
		
		} catch (SourceControlException sce)
		{
			addErrorMessage(sce.getMessage());
			log.warn(sce.getMessage());
			if ( sce.getCause() != null )
			{
				log.warn("Caused by: " + sce.getCause().getMessage());
			}
			return INPUT;

		} catch (Exception e) {
		    addErrorMessage("Error obtaining access token.");
            return INPUT;
        }

		return doAddOrganization();
	}

    private String doAddOrganization()
	{
		try
		{
			Organization newOrganization = new Organization();
			newOrganization.setName(organization);
			newOrganization.setHostUrl(url);
			newOrganization.setDvcsType(GithubEnterpriseCommunicator.GITHUB_ENTERPRISE);
			newOrganization.setAutolinkNewRepos(hadAutolinkingChecked());
			newOrganization.setCredential(new Credential(null, null, accessToken));
			newOrganization.setSmartcommitsOnNewRepos(hadAutolinkingChecked());
			
			organizationService.save(newOrganization);
			
		} catch (SourceControlException e)
		{		
			addErrorMessage("Failed adding the account: [" + e.getMessage() + "]");
			log.debug("Failed adding the account: [" + e.getMessage() + "]");
			e.printStackTrace();
			return INPUT;
		}

        return getRedirect("ConfigureDvcsOrganizations.jspa?atl_token=" + CustomStringUtils.encode(getXsrfToken()));
	}

	private String requestAccessToken()
	{
		return githubOAuthUtils.requestAccessToken(url, code);
	}

	public static String encode(String url)
	{
		return CustomStringUtils.encode(url);
	}

	public String getCode()
	{
		return code;
	}

	public void setCode(String code)
	{
		this.code = code;
	}

	public String getOrganization()
	{
		return organization;
	}

	public void setOrganization(String organization)
	{
		this.organization = organization;
	}

    public String getOauthClientIdGhe()
    {
        return oauthClientIdGhe;
    }

    public void setOauthClientIdGhe(String oauthClientIdGhe)
    {
        this.oauthClientIdGhe = oauthClientIdGhe;
    }

    public String getOauthSecretGhe()
    {
        return oauthSecretGhe;
    }

    public void setOauthSecretGhe(String oauthSecretGhe)
    {
        this.oauthSecretGhe = oauthSecretGhe;
    }

    public String getOauthRequiredGhe()
    {
        return oauthRequiredGhe;
    }

    public void setOauthRequiredGhe(String oauthRequiredGhe)
    {
        this.oauthRequiredGhe = oauthRequiredGhe;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }


}