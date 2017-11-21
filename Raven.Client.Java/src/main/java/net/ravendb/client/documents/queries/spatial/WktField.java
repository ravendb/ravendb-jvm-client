package net.ravendb.client.documents.queries.spatial;

import java.util.function.BiFunction;

public class WktField extends SpatialDynamicField {
    public final String wkt;

    public WktField(String wkt) {
        this.wkt = wkt;
    }

    @Override
    public String toField(BiFunction<String, Boolean, String> ensureValidFieldName) {
        return "wkt(" + ensureValidFieldName.apply(wkt, false) + ")";
    }
}
