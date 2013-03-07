package it.restart.com.atlassian.jira.plugins.dvcs.page.issue;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.WebDriverLocatable;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;

/**
 * Represents "Comment" activity of {@link IssuePagePullRequestTab#getActivities()}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class IssuePagePullRequestTabActivityComment extends IssuePagePullRequestTabActivity
{

    /**
     * Reference to "Author" link;
     */
    @ElementBy(xpath = "./article/header//a[contains(concat(' ', @class, ' '), 'author')]")
    private PageElement authorNameLink;

    /**
     * Reference to "Pull Request" link.
     */
    @ElementBy(xpath = "./article/header//a[3]")
    private PageElement pullRequestLink;

    /**
     * Reference to span with "Pull Request State" information.
     */
    @ElementBy(xpath = "./article/header//span[contains(concat(' ', @class, ' '), 'pull-request-state')]")
    private PageElement pullRequestStateSpan;

    /**
     * Reference of pull request comment DIV.
     */
    @ElementBy(xpath = "./article//div[contains(concat(' ', @class, ' '), 'comment-content')]")
    private PageElement commentDiv;

    /**
     * Constructor.
     * 
     * @param parent
     * @param timeoutType
     */
    public IssuePagePullRequestTabActivityComment(WebDriverLocatable parent, TimeoutType timeoutType)
    {
        super(parent, timeoutType);
    }

    /**
     * @return Returns URL link of author name.
     */
    public String getAuthorName()
    {
        return authorNameLink.getText();
    }

    /**
     * @return Returns URL link of author profile.
     */
    public String getAuthorUrl()
    {
        return authorNameLink.getAttribute("href");
    }

    /**
     * @return Returns "Pull Request State" information.
     */
    public String getPullRequestState()
    {
        return pullRequestStateSpan.getText();
    }

    /**
     * @return Returns "Pull Request Name".
     */
    public String getPullRequestName()
    {
        return pullRequestLink.getText();
    }

    /**
     * @return Returns "Pull Request URL".
     */
    public String getPullRequestUrl()
    {
        return pullRequestLink.getAttribute("href");
    }

    /**
     * @return Returns "Comment" of pull request.
     */
    public String getComment()
    {
        return commentDiv.getText();
    }

}
