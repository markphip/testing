package com.atlassian.jira.plugins.dvcs.spi.bitbucket.ratelimit;

import org.joda.time.DateTime;

/**
 * Our own Clock, as I had issues with injecting the one from atlassian-core.
 */
public class Clock {
    public DateTime getCurrentDate() {
        return new DateTime();
    }
}
