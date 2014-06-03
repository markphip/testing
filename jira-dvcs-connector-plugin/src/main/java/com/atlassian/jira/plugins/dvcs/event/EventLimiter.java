package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.plugins.dvcs.sync.SyncConfig;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nonnull;

import static com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag.WEBHOOK_SYNC;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Limiter for synchronisation events.
 */
class EventLimiter
{
    private static final Logger logger = LoggerFactory.getLogger(EventLimiter.class);

    /**
     * Used to obtain the scheduled sync interval, which is necessary to come up with the limit for scheduled syncs.
     */
    private final SyncConfig syncConfig;

    /**
     * Used to check for limit overrides.
     */
    private final ApplicationProperties applicationProperties;

    /**
     * The number of permits remaining for each limit. The cache values are mutable as they need to be decremented as
     * events come in.
     */
    private final Cache<LimitKey, AtomicLong> remainingPermits = CacheBuilder.newBuilder().build(new CacheLoader<LimitKey, AtomicLong>()
    {
        @Override
        public AtomicLong load(@Nonnull LimitKey key) throws Exception
        {
            final long multiplier;
            if (!key.scheduledSync)
            {
                // apply limit as-is for webhook-initiated sync
                multiplier = 1;
            }
            else
            {
                // scale limits up for scheduled sync but only up to 1 hour's worth of events
                multiplier = Math.min(60, MILLISECONDS.toMinutes(syncConfig.scheduledSyncIntervalMillis()));
            }

            return new AtomicLong(multiplier * getEffectiveLimit(key.eventLimit));
        }
    });

    /**
     * Creates a new EventLimiter.
     *
     * @param syncConfig a SyncConfig
     * @param applicationProperties the ApplicationProperties
     */
    public EventLimiter(SyncConfig syncConfig, ApplicationProperties applicationProperties)
    {
        this.syncConfig = syncConfig;
        this.applicationProperties = applicationProperties;
    }

    /**
     * Tries to acquire a permit for raising the given event.
     *
     * @param event the event to acquire a permit for
     * @return true if a permit was acquired, false otherwise
     */
    public boolean isLimitExceeded(@Nonnull SyncEvent event)
    {
        if (event instanceof LimitedEvent)
        {
            AtomicLong remaining = remainingPermits.getUnchecked(LimitKey.of((LimitedEvent) event));

            return remaining.decrementAndGet() < 0;
        }

        // unlimited event
        return false;
    }

    /**
     * Returns the number of times that the limit was exceeded. This is equal to the number of times that {@link
     * #isLimitExceeded(SyncEvent)} returned true.
     *
     * @return the number of times that the limit was exceeded
     */
    public int getLimitExceededCount()
    {
        int exceeded = 0;
        for (AtomicLong remaining : remainingPermits.asMap().values())
        {
            exceeded -= Math.min(0, remaining.get());
        }

        return exceeded;
    }

    /**
     * Returns the effective limit for {@code eventLimit}, taking any overrides into account.
     *
     * @param eventLimit the EventLimit to calculate the effective limit for
     * @return the effective limit for {@code eventLimit}
     */
    private int getEffectiveLimit(EventLimit eventLimit)
    {
        String override = applicationProperties.getString(eventLimit.getOverrideLimitProperty());
        if (override != null)
        {
            try
            {
                return Integer.parseInt(override);
            }
            catch (NumberFormatException e)
            {
                logger.warn("Ignoring invalid limit override for {}: {}", eventLimit, override);
            }
        }

        return eventLimit.getDefaultLimit();
    }

    /**
     * Cache key for event limits.
     */
    private static class LimitKey
    {
        public static LimitKey of(@Nonnull LimitedEvent event)
        {
            return new LimitKey(event.getEventLimit(), !event.getSyncFlags().contains(WEBHOOK_SYNC));
        }

        @Nonnull
        private final EventLimit eventLimit;
        private final boolean scheduledSync;

        public LimitKey(EventLimit eventLimit, boolean scheduledSync)
        {
            this.eventLimit = checkNotNull(eventLimit);
            this.scheduledSync = scheduledSync;
        }

        @Override
        public boolean equals(final Object o)
        {
            if (this == o) { return true; }
            if (o == null || getClass() != o.getClass()) { return false; }

            final LimitKey limitKey = (LimitKey) o;

            if (scheduledSync != limitKey.scheduledSync) { return false; }
            if (eventLimit != limitKey.eventLimit) { return false; }

            return true;
        }

        @Override
        public int hashCode()
        {
            int result = eventLimit.hashCode();
            result = 31 * result + (scheduledSync ? 1 : 0);
            return result;
        }
    }
}
