package com.atlassian.jira.plugins.dvcs.pageobjects.page;

import com.atlassian.jira.pageobjects.framework.elements.PageElements;
import com.atlassian.jira.plugins.dvcs.pageobjects.common.OAuth;
import com.atlassian.jira.plugins.dvcs.pageobjects.common.OAuthUtils;
import com.atlassian.jira.plugins.dvcs.pageobjects.util.PageElementUtils;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.webdriver.testing.rule.WebDriverSupport;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import org.openqa.selenium.By;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.atlassian.pageobjects.elements.query.Poller.waitUntilFalse;
import static com.atlassian.pageobjects.elements.timeout.TimeoutType.PAGE_LOAD;
import static org.fest.assertions.api.Assertions.assertThat;

public class BitbucketOAuthPage implements Page
{
    @ElementBy (linkText = "Add consumer")
    private PageElement addConsumerButton;

    @ElementBy (id = "edit-oauth-consumer-form", timeoutType = PAGE_LOAD)
    private PageElement bbAddConsumerDialog;

    @ElementBy (id = "id_name")
    private PageElement consumerNameInput;

    @ElementBy (id = "id_description")
    private PageElement consumerDescriptionInput;

    @ElementBy (tagName = "body")
    private PageElement body;

    @ElementBy (id = "oauth-consumers")
    private PageElement oauthConsumersSection;

    private final String account;

    public BitbucketOAuthPage(final String account)
    {
        this.account = account;
    }

    @Override
    public String getUrl()
    {
        return "https://bitbucket.org/account/user/" + account + "/api";
    }

    public OAuth addConsumer()
    {
        // accessing tag name as workaround for permission denied to access property 'nr@context' issue
        PageElementUtils.permissionDeniedWorkAround(addConsumerButton);

        addConsumerButton.click();

        // clicking the button scrolls to it, scroll back to top so that the dialog is considered visible
        WebDriverSupport.fromAutoInstall().getDriver().executeScript("scroll(0, 0);");

        Poller.waitUntilTrue(bbAddConsumerDialog.timed().isVisible());
        String consumerName = OAuthUtils.generateTestOAuthName();
        String consumerDescription = "Test OAuth Description [" + consumerName + "]";
        consumerNameInput.click().type(consumerName);
        consumerDescriptionInput.type(consumerDescription);
        bbAddConsumerDialog.find(By.className("aui-button-primary")).click();
        waitUntilFalse(bbAddConsumerDialog.timed().isVisible());

        return parseOAuthCredentials(consumerName);
    }

    private OAuth parseOAuthCredentials(final String consumerName)
    {
        final OAuthConsumerRow createdConsumer = findConsumer(consumerName);

        String applicationId = createdConsumer.getId();
        assertThat(applicationId).overridingErrorMessage("newly added oauth consumer app id should not be empty").isNotEmpty();
        String key = createdConsumer.getKey();
        assertThat(key).overridingErrorMessage("newly added oauth key should not be empty").isNotEmpty();
        String secret = createdConsumer.getSecret();
        assertThat(secret).overridingErrorMessage("newly added oauth secret should not be empty").isNotEmpty();

        return new OAuth(key, secret, applicationId);
    }

    private OAuthConsumerRow findConsumer(final String consumerName)
    {
        // Ideally we would select the correct table and only its tr elements but because there are tables in tables we need to
        final List<PageElement> rows = oauthConsumersSection.findAll(By.tagName("tr"));
        Predicate<PageElement> predicate = Predicates.or(PageElements.hasDataAttribute("id"), PageElements.hasClass("extra-info"));
        final Iterable<PageElement> filteredRows = Iterables.filter(rows, predicate);

        Iterator<PageElement> rowsIterator = filteredRows.iterator();

        List<OAuthConsumerRow> consumerRows = new ArrayList<OAuthConsumerRow>();

        while (rowsIterator.hasNext())
        {
            PageElement consumer = rowsIterator.next();
            assertThat(consumer.getAttribute("data-id")).isNotEmpty();
            assertThat(true).isEqualTo(rowsIterator.hasNext());
            PageElement secret = rowsIterator.next();
            assertThat("extra-info").isEqualTo(secret.getAttribute("class"));
            consumerRows.add(new OAuthConsumerRow(consumer, secret));
        }

        final OAuthConsumerRow createdConsumer = Iterables.find(consumerRows, new Predicate<OAuthConsumerRow>()
        {
            @Override
            public boolean apply(final OAuthConsumerRow input)
            {
                return consumerName.equals(input.getName());
            }
        });

        assertThat(createdConsumer).isNotNull();
        return createdConsumer;
    }

    public void removeConsumer(String applicationId)
    {
        final String consumerActionsForThisApplication = "consumer-actions-" + applicationId;

        // Find the '...' and click on it to make the delete visible
        final PageElement consumerRow = body.find(By.id("consumer-" + applicationId));
        final PageElement actionButton = consumerRow.find(By.tagName("button"));
        assertThat(actionButton.getAttribute("aria-owns")).isEqualTo(consumerActionsForThisApplication);
        actionButton.javascript().execute("jQuery(arguments[0]).click()");

        // Trigger the delete
        final PageElement inlineDialog = body.find(By.id(consumerActionsForThisApplication));
        final PageElement deleteButton = inlineDialog.find(By.linkText("Delete"));
        deleteButton.javascript().execute("jQuery(arguments[0]).click()");
    }

    private static class OAuthConsumerRow
    {
        private final PageElement consumer;
        private final PageElement secret;

        public OAuthConsumerRow(final PageElement consumer, final PageElement secret)
        {
            this.consumer = consumer;
            this.secret = secret;
        }

        public String getName()
        {
            return consumer.find(By.cssSelector("div.name")).getText();
        }

        public String getId()
        {
            return consumer.getAttribute("data-id");
        }

        public String getKey()
        {
            ensureSecretIsVisible();
            return secret.find(By.className("oauth-key")).getText();
        }

        public String getSecret()
        {
            ensureSecretIsVisible();
            return secret.find(By.className("oauth-secret")).getText();
        }

        private void ensureSecretIsVisible()
        {
            if (!secret.isVisible())
            {
                consumer.find(By.linkText(getName())).javascript().execute("jQuery(arguments[0]).click()");
            }
        }
    }
}
