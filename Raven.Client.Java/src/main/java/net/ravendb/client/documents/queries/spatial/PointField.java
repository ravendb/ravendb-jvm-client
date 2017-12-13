package net.ravendb.client.documents.queries.spatial;

import java.util.function.BiFunction;

public class PointField extends DynamicSpatialField {
    public final String latitude;
    public final String longitude;

    public PointField(String latitude, String longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public String toField(BiFunction<String, Boolean, String> ensureValidFieldName) {
        return "spatial.point(" + ensureValidFieldName.apply(latitude, false) + ", " + ensureValidFieldName.apply(longitude, false) + ")";
    }
}
