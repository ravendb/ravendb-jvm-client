package net.ravendb.client.documents.indexes.spatial;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum SpatialRelation {
    WITHIN,
    CONTAINS,
    DISJOINT,
    INTERSECTS
}
