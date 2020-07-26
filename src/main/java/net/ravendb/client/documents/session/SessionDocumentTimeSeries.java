package net.ravendb.client.documents.session;

import net.ravendb.client.documents.session.timeSeries.TimeSeriesEntry;

import java.util.*;

public class SessionDocumentTimeSeries extends SessionTimeSeriesBase
        implements ISessionDocumentTimeSeries {

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
    public TimeSeriesEntry[] get(Date from, Date to) {
        return get(from, to, 0, Integer.MAX_VALUE);
    }

    @Override
    public TimeSeriesEntry[] get(Date from, Date to, int start) {
        return get(from, to, start, Integer.MAX_VALUE);
    }

    @Override
    public TimeSeriesEntry[] get(Date from, Date to, int start, int pageSize) {
        return getInternal(from, to, start, pageSize);
    }
}
