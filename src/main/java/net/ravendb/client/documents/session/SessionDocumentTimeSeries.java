package net.ravendb.client.documents.session;

import net.ravendb.client.documents.session.loaders.ITimeSeriesIncludeBuilder;
import net.ravendb.client.documents.session.timeSeries.TimeSeriesEntry;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

public class SessionDocumentTimeSeries extends SessionTimeSeriesBase
        implements ISessionDocumentTimeSeries, ISessionDocumentIncrementalTimeSeries {

    public SessionDocumentTimeSeries(InMemoryDocumentSessionOperations session, String documentId, String name) {
        super(session, documentId, name);
    }

    public SessionDocumentTimeSeries(InMemoryDocumentSessionOperations session, Object entity, String name) {
        super(session, entity, name);
    }

    /**
     * {@inheritDoc}
     * @see ISessionDocumentTimeSeries#get()
     */
    @Override
    public TimeSeriesEntry[] get() {
        return get(null, null, 0, Integer.MAX_VALUE);
    }

    /**
     * {@inheritDoc}
     * @see ISessionDocumentTimeSeries#get(int, int)
     */
    @Override
    public TimeSeriesEntry[] get(int start, int pageSize) {
        return get(null, null, start, pageSize);
    }
    /**
     * {@inheritDoc}
     * @see ISessionDocumentTimeSeries#get(Date, Date)
     */
    @Override
    public TimeSeriesEntry[] get(Date from, Date to) {
        return get(from, to, 0, Integer.MAX_VALUE);
    }
    /**
     * {@inheritDoc}
     * @see ISessionDocumentTimeSeries#get(Date, Date, int)
     */
    @Override
    public TimeSeriesEntry[] get(Date from, Date to, int start) {
        return get(from, to, start, Integer.MAX_VALUE);
    }
    /**
     * {@inheritDoc}
     * @see ISessionDocumentTimeSeries#get(Date, Date, int, int)
     */
    @Override
    public TimeSeriesEntry[] get(Date from, Date to, int start, int pageSize) {
        return get(from, to, null, start, pageSize);
    }
    /**
     * {@inheritDoc}
     * @see ISessionDocumentTimeSeries#get(Date, Date, Consumer)
     */
    @Override
    public TimeSeriesEntry[] get(Date from, Date to, Consumer<ITimeSeriesIncludeBuilder> includes) {
        return get(from, to, includes, 0, Integer.MAX_VALUE);
    }
    /**
     * {@inheritDoc}
     * @see ISessionDocumentTimeSeries#get(Date, Date, Consumer, int)
     */
    @Override
    public TimeSeriesEntry[] get(Date from, Date to, Consumer<ITimeSeriesIncludeBuilder> includes, int start) {
        return get(from, to, includes, start, Integer.MAX_VALUE);
    }
    /**
     * {@inheritDoc}
     * @see ISessionDocumentTimeSeries#get(Date, Date, Consumer, int, int)
     */
    @Override
    public TimeSeriesEntry[] get(Date from, Date to, Consumer<ITimeSeriesIncludeBuilder> includes, int start, int pageSize) {
        if (notInCache(from, to)) {
            return getTimeSeriesAndIncludes(from, to, includes, start, pageSize);
        }

        List<TimeSeriesEntry> resultsToUser = serveFromCache(from, to, start, pageSize, includes);

        if (resultsToUser == null) {
            return null;
        }

        return resultsToUser.stream()
                .limit(pageSize)
                .toArray(TimeSeriesEntry[]::new);
    }

    @Override
    public Iterator<TimeSeriesEntry> stream(Instant from, Instant to, Duration offset) {
        return null;
    }
}
