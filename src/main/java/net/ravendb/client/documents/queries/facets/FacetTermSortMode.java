package net.ravendb.client.documents.queries.facets;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum FacetTermSortMode {
    VALUE_ASC,
    VALUE_DESC,
    COUNT_ASC,
    COUNT_DESC
}
