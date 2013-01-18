package com.atlassian.jira.plugins.dvcs.spi.bitbucket.webwork;

import org.scribe.builder.api.DefaultApi10a;
import org.scribe.model.OAuthConfig;
import org.scribe.model.Token;
import org.scribe.oauth.OAuthService;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.scribe.HttpClientThrreeLoOAuth10aServiceImpl;

public class Bitbucket10aScribeApi extends DefaultApi10a
{
	private final String hostUrl;

	public Bitbucket10aScribeApi(String hostUrl)
	{
		super();
		this.hostUrl = hostUrl;
	}

	@Override
	public String getRequestTokenEndpoint()
	{
		return hostUrl() + "api/1.0/oauth/request_token";
	}

	@Override
	public String getAccessTokenEndpoint()
	{
		return hostUrl() + "api/1.0/oauth/access_token";
	}

	@Override
	public String getAuthorizationUrl(Token token)
	{
		return String.format(hostUrl() + "api/1.0/oauth/authenticate?oauth_token=%s", token.getToken());
	}

	private String hostUrl()
	{
		return hostUrl + normalize();
	}

	private String normalize()
	{
		if (!hostUrl.endsWith("/"))
		{
			return "/";
		}
		return "";
	}

    @Override
    public OAuthService createService(OAuthConfig config)
    {
        return new HttpClientThrreeLoOAuth10aServiceImpl(this, config);
    }
}