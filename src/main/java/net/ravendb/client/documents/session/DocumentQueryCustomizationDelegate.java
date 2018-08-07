package net.ravendb.client.documents.session;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.queries.IndexQuery;
import net.ravendb.client.documents.queries.QueryResult;
import net.ravendb.client.documents.queries.timings.QueryTimings;
import net.ravendb.client.documents.session.operations.QueryOperation;
import net.ravendb.client.primitives.Reference;

import java.time.Duration;
import java.util.function.Consumer;

@SuppressWarnings("unchecked")
public class DocumentQueryCustomizationDelegate implements IDocumentQueryCustomization {

    private final AbstractDocumentQuery query;

    public DocumentQueryCustomizationDelegate(AbstractDocumentQuery query) {
        this.query = query;
    }

    @Override
    public QueryOperation getQueryOperation() {
        return query.getQueryOperation();
    }

    public IDocumentQueryCustomization addBeforeQueryExecutedListener(Consumer<IndexQuery> action) {
        query._addBeforeQueryExecutedListener(action);
        return this;
    }

    @Override
    public IDocumentQueryCustomization removeBeforeQueryExecutedListener(Consumer<IndexQuery> action) {
        query._removeBeforeQueryExecutedListener(action);
        return this;
    }

    @Override
    public IDocumentQueryCustomization addAfterQueryExecutedListener(Consumer<QueryResult> action) {
        query._addAfterQueryExecutedListener(action);
        return this;
    }

    @Override
    public IDocumentQueryCustomization removeAfterQueryExecutedListener(Consumer<QueryResult> action) {
        query._removeAfterQueryExecutedListener(action);
        return this;
    }

    @Override
    public IDocumentQueryCustomization addAfterStreamExecutedCallback(Consumer<ObjectNode> action) {
        query._addAfterStreamExecutedListener(action);
        return this;
    }

    @Override
    public IDocumentQueryCustomization removeAfterStreamExecutedCallback(Consumer<ObjectNode> action) {
        query._removeAfterStreamExecutedListener(action);
        return this;
    }

    @Override
    public IDocumentQueryCustomization noCaching() {
        query._noCaching();
        return this;
    }

    @Override
    public IDocumentQueryCustomization noTracking() {
        query._noTracking();
        return this;
    }

    @Override
    public IDocumentQueryCustomization timings(Reference<QueryTimings> timings) {
        query._includeTimings(timings);
        return this;
    }

    @Override
    public IDocumentQueryCustomization randomOrdering() {
        query._randomOrdering();
        return this;
    }

    @Override
    public IDocumentQueryCustomization randomOrdering(String seed) {
        query._randomOrdering(seed);
        return this;
    }

    @Override
    public IDocumentQueryCustomization waitForNonStaleResults() {
        query._waitForNonStaleResults(null);
        return this;
    }

    @Override
    public IDocumentQueryCustomization waitForNonStaleResults(Duration waitTimeout) {
        query._waitForNonStaleResults(waitTimeout);
        return this;
    }
}
