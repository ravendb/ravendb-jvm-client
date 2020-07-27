package net.ravendb.client.documents.session;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.indexes.AbstractCommonApiForIndexes;
import net.ravendb.client.documents.queries.*;
import net.ravendb.client.documents.queries.timings.QueryTimings;
import net.ravendb.client.documents.session.tokens.WithEdgesToken;
import net.ravendb.client.documents.session.tokens.WithToken;
import net.ravendb.client.primitives.Reference;

import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public class GraphDocumentQuery<T> extends AbstractDocumentQuery<T, GraphDocumentQuery<T>> implements IGraphDocumentQuery<T> {

    public GraphDocumentQuery(Class<T> clazz, InMemoryDocumentSessionOperations session, String graphQuery) {
        super(clazz, session, null, null, false, null, null);
        _graphQuery(graphQuery);
    }

    @Override
    public IGraphDocumentQuery<T> usingDefaultOperator(QueryOperator queryOperator) {
        _usingDefaultOperator(queryOperator);
        return this;
    }

    @Override
    public IGraphDocumentQuery<T> waitForNonStaleResults() {
        return waitForNonStaleResults(null);
    }

    @Override
    public IGraphDocumentQuery<T> waitForNonStaleResults(Duration waitTimeout) {
        _waitForNonStaleResults(waitTimeout);
        return this;
    }

    @Override
    public IGraphDocumentQuery<T> addParameter(String name, Object value) {
        _addParameter(name, value);
        return this;
    }

    @Override
    public IGraphDocumentQuery<T> addAfterStreamExecutedListener(Consumer<ObjectNode> action) {
        _addAfterStreamExecutedListener(action);
        return this;
    }

    @Override
    public IGraphDocumentQuery<T> removeAfterStreamExecutedListener(Consumer<ObjectNode> action) {
        _removeAfterStreamExecutedListener(action);
        return this;
    }

    @Override
    public IGraphDocumentQuery<T> addBeforeQueryExecutedListener(Consumer<IndexQuery> action) {
        _addBeforeQueryExecutedListener(action);
        return this;
    }

    @Override
    public IGraphDocumentQuery<T> removeBeforeQueryExecutedListener(Consumer<IndexQuery> action) {
        _removeBeforeQueryExecutedListener(action);
        return this;
    }

    @Override
    public IGraphDocumentQuery<T> addAfterQueryExecutedListener(Consumer<QueryResult> action) {
        _addAfterQueryExecutedListener(action);
        return this;
    }

    @Override
    public IGraphDocumentQuery<T> removeAfterQueryExecutedListener(Consumer<QueryResult> action) {
        _removeAfterQueryExecutedListener(action);
        return this;
    }


    @Override
    public IGraphDocumentQuery<T> skip(int count) {
        _skip(count);
        return this;
    }

    @Override
    public IGraphDocumentQuery<T> statistics(Reference<QueryStatistics> stats) {
        _statistics(stats);
        return this;
    }

    @Override
    public IGraphDocumentQuery<T> take(int count) {
        _take(count);
        return this;
    }

    @Override
    public IGraphDocumentQuery<T> noCaching() {
        _noCaching();
        return this;
    }

    @Override
    public IGraphDocumentQuery<T> noTracking() {
        _noTracking();
        return this;
    }

    @Override
    public IGraphDocumentQuery<T> timings(Reference<QueryTimings> timings) {
        _includeTimings(timings);
        return this;
    }

    @Override
    public <TOther> IGraphDocumentQuery<T> with(Class<TOther> clazz, String alias, String rawQuery) {
        //noinspection unchecked
        return withInternal(clazz, alias, (AbstractDocumentQuery<TOther, ?>) getSession().advanced().rawQuery(clazz, rawQuery));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <TOther> IGraphDocumentQuery<T> with(String alias, IDocumentQuery<TOther> query) {
        setParameterPrefix("w" + withTokens.size() + "p");
        return withInternal(query.getQueryClass(), alias, (AbstractDocumentQuery<TOther, ?>) query);
    }

    @Override
    public <TOther> IGraphDocumentQuery<T> with(String alias, Function<DocumentQueryBuilder, IDocumentQuery<TOther>> queryFactory) {
        DocumentQuery<TOther> docQuery = (DocumentQuery<TOther>) queryFactory.apply(new DocumentQueryBuilder(getSession(), "w" + withTokens.size() + "p"));
        return withInternal(docQuery.getQueryClass(), alias, docQuery);
    }

    @Override
    public IGraphDocumentQuery<T> withEdges(String alias, String edgeSelector, String query) {
        withTokens.add(new WithEdgesToken(alias, edgeSelector, query));
        return this;
    }

    public static class DocumentQueryBuilder {
        private final IDocumentSession _session;
        private final String _parameterPrefix;

        public DocumentQueryBuilder(IDocumentSession session, String parameterPrefix) {
            _session = session;
            _parameterPrefix = parameterPrefix;
        }

        public <T> IDocumentQuery<T> query(Class<T> clazz) {
            DocumentQuery<T> query = (DocumentQuery<T>) _session.query(clazz);
            query.setParameterPrefix(_parameterPrefix);
            return query;
        }

        public <T> IDocumentQuery<T> query(Class<T> clazz, Query collectionOrIndexName) {
            DocumentQuery<T> query = (DocumentQuery<T>) _session.query(clazz, collectionOrIndexName);
            query.setParameterPrefix(_parameterPrefix);
            return query;
        }

        public <T, TIndex extends AbstractCommonApiForIndexes> IDocumentQuery<T> query(Class<T> clazz, Class<TIndex> indexClazz) {
            DocumentQuery<T> query = (DocumentQuery<T>) _session.query(clazz, indexClazz);
            query.setParameterPrefix(_parameterPrefix);
            return query;
        }
    }

    @SuppressWarnings("unused")
    private <TOther> IGraphDocumentQuery<T> withInternal(Class<TOther> clazz, String alias, AbstractDocumentQuery<TOther, ?> docQuery) {
        if (docQuery.selectTokens != null && !docQuery.selectTokens.isEmpty()) {
            throw new UnsupportedOperationException("Select is not permitted in a 'with' clause in query: " + docQuery);
        }

        for (Map.Entry<String, Object> keyValue : docQuery.queryParameters.entrySet()) {
            queryParameters.put(keyValue.getKey(), keyValue.getValue());
        }

        withTokens.add(new WithToken(alias, docQuery.toString()));

        if (docQuery.theWaitForNonStaleResults) {
            theWaitForNonStaleResults = true;

            if (timeout == null || timeout.compareTo(docQuery.timeout) < 0) {
                timeout = docQuery.timeout;
            }
        }

        return this;
    }

}

