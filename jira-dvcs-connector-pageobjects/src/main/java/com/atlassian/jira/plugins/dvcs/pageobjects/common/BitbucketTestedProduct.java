package com.atlassian.jira.plugins.dvcs.pageobjects.common;

import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitbucketLoginPage;
import com.atlassian.pageobjects.DefaultProductInstance;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.ProductInstance;
import com.atlassian.pageobjects.TestedProduct;
import com.atlassian.pageobjects.binder.BrowserModule;
import com.atlassian.pageobjects.binder.InjectPageBinder;
import com.atlassian.pageobjects.binder.LoggerModule;
import com.atlassian.pageobjects.binder.StandardModule;
import com.atlassian.pageobjects.elements.ElementModule;
import com.atlassian.pageobjects.elements.timeout.TimeoutsModule;
import com.atlassian.webdriver.AtlassianWebDriverModule;
import com.atlassian.webdriver.pageobjects.WebDriverTester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

public class BitbucketTestedProduct implements TestedProduct<WebDriverTester>
{
    private static final Logger log = LoggerFactory.getLogger(BitbucketTestedProduct.class);

    private final ProductInstance productInstance;
    private final PageBinder pageBinder;
    private final WebDriverTester tester;

    public BitbucketTestedProduct(@Nonnull WebDriverTester webDriverTester) {
        this.productInstance = new DefaultProductInstance("https://bitbucket.org", "bitbucket", 443, "/");
        this.tester = checkNotNull(webDriverTester, "tester");

        this.pageBinder = new InjectPageBinder(productInstance, webDriverTester,
                new AtlassianWebDriverModule(this),
                new StandardModule(this),
                new TimeoutsModule(),
                new ElementModule(),
                new BrowserModule(),
                new LoggerModule(log));
    }

    @Override
    public <P extends Page> P visit(Class<P> pageClass, Object... args)
    {
        return pageBinder.navigateToAndBind(pageClass, args);
    }

    @Override
    public PageBinder getPageBinder()
    {
        return pageBinder;
    }

    @Override
    public ProductInstance getProductInstance()
    {
        return productInstance;
    }

    @Override
    public WebDriverTester getTester()
    {
        return tester;
    }

    @Nonnull
    public BitbucketTestedProduct login(String account, String password)
    {
        visit(BitbucketLoginPage.class).doLogin(account, password);

        return this;
    }

    @Nonnull
    public <P extends Page> P loginAndGoTo(String account, String password, Class<P> targetPage, Object... args)
    {
        login(account, password);
        return visit(targetPage, args);
    }

    @Nonnull
    public BitbucketTestedProduct logout()
    {
        visit(BitbucketLoginPage.class).doLogout();

        return this;
    }
}
