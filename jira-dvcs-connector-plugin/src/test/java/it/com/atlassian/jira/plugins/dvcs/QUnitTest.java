package it.com.atlassian.jira.plugins.dvcs;

import com.atlassian.jira.pageobjects.BaseJiraWebTest;
import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.qunit.test.runner.QUnitPageObjectsHelper;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import java.io.File;

public class QUnitTest extends BaseJiraWebTest
{
    private final File outputDirectory;
    private final JiraTestedProduct product = TestedProductFactory.create(JiraTestedProduct.class);

    public QUnitTest()
    {
        String location = System.getProperty("jira.qunit.testoutput.location");

        if (StringUtils.isEmpty(location)) {
            System.err.println("jira.qunit.testoutput.location is not defined, writing TestQUnit output to tmp.");
            location = System.getProperty("java.io.tmpdir");
        }

        outputDirectory = new File(location);
    }

    @Test
    public void runJustOurTest() throws Exception
    {
        QUnitPageObjectsHelper helper = new QUnitPageObjectsHelper(outputDirectory, product.getPageBinder(), "/qunit");
        helper.runTests(QUnitPageObjectsHelper.testFilePathContains("com.atlassian.jira.plugins.jira-bitbucket-connector-plugin"));
    }
}