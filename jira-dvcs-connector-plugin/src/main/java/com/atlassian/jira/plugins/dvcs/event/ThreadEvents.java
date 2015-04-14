package com.atlassian.jira.plugins.dvcs.event;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Thread-local event bus.
 */
@Component
public class ThreadEvents
{
    private static final Logger logger = LoggerFactory.getLogger(ThreadEvents.class);

    /**
     * Captures thread-local events until they are published or discarded.
     */
    private final ThreadLocal<ThreadEventsCaptorImpl> threadEventCaptor = new ThreadLocal<ThreadEventsCaptorImpl>();

    public ThreadEvents()
    {
    }

    /**
     * Returns an EventsCapture instance that can be used to capture and publish events on the current thread. Captured
     * events can be processed using {@link ThreadEventsCaptor#processEach(ThreadEventsCaptor.Closure)}.
     * <p/>
     * Remember to <b>call {@code ThreadEventsCaptor.stopCapturing()} to terminate the capture</b> or risk leaking memory.
     *
     * @return a new EventsCapture
     * @throws java.lang.IllegalStateException if there is already an active ThreadEventsCapture on the current thread
     */
    @Nonnull
    public ThreadEventsCaptor startCapturing()
    {
        if (threadEventCaptor.get() != null)
        {
            // we could chain these up but YAGNI... just error out for now
            throw new IllegalStateException("There is already an active ThreadEventsCapture");
        }

        return new ThreadEventsCaptorImpl();
    }

    /**
     * Broadcasts the given event.
     *
     * @param event an event
     */
    public void broadcast(Object event)
    {
        ThreadEventsCaptorImpl eventCaptor = threadEventCaptor.get();
        if (eventCaptor != null)
        {
            eventCaptor.capture(event);
        }
        else
        {
            logger.debug("There is no active ThreadEventsCaptor. Dropping event: {}", event);
        }
    }

    private final class ThreadEventsCaptorImpl implements ThreadEventsCaptor
    {
        private static final int THRESHOLD = 30;
        private int index = 0;
        
        /**
         * Where we hold captured events until they are published or discarded.
         */
        private List<Object> capturedEvents = Lists.newArrayList();
        
        private List<DevSummaryChangedEvent> devSummaryChangedEvents = Lists.newArrayList();
        
        private Set<String> devSummaryChangedEventsKeysSeen = Sets.newHashSet();

        /**
         * Creates a new ThreadEventsCapture and sets it as the active capture in the enclosing ThreadEvents.
         */
        ThreadEventsCaptorImpl()
        {
            threadEventCaptor.set(this);
        }

        @Nonnull
        @Override
        public ThreadEventsCaptor stopCapturing()
        {
            threadEventCaptor.remove();
            return this;
        }

        @Override
        public void processEach(@Nonnull Closure<Object> closure)
        {
            processEach(Object.class, closure);
        }

        @Override
        public <T> void processEach(@Nonnull Class<T> eventClass, @Nonnull Closure<? super T> closure)
        {
            checkNotNull(eventClass, "eventClass");
            checkNotNull(closure, "closure");

            logger.warn(devSummaryChangedEvents.toString());
            capturedEvents.addAll(devSummaryChangedEvents);
            clearDevSummaryChangedEvents();
            final List<?> all = ImmutableList.copyOf(capturedEvents);
            for (Object object : all)
            {
                if (eventClass.isInstance(object))
                {
                    T event = eventClass.cast(object);

                    logger.debug("Processing event with {}: {}", closure, event);
                    closure.process(event);

                    // remove processed events from the list so that in case the
                    // closure above throws an exception we are still in a valid state.
                    capturedEvents.remove(event);
                }
            }

            logger.warn("Processed {} events of type {} with {}", new Object[] { all.size() - capturedEvents.size(), eventClass, closure });
        }

        private void clearDevSummaryChangedEvents()
        {
            devSummaryChangedEvents.clear();
            devSummaryChangedEventsKeysSeen.clear();
            index = 0;
        }

        /**
         * Captures an event that was raised while this EventsCapture is active.
         *
         * @param event an event
         */
        void capture(final Object event)
        {
            logger.debug("Capturing event: {}", event);
            
            if (event instanceof DevSummaryChangedEvent)
            {
                addNewDevSummaryChangedEvent((DevSummaryChangedEvent) event);
            }
            else
            {
                capturedEvents.add(event);
            }
        }

        private void addNewDevSummaryChangedEvent(@Nonnull final DevSummaryChangedEvent devSummaryChangedEvent)
        {
            final Set<String> unseenKeys = extractUnseenKeys(devSummaryChangedEvent.getIssueKeys());
            
            if (devSummaryChangedEvents.size() > 0 && roomToAddKeysToExistingEvent(unseenKeys.size()))
            {
                addUnseenIssueKeysToExistingEvent(unseenKeys);
            }
            else
            {
                addNewEventForUnseenIssueKeys(devSummaryChangedEvent, unseenKeys);
            }
            
            devSummaryChangedEventsKeysSeen.addAll(unseenKeys);
        }

        private void addUnseenIssueKeysToExistingEvent(@Nonnull final Set<String> unseenKeys)
        {
            final DevSummaryChangedEvent existingEvent = devSummaryChangedEvents.get(index);
            final Set<String> combinedKeys = Sets.newHashSet();
            combinedKeys.addAll(unseenKeys);
            combinedKeys.addAll(existingEvent.getIssueKeys());
            devSummaryChangedEvents.set(index, eventCopyWithNewIssueKeys(existingEvent, combinedKeys));
        }

        private void addNewEventForUnseenIssueKeys(
                @Nonnull final DevSummaryChangedEvent devSummaryChangedEvent,
                @Nonnull final Set<String> unseenKeys)
        {
            final DevSummaryChangedEvent newEvent = eventCopyWithNewIssueKeys(devSummaryChangedEvent, unseenKeys);
            devSummaryChangedEvents.add(newEvent);
            if (index < devSummaryChangedEvents.size() - 1)
            {
                index += 1;
            }
        }

        private boolean roomToAddKeysToExistingEvent(final int numKeysUnseen)
        {
            final int numKeysExisting = devSummaryChangedEvents.get(index).getIssueKeys().size();
            return (numKeysExisting + numKeysUnseen) < THRESHOLD;
        }

        @Nonnull
        private DevSummaryChangedEvent eventCopyWithNewIssueKeys(
                @Nonnull final DevSummaryChangedEvent devSummaryChangedEvent,
                @Nonnull final Set<String> newIssueKeys)
        {
            return new DevSummaryChangedEvent(
                    devSummaryChangedEvent.getRepositoryId(),
                    devSummaryChangedEvent.getDvcsType(),
                    ImmutableSet.copyOf(newIssueKeys),
                    new Date(devSummaryChangedEvent.getDate().getTime()));
        }

        @Nonnull
        private Set<String> extractUnseenKeys(@Nonnull final Set<String> issueKeys) 
        {
            final Set<String> unseenKeys = new HashSet<String>(issueKeys);
            unseenKeys.removeAll(devSummaryChangedEventsKeysSeen);
            return unseenKeys;
        }
    }
}
