package net.ravendb.client.documents.queries.facets;

import java.util.Map;
import java.util.function.Consumer;

public interface IAggregationDocumentQuery<T> {

    IAggregationDocumentQuery<T> andAggregateBy(Consumer<IFacetBuilder<T>> builder);

    IAggregationDocumentQuery<T> andAggregateBy(FacetBase facet);

    Map<String, FacetResult> execute();

    //TBD Lazy<Dictionary<string, FacetResult>> ExecuteLazy(Action<Dictionary<string, FacetResult>> onEval = null);
}
