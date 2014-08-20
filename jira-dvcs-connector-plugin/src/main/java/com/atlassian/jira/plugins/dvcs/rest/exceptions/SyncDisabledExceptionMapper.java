package com.atlassian.jira.plugins.dvcs.rest.exceptions;

import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.rest.security.AuthorizationException;
import com.atlassian.plugins.rest.common.Status;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Mapper for {@link SourceControlException.SynchronizationDisabled}
 */
@Provider
public class SyncDisabledExceptionMapper implements ExceptionMapper<SourceControlException.SynchronizationDisabled>
{
    @Override
    public Response toResponse(final SourceControlException.SynchronizationDisabled exception)
    {

        return buildErrorResponse(Response.Status.SERVICE_UNAVAILABLE, exception.getMessage());
    }

    private Response buildErrorResponse(Response.Status status, String message)
    {
        CacheControl cacheControl = new CacheControl();
        cacheControl.setNoCache(true);
        cacheControl.setNoStore(true);

        Response.ResponseBuilder responseBuilder = Response.status(status).entity(new com.atlassian.jira.plugins.dvcs.rest.common.Status(status, message)).cacheControl(cacheControl);

        return responseBuilder.build();
    }
}
