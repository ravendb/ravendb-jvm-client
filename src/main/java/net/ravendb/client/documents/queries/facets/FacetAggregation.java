package net.ravendb.client.documents.queries.facets;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum FacetAggregation {
    NONE,
    MAX,
    MIN,
    AVERAGE,
    SUM
}
