package it.restart.com.atlassian.jira.plugins.dvcs.test.pullRequest;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.pageobjects.TestedProduct;
import com.atlassian.pageobjects.TestedProductFactory;
import org.testng.annotations.Test;

/**
 * TODO: Document this class / interface here
 *
 * @since v6.3
 */
public class InstallGreenhopperData
{
    protected static final String TEST_DATA = "test-dvcs.zip";
    @Test
    public void installGreenhopperDataTest(){
        TestedProductFactory.create(JiraTestedProduct.class).backdoor().restoreDataFromResource(TEST_DATA);
    }
}