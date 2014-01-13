package com.atlassian.jira.plugins.dvcs.spi.bitbucket.ratelimit;

import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class RateLimiter
{
    private final DelayQueue delayQueue;
    private final long queryLimit;
    private final long timeLength;
    private final TimeUnit timeUnit;
    private final Clock clock;

    public RateLimiter(long maxConcurrency,
                       long queryLimit,
                       long timeLength,
                       TimeUnit timeUnit,
                       Clock clock,
                       DelayQueue delayQueue)
    {
        this.queryLimit = queryLimit;
        this.timeLength = timeLength;
        this.timeUnit = timeUnit;
        this.clock = clock;
        this.delayQueue = delayQueue;

        DateTime initialToken = clock.getCurrentDate();
        for (int i = 0; i < maxConcurrency; i++)
        {
            delayQueue.add(new DelayedCall(initialToken));
        }
    }

    public void aquire()
    {
        try
        {
            delayQueue.take();
            delayQueue.add(delayToNext());
        }
        catch (InterruptedException e)
        {
            throw new SourceControlException("Interrupted while waiting for rate limiting.");
        }
    }

    private Delayed delayToNext()
    {
        return new DelayedCall(clock.getCurrentDate().withDurationAdded(new Duration(timeUnit.toMillis(timeLength) / queryLimit), 1));
    }

    private class DelayedCall implements Delayed
    {
        private final DateTime delayedUntil;

        DelayedCall(DateTime time) {
            this.delayedUntil = time;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(new Period(clock.getCurrentDate(), delayedUntil).getMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            return (new Long(getDelay(TimeUnit.MILLISECONDS)).compareTo(o.getDelay(TimeUnit.MILLISECONDS)));
        }
    }


}
