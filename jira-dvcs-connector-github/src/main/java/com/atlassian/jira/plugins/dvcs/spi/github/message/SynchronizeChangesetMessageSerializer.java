package com.atlassian.jira.plugins.dvcs.spi.github.message;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.atlassian.jira.plugins.dvcs.model.DefaultProgress;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.service.message.MessagePayloadSerializer;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;

/**
 * An implementation of {@link MessagePayloadSerializer} over {@link SynchronizeChangesetMessage}.
 *
 * @author Stanislav Dvorscak
 *
 */
public class SynchronizeChangesetMessageSerializer implements MessagePayloadSerializer<SynchronizeChangesetMessage>
{

    /**
     * @see #setRepositoryService(RepositoryService)
     */
    private RepositoryService repositoryService;

    /**
     * @see #setSynchronizer(Synchronizer)
     */
    private Synchronizer synchronizer;

    /**
     * @param repositoryService
     *            injected {@link RepositoryService} dependency
     */
    public void setRepositoryService(RepositoryService repositoryService)
    {
        this.repositoryService = repositoryService;
    }

    /**
     * @param synchronizer
     *            injected {@link Synchronizer} dependency
     */
    public void setSynchronizer(Synchronizer synchronizer)
    {
        this.synchronizer = synchronizer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String serialize(SynchronizeChangesetMessage payload)
    {
        try
        {
            JSONObject result = new JSONObject();
            result.put("branch", payload.getBranch());
            result.put("node", payload.getNode());
            result.put("refreshAfterSynchronizedAt", getDateFormat().format(payload.getRefreshAfterSynchronizedAt()));
            result.put("repository", payload.getRepository().getId());
            result.put("softSync", payload.isSoftSync());
            result.put("syncAuditId", payload.getSyncAuditId());
            return result.toString();

        } catch (JSONException e)
        {
            throw new RuntimeException(e);

        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SynchronizeChangesetMessage deserialize(String payload)
    {
        Repository repository;
        String branch;
        String node;
        Date refreshAfterSynchronizedAt;
        Progress progress;
        boolean softSync;
        int syncAuditId = 0;

        try
        {
            JSONObject result = new JSONObject(payload);

            repository = repositoryService.get(result.getInt("repository"));
            branch = result.getString("branch");
            node = result.getString("node");
            refreshAfterSynchronizedAt = getDateFormat().parse(result.getString("refreshAfterSynchronizedAt"));
            softSync = result.getBoolean("softSync");
            syncAuditId = result.optInt("syncAuditId");

            progress = synchronizer.getProgress(repository.getId());
            if (progress == null || progress.isFinished())
            {
                synchronizer.putProgress(repository, progress = new DefaultProgress());
            }

        } catch (JSONException e)
        {
            throw new RuntimeException(e);

        } catch (ParseException e)
        {
            throw new RuntimeException(e);

        }

        return new SynchronizeChangesetMessage(repository, branch, node, refreshAfterSynchronizedAt, progress, softSync, syncAuditId);
    }

    /**
     * @return date formatter
     */
    private DateFormat getDateFormat()
    {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<SynchronizeChangesetMessage> getPayloadType()
    {
        return SynchronizeChangesetMessage.class;
    }

}
