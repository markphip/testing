package com.atlassian.jira.plugins.dvcs.smartcommits;

import com.atlassian.jira.plugins.dvcs.analytics.smartcommits.SmartCommitsAnalyticsService;
import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.util.concurrent.Promise;
import com.atlassian.util.concurrent.Promises;
import com.atlassian.util.concurrent.ThreadFactories;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

@ExportAsService (SmartcommitsChangesetsProcessor.class)
@Component
public class DefaultSmartcommitsChangesetsProcessor implements SmartcommitsChangesetsProcessor, DisposableBean
{

    /**
     * Logger of this class.
     */
    private static final Logger log = LoggerFactory.getLogger(DefaultSmartcommitsChangesetsProcessor.class);

    private final ListeningExecutorService executor;
    private final SmartcommitsDarkFeature smartcommitsFeature;
    private final SmartcommitsService smartcommitService;
    private final CommitMessageParser commitParser;
    private final ChangesetDao changesetDao;
    private final SmartCommitsAnalyticsService smartCommitsAnalyticsService;

    @Autowired
    public DefaultSmartcommitsChangesetsProcessor(
            @Nonnull final ChangesetDao changesetDao,
            @Nonnull final SmartcommitsDarkFeature smartcommitsFeature,
            @Nonnull final SmartcommitsService smartcommitService,
            @Nonnull final CommitMessageParser commitParser,
            @Nonnull final SmartCommitsAnalyticsService smartCommitsAnalyticsService)
    {
        this.changesetDao = checkNotNull(changesetDao);
        this.smartcommitService = checkNotNull(smartcommitService);
        this.smartcommitsFeature = checkNotNull(smartcommitsFeature);
        this.commitParser = checkNotNull(commitParser);
        this.smartCommitsAnalyticsService = checkNotNull(smartCommitsAnalyticsService);

        // a listening decorator returns ListenableFuture, which we then wrap in a Promise. using JDK futures directly
        // leads to an extra thread being created for the lifetime of the Promise (see Guava JdkFutureAdapters)
        executor = MoreExecutors.listeningDecorator(createThreadPool());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() throws Exception
    {
        executor.shutdown();
        if (!executor.awaitTermination(1, TimeUnit.MINUTES))
        {
            log.error("Unable properly shutdown queued tasks.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public Promise<Void> startProcess(Progress forProgress, Repository repository, ChangesetService changesetService)
    {
        if (smartcommitsFeature.isDisabled())
        {
            log.debug("Smart commits processing disabled by feature flag.");
            return Promises.promise(null);
        }

        return Promises.forListenableFuture(executor.submit(
                new SmartcommitOperation(changesetDao, commitParser, smartcommitService, forProgress, repository, changesetService,smartCommitsAnalyticsService)
        ));
    }

    private ThreadPoolExecutor createThreadPool()
    {
        return new ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
                ThreadFactories.namedThreadFactory("DVCSConnector.SmartCommitsProcessor"));
    }
}
