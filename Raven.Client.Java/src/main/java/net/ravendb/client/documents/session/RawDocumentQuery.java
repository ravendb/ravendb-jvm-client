package net.ravendb.client.documents.session;

import net.ravendb.client.documents.queries.QueryOperator;
import net.ravendb.client.primitives.Reference;

public class RawDocumentQuery<T> extends AbstractDocumentQuery<T, RawDocumentQuery<T>> implements IRawDocumentQuery<T> {

    public RawDocumentQuery(Class<T> clazz, InMemoryDocumentSessionOperations session, String rawQuery) {
        super(clazz, session, null, null, false, null, null, null);
        this.queryRaw = rawQuery;
    }

    public IRawDocumentQuery<T> skip(int count) {
        _skip(count);
        return this;
    }

    @Override
    public IRawDocumentQuery<T> take(int count) {
        _take(count);
        return this;
    }

    @Override
    public RawDocumentQuery<T> waitForNonStaleResults() {
        _waitForNonStaleResults();
        return this;
    }

    @Override
    public IRawDocumentQuery<T> showTimings() {
        _showTimings();
        return this;
    }

    @Override
    public IRawDocumentQuery<T> noTracking() {
        _noTracking();
        return this;
    }

    @Override
    public IRawDocumentQuery<T> noCaching() {
        _noCaching();
        return this;
    }

    @Override
    public IRawDocumentQuery<T> usingDefaultOperator(QueryOperator queryOperator) {
        _usingDefaultOperator(defaultOperatator);
        return this;
    }

    @Override
    public IRawDocumentQuery<T> statistics(Reference<QueryStatistics> stats) {
        _statistics(stats);
        return this;
    }
}
