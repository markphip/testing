package com.atlassian.jira.plugins.dvcs.rest.filter;

import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.sun.jersey.api.model.AbstractMethod;
import com.sun.jersey.spi.container.ResourceFilter;
import com.sun.jersey.spi.container.ResourceFilterFactory;

import java.util.Collections;
import java.util.List;
import javax.ws.rs.ext.Provider;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>A {@link ResourceFilterFactory} that checks wether the client is authenticated or not.<p>
 * @see AdminOnlyResourceFilter
 */
@Scanned
@Provider
public class AdminOnlyResourceFilterFactory implements ResourceFilterFactory
{
    private final JiraAuthenticationContext authenticationContext;

    private final GlobalPermissionManager globalPermissionManager;

    public AdminOnlyResourceFilterFactory(@ComponentImport final GlobalPermissionManager globalPermissionManager,
            @ComponentImport final JiraAuthenticationContext authenticationContext)
    {
        this.authenticationContext = checkNotNull(authenticationContext);
        this.globalPermissionManager = checkNotNull(globalPermissionManager);
    }

    public List<ResourceFilter> create(AbstractMethod abstractMethod)
    {
        return Collections.<ResourceFilter>singletonList(new AdminOnlyResourceFilter(abstractMethod, globalPermissionManager, authenticationContext));
    }
}
