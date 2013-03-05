package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;
import java.util.List;

/**
 * BitbucketPullRequest
 *
 * 
 * <br /><br />
 * Created on 11.12.2012, 14:02:57
 * <br /><br />
 * @author jhocman@atlassian.com
 *
 */
public class BitbucketPullRequestCommentActivity extends BitbucketPullRequestBaseActivity implements Serializable, HasPossibleUpdatedMessages
{
    private static final long serialVersionUID = 8212352604704981087L;

    private Integer id;
    private BitbucketPullRequestCommentActivityContent content;
    private BitbucketPullRequestCommentActivity parent;
    private BitbucketPullRequestCommentActivityInline inline;
    
    public BitbucketPullRequestCommentActivity()
    {
        super();
    }

    public BitbucketPullRequestCommentActivityContent getContent()
    {
        return content;
    }

    public void setContent(BitbucketPullRequestCommentActivityContent content)
    {
        this.content = content;
    }
    
    @Override
    public List<String> getMessages()
    {
        List<String> messages = super.getMessages();
        if (content != null)
        {
            messages.add(content.getRaw());
        }
        return messages;
    }

	public BitbucketPullRequestCommentActivity getParent() {
		return parent;
	}

	public void setParent(BitbucketPullRequestCommentActivity parent) {
		this.parent = parent;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public BitbucketPullRequestCommentActivityInline getInline() {
		return inline;
	}

	public void setInline(BitbucketPullRequestCommentActivityInline inline) {
		this.inline = inline;
	}
}
