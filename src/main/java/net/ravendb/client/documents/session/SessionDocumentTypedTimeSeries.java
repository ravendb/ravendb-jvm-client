package net.ravendb.client.documents.session;

import net.ravendb.client.documents.session.timeSeries.TimeSeriesEntry;
import net.ravendb.client.documents.session.timeSeries.TimeSeriesValuesHelper;
import net.ravendb.client.documents.session.timeSeries.TypedTimeSeriesEntry;

import java.util.Arrays;
import java.util.Date;

public class SessionDocumentTypedTimeSeries<T> extends SessionTimeSeriesBase
        implements ISessionDocumentTypedTimeSeries<T>, ISessionDocumentTypedIncrementalTimeSeries<T> {

    private final Class<T> _clazz;

    public SessionDocumentTypedTimeSeries(Class<T> clazz, InMemoryDocumentSessionOperations session, String documentId, String name) {
        super(session, documentId, name);
        _clazz = clazz;
    }

    public SessionDocumentTypedTimeSeries(Class<T> clazz, InMemoryDocumentSessionOperations session, Object entity, String name) {
        super(session, entity, name);
        _clazz = clazz;
    }

    @Override
    public TypedTimeSeriesEntry<T>[] get() {
        return get(null, null, 0, Integer.MAX_VALUE);
    }

    @Override
    public TypedTimeSeriesEntry<T>[] get(Date from, Date to) {
        return get(from, to, 0, Integer.MAX_VALUE);
    }

    @Override
    public TypedTimeSeriesEntry<T>[] get(Date from, Date to, int start) {
        return get(from, to, start, Integer.MAX_VALUE);
    }

    @SuppressWarnings("unchecked")
    @Override
    public TypedTimeSeriesEntry<T>[] get(Date from, Date to, int start, int pageSize) {
        if (notInCache(from, to)) {
            TimeSeriesEntry[] entries = getTimeSeriesAndIncludes(from, to, null, start, pageSize);
            if (entries == null) {
                return null;
            }
            return Arrays.stream(entries)
                    .map(x -> x.asTypedEntry(_clazz))
                    .toArray(TypedTimeSeriesEntry[]::new);
        }

        TimeSeriesEntry[] results = getFromCache(from, to, null, start, pageSize);
        return Arrays.stream(results)
                .map(x -> x.asTypedEntry(_clazz))
                .toArray(TypedTimeSeriesEntry[]::new);
    }

    @Override
    public void append(Date timestamp, T entry) {
        append(timestamp, entry, null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void append(Date timestamp, T entry, String tag) {
        double[] values = TimeSeriesValuesHelper.getValues((Class<T>)entry.getClass(), entry);
        append(timestamp, values, tag);
    }

    @Override
    public void append(TypedTimeSeriesEntry<T> entry) {
        append(entry.getTimestamp(), entry.getValue(), entry.getTag());
    }
}
