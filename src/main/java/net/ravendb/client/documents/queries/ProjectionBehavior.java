package net.ravendb.client.documents.queries;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum ProjectionBehavior {
    /**
     * Try to extract value from index field (if field value is stored in index),
     * on a failure (or when field value is not stored in index) extract value from a document
     */
    DEFAULT,

    /**
     * Try to extract value from index field (if field value is stored in index), on a failure skip field
     */
    FROM_INDEX,

    /**
     * Extract value from index field or throw
     */
    FROM_INDEX_OR_THROW,

    /**
     * Try to extract value from document field, on a failure skip field
     */
    FROM_DOCUMENT,

    /**
     * Extract value from document field or throw
     */
    FROM_DOCUMENT_OR_THROW
}
