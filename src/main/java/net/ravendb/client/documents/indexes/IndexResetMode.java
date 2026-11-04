package net.ravendb.client.documents.indexes;

import net.ravendb.client.primitives.UseSharpEnum;

/**
 * Specifies how an index is reset.
 */
@UseSharpEnum
public enum IndexResetMode {
    /**
     * Resets the index in place, replacing the existing index with the newly rebuilt one.
     */
    IN_PLACE,

    /**
     * Resets the index in a side-by-side manner, allowing the new index to be built alongside the existing one.
     */
    SIDE_BY_SIDE
}
