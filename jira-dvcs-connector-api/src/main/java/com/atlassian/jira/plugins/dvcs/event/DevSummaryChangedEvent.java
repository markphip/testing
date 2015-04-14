package com.atlassian.jira.plugins.dvcs.event;

import com.google.common.collect.ImmutableSet;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Date;
import java.util.Set;
import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A change was detected for the Issue Keys
 */
public final class DevSummaryChangedEvent implements SyncEvent
{
    private final Date date;
    private final String dvcsType;
    private final Set<String> issueKeys;
    private final int repositoryId;

    public DevSummaryChangedEvent(
            final int repositoryId,
            @Nonnull final String dvcsType,
            @Nonnull final Set<String> issueKeys)
    {
        this(repositoryId, dvcsType, issueKeys, new Date());
    }

    public DevSummaryChangedEvent(
            final int repositoryId,
            @Nonnull final String dvcsType, 
            @Nonnull final Set<String> issueKeys,
            @Nonnull final Date date)
    {
        this.date = checkNotNull(date);
        this.issueKeys = checkNotNull(issueKeys);
        this.repositoryId = repositoryId;
        this.dvcsType = checkNotNull(dvcsType);
    }

    @Nonnull
    public String getDvcsType()
    {
        return dvcsType;
    }

    public int getRepositoryId()
    {
        return repositoryId;
    }

    @Nonnull
    public Set<String> getIssueKeys()
    {
        return ImmutableSet.copyOf(issueKeys);
    }

    @Nonnull
    @Override
    public Date getDate()
    {
        return new Date(date.getTime());
    }

    @JsonCreator
    private static DevSummaryChangedEvent fromJSON(
            @JsonProperty ("repositoryId") int repositoryId,
            @JsonProperty ("dvcsType") String dvcsType, 
            @JsonProperty ("issueKeys") Set<String> issueKeys,
            @JsonProperty ("date") Date date)
    {
        return new DevSummaryChangedEvent(repositoryId, dvcsType, issueKeys, date);
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        final DevSummaryChangedEvent that = (DevSummaryChangedEvent) o;

        if (repositoryId != that.repositoryId) { return false; }
        if (!date.equals(that.date)) { return false; }
        if (!dvcsType.equals(that.dvcsType)) { return false; }
        if (!issueKeys.equals(that.issueKeys)) { return false; }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = date.hashCode();
        result = 31 * result + issueKeys.hashCode();
        result = 31 * result + repositoryId;
        result = 31 * result + dvcsType.hashCode();
        return result;
    }

    @Override
    public String toString()
    {
        return "DevSummaryChangedEvent{" +
                "date=" + date +
                ", issueKeys=" + issueKeys +
                ", repositoryId=" + repositoryId +
                ", dvcsType='" + dvcsType + '\'' +
                '}';
    }
}
