package com.atlassian.jira.plugins.dvcs.adduser;

import com.atlassian.jira.plugins.dvcs.listener.PluginFeatureDetector;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.web.model.AbstractWebPanel;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.templaterenderer.TemplateRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.Map;

/**
 * This panel extends the "add user" form in JIRA. It appends configured bitbucket
 * accounts with their groups so administrator can choose to which groups the
 * user being added should be invited.
 * 
 * An error rendering this panel will have no effect on the final "add user" form.
 * 
 * @author jhocman@atlassian.com
 */
public class AddUserDvcsExtensionWebPanel extends AbstractWebPanel
{
    private static final Logger log = LoggerFactory.getLogger(AddUserDvcsExtensionWebPanel.class);

    private final ApplicationProperties appProperties;
    private final PluginFeatureDetector featuresDetector;
    private final OrganizationService organizationService;
    private final TemplateRenderer templateRenderer;

    public AddUserDvcsExtensionWebPanel(PluginAccessor pluginAccessor, TemplateRenderer templateRenderer,
            ApplicationProperties appProperties, PluginFeatureDetector featuresDetector, OrganizationService organizationService)
    {
        super(pluginAccessor);
        this.templateRenderer = templateRenderer;
        this.appProperties = appProperties;
        this.featuresDetector = featuresDetector;
        this.organizationService = organizationService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHtml(Map<String, Object> model)
    {

        if (!featuresDetector.isUserInvitationsEnabled() || organizationService.getAllCount() == 0) 
        {
            return "";
        }

        StringWriter stringWriter = new StringWriter();
        try
        {
        	 model.put("baseurl", appProperties.getBaseUrl());
        	 
            templateRenderer.render("/templates/dvcs/add-user-dvcs-extension.vm", model, stringWriter);
        } catch (Exception e)
        {
            log.warn("Error while rendering DVCS extension fragment for add user form.", e);
            stringWriter = new StringWriter(); // reset writer so no broken
                                               // output goes out TODO should we print the error message to the writer?
        }

        return stringWriter.toString();
    }
}
