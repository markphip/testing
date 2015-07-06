package com.atlassian.jira.plugin.dvcs.testkit.healtcheck;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

/**
 * Holds health check information
 *
 * Provides an overall status, as well as support for per-component status if needed.
 *
 * Supports JSON serialization/de-serialization via Jackson.
 */
@JsonSerialize
@JsonDeserialize
public class HealthCheckBean
{
    public static HealthCheckBean OK()
    {
        return new HealthCheckBean(HealthCheckStatus.OK);
    }

    public static HealthCheckBean FAIL(String... messages)
    {
        return new HealthCheckBean(HealthCheckStatus.FAILED, messages);
    }

    @JsonProperty
    private HealthCheckStatus overallStatus;

    @JsonProperty
    private Map<String, HealthCheckStatus> components = new HashMap<>();

    @JsonProperty
    private List<String> messages = new ArrayList<>();

    private HealthCheckBean()
    {

    }

    private HealthCheckBean(HealthCheckStatus overall)
    {
        this.overallStatus = overall;
    }

    private HealthCheckBean(HealthCheckStatus overall, String... messages)
    {
        this.overallStatus = overall;
        if (messages != null)
        {
            this.messages.addAll(asList(messages));
        }
    }

    public HealthCheckStatus getOverallStatus()
    {
        return overallStatus;
    }

    public void setComponentStatus(String key, HealthCheckStatus status)
    {
        components.put(key, status);
    }

    public HealthCheckStatus getStatus(String key)
    {
        return components.get(key);
    }

    public void addMessage(String message)
    {
        if (message == null)
        {
            return;
        }
        this.messages.add(message);
    }

}
