package com.atlassian.jira.plugins.dvcs.activity;

import net.java.ao.Preload;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

import java.util.Date;

@Preload
@Table("COMMIT")
public interface RepositoryCommitMapping extends RepositoryDomainMapping
{
    String RAW_AUTHOR = "RAW_AUTHOR";
    String AUTHOR = "AUTHOR";
    String NODE = "NODE";
    String MESSAGE = "MESSAGE";
    String DATE = "DATE";
    String AUTHOR_AVATAR_URL = "AUTHOR_AVATAR_URL";

    /**
     * @return Author's full name of the commit, useful if the {@link #getAuthor()} username is not available.
     */
    String getRawAuthor();

    /**
     * @return Author's username of the commit.
     */
    String getAuthor();

    /**
     * @return Message of this commit.
     */
    @StringLength(StringLength.UNLIMITED)
    String getMessage();

    /**
     * @return SHA/commit ID/hash ID of the commit.
     */
    String getNode();

    /**
     * @return Date of the commit
     */
    @NotNull
    Date getDate();

    /**
     * @return Author's avatar URL, useful if the {@link #getAuthor()} username is not available. Can be null, then internal resolver will
     *         be used, otherwise it has precedence.
     */
    String getAuthorAvatarUrl();

    void setRawAuthor(String rawAuthor);

    void setAuthor(String author);

    @StringLength(StringLength.UNLIMITED)
    void setMessage(String message);

    void setNode(String node);

    void setDate(Date date);

    void setAuthorAvatarUrl(String authorAvatarUrl);

}
