package net.ravendb.client.documents.operations.cdcSink;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum CdcSinkRelationType {

    /**
     * One-to-many: stored as a JSON array.
     */
    ARRAY,

    /**
     * One-to-many: stored as a JSON object keyed by primary key value(s).
     * For composite PKs, the key is "pk1,pk2".
     */
    MAP,

    /**
     * Many-to-one: stored as a single value/object.
     */
    VALUE
}
