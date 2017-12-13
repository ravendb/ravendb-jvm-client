package net.ravendb.client.documents.queries.spatial;

import java.util.function.BiFunction;

public abstract class DynamicSpatialField {
    public abstract String toField(BiFunction<String, Boolean, String> ensureValidFieldName);
}
