package com.atlassian.jira.plugins.dvcs.pageobjects.page;

import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.PageElementFinder;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;
import org.openqa.selenium.By;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AbstractComponentPageObject implements PageElementFinder
{
    protected final PageElement container;

    protected AbstractComponentPageObject(PageElement container)
    {
        this.container = checkNotNull(container, "container");
    }

    @Override
    public PageElement find(By by)
    {
        return container.find(by);
    }

    @Override
    public PageElement find(By by, TimeoutType timeoutType)
    {
        return container.find(by, timeoutType);
    }

    @Override
    public List<PageElement> findAll(By by)
    {
        return container.findAll(by);
    }

    @Override
    public List<PageElement> findAll(By by, TimeoutType timeoutType)
    {
        return container.findAll(by, timeoutType);
    }

    @Override
    public <T extends PageElement> T find(By by, Class<T> elementClass)
    {
        return container.find(by, elementClass);
    }

    @Override
    public <T extends PageElement> T find(By by, Class<T> elementClass, TimeoutType timeoutType)
    {
        return container.find(by, elementClass, timeoutType);
    }

    @Override
    public <T extends PageElement> List<T> findAll(By by, Class<T> elementClass)
    {
        return container.findAll(by, elementClass);
    }

    @Override
    public <T extends PageElement> List<T> findAll(By by, Class<T> elementClass, TimeoutType timeoutType)
    {
        return container.findAll(by, elementClass, timeoutType);
    }
}
