package net.ravendb.client.documents.session;

import net.ravendb.client.documents.session.timeSeries.TimeSeriesEntry;
import net.ravendb.client.documents.session.timeSeries.TimeSeriesValuesHelper;
import net.ravendb.client.documents.session.timeSeries.TypedTimeSeriesEntry;
import net.ravendb.client.documents.session.timeSeries.TypedTimeSeriesRollupEntry;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class SessionDocumentRollupTypedTimeSeries<T> extends SessionTimeSeriesBase
    implements ISessionDocumentRollupTypedTimeSeries<T> {

    private final Class<T> _clazz;

    public SessionDocumentRollupTypedTimeSeries(Class<T> clazz, InMemoryDocumentSessionOperations session, String documentId, String name) {
        super(session, documentId, name);
        _clazz = clazz;
    }

    public SessionDocumentRollupTypedTimeSeries(Class<T> clazz, InMemoryDocumentSessionOperations session, Object entity, String name) {
        super(session, entity, name);
        _clazz = clazz;
    }

    @Override
    public List<TypedTimeSeriesRollupEntry<T>> get() {
        return get(null, null, 0, Integer.MAX_VALUE);
    }

    @Override
    public List<TypedTimeSeriesRollupEntry<T>> get(Date from, Date to) {
        return get(from, to, 0, Integer.MAX_VALUE);
    }

    @Override
    public List<TypedTimeSeriesRollupEntry<T>> get(Date from, Date to, int start) {
        return get(from, to, start, Integer.MAX_VALUE);
    }

    @Override
    public List<TypedTimeSeriesRollupEntry<T>> get(Date from, Date to, int start, int pageSize) {
        List<TimeSeriesEntry> results = getInternal(from, to, start, pageSize);

        return results
                .stream()
                .map(x -> TypedTimeSeriesRollupEntry.fromEntry(_clazz, x))
                .collect(Collectors.toList());
    }

    @Override
    public void append(TypedTimeSeriesRollupEntry<T> entry) {
        double[] values = entry.getValuesFromMembers();
        append(entry.getTimestamp(), values, entry.getTag());
    }
}
