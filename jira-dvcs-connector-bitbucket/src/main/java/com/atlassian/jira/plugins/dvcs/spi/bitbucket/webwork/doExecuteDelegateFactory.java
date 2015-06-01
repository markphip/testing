package com.atlassian.jira.plugins.dvcs.spi.bitbucket.webwork;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class DoExecuteDelegateFactory
{
    public DoExecuteAction createDoExecuteAction(AddBitbucketOrganization addBitbucketOrganization){

        if (StringUtils.isBlank(System.getProperty(BitbucketRemoteClient.BITBUCKET_TEST_URL_CONFIGURATION)))
        {
            return new RedirectUserToBitbucketDelegate(addBitbucketOrganization);
        }
        else
        {
            return new DoAddOrganizationAction(addBitbucketOrganization);
        }
    }
}
