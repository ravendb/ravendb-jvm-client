package net.ravendb.client.documents.queries.spatial;

import net.ravendb.client.Constants;
import net.ravendb.client.documents.indexes.spatial.SpatialRelation;
import net.ravendb.client.documents.indexes.spatial.SpatialUnits;

public class SpatialCriteriaFactory {

    public static SpatialCriteriaFactory INSTANCE = new SpatialCriteriaFactory();

    private SpatialCriteriaFactory() {}

    public SpatialCriteria relatesToShape(String shapeWKT, SpatialRelation relation) {
        return relatesToShape(shapeWKT, relation, Constants.Documents.Indexing.Spatial.DEFAULT_DISTANCE_ERROR_PCT);
    }

    public SpatialCriteria relatesToShape(String shapeWKT, SpatialRelation relation, double distErrorPercent) {
        return new WktCriteria(shapeWKT, relation, distErrorPercent);
    }

    public SpatialCriteria intersects(String shapeWKT) {
        return intersects(shapeWKT, Constants.Documents.Indexing.Spatial.DEFAULT_DISTANCE_ERROR_PCT);
    }

    public SpatialCriteria intersects(String shapeWKT, double distErrorPercent) {
        return relatesToShape(shapeWKT, SpatialRelation.INTERSECTS, distErrorPercent);
    }

    public SpatialCriteria contains(String shapeWKT) {
        return contains(shapeWKT, Constants.Documents.Indexing.Spatial.DEFAULT_DISTANCE_ERROR_PCT);
    }

    public SpatialCriteria contains(String shapeWKT, double distErrorPercent) {
        return relatesToShape(shapeWKT, SpatialRelation.CONTAINS, distErrorPercent);
    }

    public SpatialCriteria disjoint(String shapeWKT) {
        return disjoint(shapeWKT, Constants.Documents.Indexing.Spatial.DEFAULT_DISTANCE_ERROR_PCT);
    }

    public SpatialCriteria disjoint(String shapeWKT, double distErrorPercent) {
        return relatesToShape(shapeWKT, SpatialRelation.DISJOINT, distErrorPercent);
    }

    public SpatialCriteria within(String shapeWKT) {
        return within(shapeWKT, Constants.Documents.Indexing.Spatial.DEFAULT_DISTANCE_ERROR_PCT);
    }

    public SpatialCriteria within(String shapeWKT, double distErrorPercent) {
        return relatesToShape(shapeWKT, SpatialRelation.WITHIN, distErrorPercent);
    }

    public SpatialCriteria withinRadius(double radius, double latitude, double longitude) {
        return withinRadius(radius, latitude, longitude, null);
    }

    public SpatialCriteria withinRadius(double radius, double latitude, double longitude, SpatialUnits radiusUnits) {
        return withinRadius(radius, latitude, longitude, radiusUnits, Constants.Documents.Indexing.Spatial.DEFAULT_DISTANCE_ERROR_PCT);
    }

    public SpatialCriteria withinRadius(double radius, double latitude, double longitude, SpatialUnits radiusUnits, double distErrorPercent) {
        return new CircleCriteria(radius, latitude, longitude, radiusUnits, SpatialRelation.WITHIN, distErrorPercent);
    }
}
