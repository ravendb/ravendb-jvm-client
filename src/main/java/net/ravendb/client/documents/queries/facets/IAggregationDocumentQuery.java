package net.ravendb.client.documents.queries.facets;

import net.ravendb.client.documents.Lazy;

import java.util.Map;
import java.util.function.Consumer;

public interface IAggregationDocumentQuery<T> {

    IAggregationDocumentQuery<T> andAggregateBy(Consumer<IFacetBuilder<T>> builder);

    IAggregationDocumentQuery<T> andAggregateBy(FacetBase facet);

    Map<String, FacetResult> execute();

    Lazy<Map<String, FacetResult>> executeLazy();

    Lazy<Map<String, FacetResult>> executeLazy(Consumer<Map<String, FacetResult>> onEval);
}
