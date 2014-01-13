package com.atlassian.jira.plugins.dvcs.spi.bitbucket.ratelimit;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

public class RateLimiterBuilder {
    private long maxConcurrency;
    private long queryLimit;
    private long timeLength;
    private TimeUnit timeUnit;
    private Clock clock;
    private DelayQueue delayQueue;

    public RateLimiterBuilder()
    {
        this.delayQueue = new DelayQueue();
        this.maxConcurrency = 10;
        this.clock = new Clock();
    }

    public RateLimiterBuilder maxConcurrency(long maxConcurrency)
    {
        this.maxConcurrency = maxConcurrency;
        return this;
    }

    public RateLimiterBuilder queryLimit(long queryLimit)
    {
        this.queryLimit = queryLimit;
        return this;
    }

    public RateLimiterBuilder timeLength(long timeLength, TimeUnit timeUnit)
    {
        this.timeLength = timeLength;
        this.timeUnit = timeUnit;
        return this;
    }

    public RateLimiterBuilder withClock(Clock clock)
    {
        this.clock = clock;
        return this;
    }

    public RateLimiterBuilder withDelayQueue(DelayQueue delayQueue)
    {
        this.delayQueue = delayQueue;
        return this;
    }

    public RateLimiter build()
    {
        return new RateLimiter(maxConcurrency, queryLimit, timeLength, timeUnit, clock, delayQueue);
    }
}