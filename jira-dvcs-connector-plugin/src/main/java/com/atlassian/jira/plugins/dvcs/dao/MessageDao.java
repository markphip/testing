package com.atlassian.jira.plugins.dvcs.dao;

import java.util.Map;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.MessageMapping;
import com.atlassian.jira.plugins.dvcs.model.Message;

/**
 * DAO layer for {@link MessageMapping}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public interface MessageDao
{

    /**
     * Creates new {@link MessageMapping} for provided parameters.
     * 
     * @param message
     * @param tags
     * @return created {@link MessageMapping} entity
     */
    MessageMapping create(Map<String, Object> message, String[] tags);

    /**
     * Deletes provided message.
     * 
     * @param message
     *            to delete
     */
    void delete(MessageMapping message);

    /**
     * @param id
     *            {@link Message#getId()}
     * @return resolved message
     */
    MessageMapping getById(int id);

    /**
     * Finds all messages for provided tag.
     * 
     * @param tag
     *            for which tag
     * @param messagesStream
     *            callback for stream processing
     */
    void getByTag(String tag, StreamCallback<MessageMapping> messagesStream);

    /**
     * @param key
     *            {@link Message#getAddress()}
     * @param tag
     *            {@link Message#getTags()}
     * @return count of messages which are waiting for processing
     */
    int getMessagesForConsumingCount(String key, String tag);

}
