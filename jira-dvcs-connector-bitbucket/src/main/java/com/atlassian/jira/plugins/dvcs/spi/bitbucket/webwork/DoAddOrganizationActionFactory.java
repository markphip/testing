package com.atlassian.jira.plugins.dvcs.spi.bitbucket.webwork;

import org.springframework.stereotype.Component;

@Component
public class DoAddOrganizationActionFactory
{
    public DoAddOrganizationAction createDoAddOrganizationAction(AddBitbucketOrganization  addBitbucketOrganization){
        return new DoAddOrganizationAction(addBitbucketOrganization);
    }
}
