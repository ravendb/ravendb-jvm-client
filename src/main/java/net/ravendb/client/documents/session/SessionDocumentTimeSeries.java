package net.ravendb.client.documents.session;

import net.ravendb.client.documents.session.loaders.ITimeSeriesIncludeBuilder;
import net.ravendb.client.documents.session.timeSeries.TimeSeriesEntry;

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

    @Override
    public TimeSeriesEntry[] get() {
        return get(null, null, 0, Integer.MAX_VALUE);
    }

    @Override
    public TimeSeriesEntry[] get(int start, int pageSize) {
        return get(null, null, start, pageSize);
    }

    @Override
    public TimeSeriesEntry[] get(Date from, Date to) {
        return get(from, to, 0, Integer.MAX_VALUE);
    }

    @Override
    public TimeSeriesEntry[] get(Date from, Date to, int start) {
        return get(from, to, start, Integer.MAX_VALUE);
    }

    @Override
    public TimeSeriesEntry[] get(Date from, Date to, int start, int pageSize) {
        return get(from, to, null, start, pageSize);
    }

    @Override
    public TimeSeriesEntry[] get(Date from, Date to, Consumer<ITimeSeriesIncludeBuilder> includes) {
        return get(from, to, includes, 0, Integer.MAX_VALUE);
    }

    @Override
    public TimeSeriesEntry[] get(Date from, Date to, Consumer<ITimeSeriesIncludeBuilder> includes, int start) {
        return get(from, to, includes, start, Integer.MAX_VALUE);
    }

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
}
