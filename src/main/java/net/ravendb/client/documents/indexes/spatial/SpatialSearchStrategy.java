package net.ravendb.client.documents.indexes.spatial;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum SpatialSearchStrategy {
    GEOHASH_PREFIX_TREE,
    QUAD_PREFIX_TREE,
    BOUNDING_BOX
}
