package net.ravendb.client.documents.session;

import net.ravendb.client.documents.queries.IndexQuery;
import net.ravendb.client.documents.queries.QueryOperator;
import net.ravendb.client.documents.queries.QueryResult;
import net.ravendb.client.primitives.Reference;

import java.time.Duration;
import java.util.function.Consumer;

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
    public IRawDocumentQuery<T> waitForNonStaleResults(Duration waitTimeout) {
        _waitForNonStaleResults(waitTimeout);
        return this;
    }

    @Override
    public IRawDocumentQuery<T> waitForNonStaleResultsAsOf(long cutoffEtag) {
        _waitForNonStaleResultsAsOf(cutoffEtag);
        return this;
    }

    @Override
    public IRawDocumentQuery<T> waitForNonStaleResultsAsOf(long cutOffEtag, Duration waitTimeout) {
        _waitForNonStaleResultsAsOf(cutoffEtag, waitTimeout);
        return this;
    }

    //TBD public IRawDocumentQuery<T> showTimings() {

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

    @Override
    public IRawDocumentQuery<T> removeAfterQueryExecutedListener(Consumer<QueryResult> action) {
        _removeAfterQueryExecutedListener(action);
        return this;
    }

    @Override
    public IRawDocumentQuery<T> addAfterQueryExecutedListener(Consumer<QueryResult> action) {
        _addAfterQueryExecutedListener(action);
        return this;
    }

    @Override
    public IRawDocumentQuery<T> addBeforeQueryExecutedListener(Consumer<IndexQuery> action) {
        _addBeforeQueryExecutedListener(action);
        return this;
    }

    @Override
    public IRawDocumentQuery<T> removeBeforeQueryExecutedListener(Consumer<IndexQuery> action) {
        _removeBeforeQueryExecutedListener(action);
        return this;
    }

    @Override
    public IRawDocumentQuery<T> addParameter(String name, Object value) {
        _addParameter(name, value);
        return this;
    }

}
