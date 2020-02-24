package net.ravendb.client.documents.queries.spatial;

import java.util.function.BiFunction;

public abstract class DynamicSpatialField {

    private double roundFactor;

    public abstract String toField(BiFunction<String, Boolean, String> ensureValidFieldName);

    public double getRoundFactor() {
        return roundFactor;
    }

    public DynamicSpatialField roundTo(double factor) {
        roundFactor = factor;
        return this;
    }
}
