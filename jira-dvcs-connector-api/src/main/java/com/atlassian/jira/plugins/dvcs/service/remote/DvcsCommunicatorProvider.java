package com.atlassian.jira.plugins.dvcs.service.remote;

import com.atlassian.jira.plugins.dvcs.model.AccountInfo;

public interface DvcsCommunicatorProvider
{

    public DvcsCommunicator getCommunicator(String dvcsType);

    public DvcsCommunicator getCommunicatorAndCheckSyncDisabled(String dvcsType);

    public AccountInfo getAccountInfo(String hostUrl, String accountName);

    public AccountInfo getAccountInfo(String hostUrl, String accountName, String dvcsType);

}