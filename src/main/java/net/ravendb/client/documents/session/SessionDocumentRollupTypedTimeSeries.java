package net.ravendb.client.documents.session;

import net.ravendb.client.documents.session.timeSeries.TimeSeriesEntry;
import net.ravendb.client.documents.session.timeSeries.TypedTimeSeriesRollupEntry;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;

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
    /**
     * {@inheritDoc}
     * @see ISessionDocumentRollupTypedTimeSeries#get()
     */
    @Override
    public TypedTimeSeriesRollupEntry<T>[] get() {
        return get(null, null, 0, Integer.MAX_VALUE);
    }
    /**
     * {@inheritDoc}
     * @see ISessionDocumentRollupTypedTimeSeries#get(Date, Date)
     */
    @Override
    public TypedTimeSeriesRollupEntry<T>[] get(Date from, Date to) {
        return get(from, to, 0, Integer.MAX_VALUE);
    }
    /**
     * {@inheritDoc}
     * @see ISessionDocumentRollupTypedTimeSeries#get(Date, Date, int)
     */
    @Override
    public TypedTimeSeriesRollupEntry<T>[] get(Date from, Date to, int start) {
        return get(from, to, start, Integer.MAX_VALUE);
    }
    /**
     * {@inheritDoc}
     * @see ISessionDocumentRollupTypedTimeSeries#get(Date, Date, int, int)
     */
    @SuppressWarnings("unchecked")
    @Override
    public TypedTimeSeriesRollupEntry<T>[] get(Date from, Date to, int start, int pageSize) {
        if (notInCache(from, to)) {
            TimeSeriesEntry[] results = getTimeSeriesAndIncludes(from, to, null, start, pageSize);

            return Arrays.stream(results)
                    .map(x -> TypedTimeSeriesRollupEntry.fromEntry(_clazz, x))
                    .toArray(TypedTimeSeriesRollupEntry[]::new);
        }

        TimeSeriesEntry[] results = getFromCache(from, to, null, start, pageSize);
        return Arrays.stream(results)
                .map(x -> TypedTimeSeriesRollupEntry.fromEntry(_clazz, x))
                .toArray(TypedTimeSeriesRollupEntry[]::new);
    }
    /**
     * {@inheritDoc}
     * @see ISessionDocumentRollupTypedAppendTimeSeriesBase#append(TypedTimeSeriesRollupEntry) 
     */
    @Override
    public void append(TypedTimeSeriesRollupEntry<T> entry) {
        double[] values = entry.getValuesFromMembers();
        append(entry.getTimestamp(), values, entry.getTag());
    }

    @Override
    public Iterator<TypedTimeSeriesRollupEntry<T>> stream(Instant from, Instant to, Duration offset) {
        return this.getTimeSeriesStreamResult(from, to, offset, TypedTimeSeriesRollupEntry.class);
    }
}
