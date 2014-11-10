package it.restart.com.atlassian.jira.plugins.dvcs.github;

import com.atlassian.jira.plugins.dvcs.pageobjects.common.OAuth;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.PageElementFinder;
import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.By;

import java.util.List;
import javax.inject.Inject;

/**
 *
 */
public class GithubOAuthApplicationPage implements Page
{
    @Inject
    private PageElementFinder pageElementFinder;

    @Inject
    private PageBinder pageBinder;

    private final String hostUrl;

    public GithubOAuthApplicationPage()
    {
        this("https://github.com");
    }

    public GithubOAuthApplicationPage(String hostUrl)
    {
        this.hostUrl = hostUrl;
    }

    @Override
    public String getUrl()
    {
        return hostUrl + "/settings/applications";
    }

    public void removeConsumer(OAuth oAuth)
    {
        String href = StringUtils.removeStart(oAuth.applicationId, hostUrl);

        pageElementFinder.find(By.xpath("//a[@href='" + href + "']")).click();

        pageBinder.bind(GithubOAuthPage.class).removeConsumer();
    }

    /**
     * Removes ALL the OAuth applications which start with Test_OAuth_ this may interfere with other running tests
     * so only use this as a manual cleanup, not during a regular test cleanup.
     *
     * Note that this is a bit fragile as well, we do explicitly rebind and search for new OAuth applications, we
     * rely on the list of links we found during the first search to continue working even though the page is
     * redirecting between the OAuth applications page and the page for each application.
     */
    public void removeAllTestApplications()
    {
        List<PageElement> testOauthLinks = pageElementFinder.findAll(By.partialLinkText("Test_OAuth_"));

        for (PageElement testOauthLink : testOauthLinks)
        {
            testOauthLink.click();
            pageBinder.bind(GithubOAuthPage.class).removeConsumer();
        }
    }

}
