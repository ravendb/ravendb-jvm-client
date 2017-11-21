package net.ravendb.client.documents.queries.spatial;

import java.util.function.BiFunction;

public abstract class SpatialDynamicField {
    public abstract String toField(BiFunction<String, Boolean, String> ensureValidFieldName);
}
