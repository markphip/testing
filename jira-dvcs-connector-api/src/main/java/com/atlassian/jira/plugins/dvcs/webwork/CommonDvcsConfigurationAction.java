package com.atlassian.jira.plugins.dvcs.webwork;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.dvcs.analytics.AnalyticsPossibleValues.DvcsType;
import com.atlassian.jira.plugins.dvcs.analytics.AnalyticsPossibleValues.FailureReason;
import com.atlassian.jira.plugins.dvcs.analytics.AnalyticsPossibleValues.Outcome;
import com.atlassian.jira.plugins.dvcs.analytics.AnalyticsPossibleValues.Source;
import com.atlassian.jira.plugins.dvcs.analytics.DvcsConfigAddEndedAnalyticsEvent;
import com.atlassian.jira.plugins.dvcs.analytics.DvcsConfigAddStartedAnalyticsEvent;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;

import static com.atlassian.jira.plugins.dvcs.util.CustomStringUtils.encode;
import static com.google.common.base.Preconditions.checkNotNull;

@Scanned
public class CommonDvcsConfigurationAction extends JiraWebActionSupport
{
    public static final String DEFAULT_SOURCE = "unknown";

	private String autoLinking = "";
	private String autoSmartCommits = "";

    private String source;

	private static final long serialVersionUID = 8695500426304238626L;

    private EventPublisher eventPublisher;

	protected static HashMap<String, String> dvcsTypeToUrlMap = new HashMap<String, String>();
	static {
		dvcsTypeToUrlMap.put("bitbucket", "https://bitbucket.org");
		dvcsTypeToUrlMap.put("github", "https://github.com");
	}

    public CommonDvcsConfigurationAction(@ComponentImport EventPublisher eventPublisher)
    {
        this.eventPublisher = checkNotNull(eventPublisher);
    }

	protected boolean hadAutolinkingChecked()
	{
		return StringUtils.isNotBlank(autoLinking);
	}

	protected boolean hadAutoSmartCommitsChecked()
	{
	    return StringUtils.isNotBlank(autoSmartCommits);
	}

	public String getAutoLinking()
	{
		return autoLinking;
	}

	public void setAutoLinking(String autoLinking)
	{
		this.autoLinking = autoLinking;
	}

    public String getAutoSmartCommits()
    {
        return autoSmartCommits;
    }

    public void setAutoSmartCommits(String autoSmartCommits)
    {
        this.autoSmartCommits = autoSmartCommits;
    }

    protected void triggerAddStartedEvent(DvcsType type)
    {
        eventPublisher.publish(new DvcsConfigAddStartedAnalyticsEvent(getSourceOrDefault(), type));
    }

    protected void triggerAddSucceededEvent(DvcsType type)
    {
        triggerAddEndedEvent(type, Outcome.SUCCEEDED, null);
    }

    protected void triggerAddFailedEvent(DvcsType type, FailureReason reason)
    {
        triggerAddEndedEvent(type, Outcome.FAILED, reason);
    }

    protected void triggerAddEndedEvent(DvcsType type, Outcome outcome, FailureReason reason)
    {
        eventPublisher.publish(new DvcsConfigAddEndedAnalyticsEvent(getSourceOrDefault(), type, outcome, reason));
    }

    public String getSource()
    {
        return source;
    }

    public Source getSourceOrDefault()
    {
        if(StringUtils.isNotBlank(source)){
            return Source.valueOf(source.toUpperCase());
        }else{
            return Source.UNKNOWN;
        }
    }

    /**
     * Calculate the url parameter string of the form "&amp;source=xxx" for source parameter.
     *
     * If source is null, empty string "" is returned.
     *
     * @return
     */
    protected String getSourceAsUrlParam()
    {
        return getSourceAsUrlParam("&");
    }

    /**
     * Calculate the url parameter string of the form "&amp;source=xxx" for source parameter.
     *
     * If source is null, empty string "" is returned.
     *
     * @param paramSeparator either "&amp;" or "?" depending on whether there are other url params.
     * @return
     */
    protected String getSourceAsUrlParam(String paramSeparator)
    {
        return StringUtils.isNotEmpty(source) ? (paramSeparator + "source=" + encode(source)) : "";
    }

    public void setSource(String source)
    {
        this.source = source;
    }
}
