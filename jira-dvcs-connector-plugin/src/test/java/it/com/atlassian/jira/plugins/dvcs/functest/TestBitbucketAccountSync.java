package it.com.atlassian.jira.plugins.dvcs.functest;

import com.atlassian.jira.functest.framework.FuncTestCase;
import com.atlassian.jira.functest.framework.suite.Category;
import com.atlassian.jira.functest.framework.suite.WebTest;
import com.atlassian.jira.plugin.dvcs.testkit.bitbucket.BitbucketDvcsClient;
import com.atlassian.jira.plugin.dvcs.testkit.healtcheck.HealthCheckStatus;

import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeThat;


@WebTest ({ Category.FUNC_TEST, Category.REST })
public class TestBitbucketAccountSync extends FuncTestCase
{
    private static final String BASIC_ENV_DATA = "BitbucketTestkit.zip";

    private BitbucketDvcsClient client;

    @Override
    protected void setUpTest()
    {
        super.setUpTest();
        client = new BitbucketDvcsClient(environmentData);

        assumeThat(client.healthCheck().getOverallStatus(), is(HealthCheckStatus.OK));

        administration.restoreData(BASIC_ENV_DATA);
    }

    @Override
    protected void tearDownTest()
    {
        super.tearDownTest();
    }

    public void testAddAccount(){
        assertEquals(1,0);
    }

}
