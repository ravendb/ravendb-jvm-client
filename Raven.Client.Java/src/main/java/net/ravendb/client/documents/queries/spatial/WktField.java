package net.ravendb.client.documents.queries.spatial;

import java.util.function.BiFunction;

public class WktField extends DynamicSpatialField {
    public final String wkt;

    public WktField(String wkt) {
        this.wkt = wkt;
    }

    @Override
    public String toField(BiFunction<String, Boolean, String> ensureValidFieldName) {
        return "spatial.wkt(" + ensureValidFieldName.apply(wkt, false) + ")";
    }
}
