package com.atlassian.jira.plugins.dvcs.rest.filter;

import com.atlassian.jira.plugins.dvcs.rest.security.AdminOnly;
import com.atlassian.jira.plugins.dvcs.rest.security.AuthorizationException;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugins.rest.common.security.AuthenticationRequiredException;
import com.sun.jersey.api.model.AbstractMethod;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;

import javax.ws.rs.ext.Provider;

import static com.atlassian.jira.permission.GlobalPermissionKey.ADMINISTER;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>This is a Jersey resource filter that, if the resource is marked by {@link AdminOnly} annotation,
 * checks weather the current client is authenticated and it is admin user
 * If the client is not authenticated then an {@link AuthenticationRequiredException} is thrown.
 * If the client is not admin user then an {@link AuthorizationException} is thrown</p>
 */
@Scanned
@Provider
public class AdminOnlyResourceFilter implements ResourceFilter, ContainerRequestFilter
{
    private final AbstractMethod abstractMethod;
    private final JiraAuthenticationContext authenticationContext;
    private final GlobalPermissionManager globalPermissionManager;

    public AdminOnlyResourceFilter(AbstractMethod abstractMethod,
            @ComponentImport GlobalPermissionManager permissionManager,
            @ComponentImport JiraAuthenticationContext authenticationContext)
    {
        this.abstractMethod = checkNotNull(abstractMethod);
        this.authenticationContext = checkNotNull(authenticationContext);
        this.globalPermissionManager = checkNotNull(permissionManager);
    }

    public ContainerRequestFilter getRequestFilter()
    {
        return this;
    }

    public ContainerResponseFilter getResponseFilter()
    {
        return null;
    }

    public ContainerRequest filter(ContainerRequest request)
    {
        if ( isAdminNeeded() )
        {
            ApplicationUser user = authenticationContext.getUser();
            if  (user == null)
            {
                throw new AuthenticationRequiredException();
            }
            if( !isAdmin(user) )
            {
                throw new AuthorizationException();
            }
        }
        return request;
    }

    private boolean isAdminNeeded()
    {
        return (abstractMethod.getMethod() != null && abstractMethod.getMethod().getAnnotation(AdminOnly.class) != null)
                || abstractMethod.getResource().getAnnotation(AdminOnly.class) != null;
    }

    private boolean isAdmin(ApplicationUser user)
    {
        return globalPermissionManager.hasPermission(ADMINISTER, user);
    }
}
