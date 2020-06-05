package net.ravendb.client.documents.session;

import net.ravendb.client.documents.session.timeSeries.TimeSeriesValuesHelper;
import net.ravendb.client.documents.session.timeSeries.TypedTimeSeriesEntry;
import net.ravendb.client.exceptions.RavenException;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class SessionDocumentTypedTimeSeries<T> extends SessionTimeSeriesBase implements ISessionDocumentTypedTimeSeries<T> {

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
    public List<TypedTimeSeriesEntry<T>> get() {
        return get(null, null, 0, Integer.MAX_VALUE);
    }

    @Override
    public List<TypedTimeSeriesEntry<T>> get(Date from, Date to) {
        return get(from, to, 0, Integer.MAX_VALUE);
    }

    @Override
    public List<TypedTimeSeriesEntry<T>> get(Date from, Date to, int start) {
        return get(from, to, start, Integer.MAX_VALUE);
    }

    @Override
    public List<TypedTimeSeriesEntry<T>> get(Date from, Date to, int start, int pageSize) {
        return getInternal(from, to, start, pageSize)
                .stream()
                .map(x -> x.asTypedEntry(_clazz))
                .collect(Collectors.toList());
    }

    @Override
    public void append(Date timestamp, T entry) {
        append(timestamp, entry, null);
    }

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
