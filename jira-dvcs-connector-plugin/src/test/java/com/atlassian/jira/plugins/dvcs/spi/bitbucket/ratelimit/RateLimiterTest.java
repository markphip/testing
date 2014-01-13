package com.atlassian.jira.plugins.dvcs.spi.bitbucket.ratelimit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class RateLimiterTest {
    @Mock
    Clock clock;

    @Test
    public void testBasicRateLimiting()
    {
        RateLimiter rateLimiter = new RateLimiterBuilder().maxConcurrency(1)
                .queryLimit(1)
                .timeLength(10, TimeUnit.MILLISECONDS)
                .withClock(clock)
                .build();

        long startTime = System.currentTimeMillis();
        rateLimiter.aquire();
        rateLimiter.aquire();
        assertThat("Rate limiting should prevent two within 10 milliseconds", System.currentTimeMillis() - startTime, is(greaterThan(10L)));
    }

    @Test
    public void testMoreAdvancedRateLimiting()
    {
        RateLimiter rateLimiter = new RateLimiterBuilder().maxConcurrency(1)
                .queryLimit(3)
                .timeLength(100, TimeUnit.MILLISECONDS)
                .withClock(clock)
                .build();

        long startTime = System.currentTimeMillis();
        rateLimiter.aquire();
        rateLimiter.aquire();
        rateLimiter.aquire();
        assertThat("Should get through three in under 100 millis", System.currentTimeMillis() - startTime, is(lessThan(100L)));
        rateLimiter.aquire();
        assertThat("Last should be over 100 millis", System.currentTimeMillis() - startTime, is(greaterThan(100L)));
    }

    @Test
    public void testRollingWindow()
    {
        RateLimiter rateLimiter = new RateLimiterBuilder().maxConcurrency(1)
                .queryLimit(1)
                .timeLength(10, TimeUnit.MILLISECONDS)
                .withClock(clock)
                .build();

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 30; i++)
        {
            rateLimiter.aquire();
        }
        assertThat("30 should take longer than 300 milliseconds", System.currentTimeMillis() - startTime, is(greaterThan(300L)));

    }
}
