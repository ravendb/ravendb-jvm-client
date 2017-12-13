package net.ravendb.client.documents.queries.spatial;

import net.ravendb.client.Constants;
import net.ravendb.client.documents.indexes.spatial.SpatialRelation;
import net.ravendb.client.documents.indexes.spatial.SpatialUnits;

public class SpatialCriteriaFactory {

    public static SpatialCriteriaFactory INSTANCE = new SpatialCriteriaFactory();

    private SpatialCriteriaFactory() {}

    public SpatialCriteria relatesToShape(String shapeWkt, SpatialRelation relation) {
        return relatesToShape(shapeWkt, relation, Constants.Documents.Indexing.Spatial.DEFAULT_DISTANCE_ERROR_PCT);
    }

    public SpatialCriteria relatesToShape(String shapeWkt, SpatialRelation relation, double distErrorPercent) {
        return new WktCriteria(shapeWkt, relation, distErrorPercent);
    }

    public SpatialCriteria intersects(String shapeWkt) {
        return intersects(shapeWkt, Constants.Documents.Indexing.Spatial.DEFAULT_DISTANCE_ERROR_PCT);
    }

    public SpatialCriteria intersects(String shapeWkt, double distErrorPercent) {
        return relatesToShape(shapeWkt, SpatialRelation.INTERSECTS, distErrorPercent);
    }

    public SpatialCriteria contains(String shapeWkt) {
        return contains(shapeWkt, Constants.Documents.Indexing.Spatial.DEFAULT_DISTANCE_ERROR_PCT);
    }

    public SpatialCriteria contains(String shapeWkt, double distErrorPercent) {
        return relatesToShape(shapeWkt, SpatialRelation.CONTAINS, distErrorPercent);
    }

    public SpatialCriteria disjoint(String shapeWkt) {
        return disjoint(shapeWkt, Constants.Documents.Indexing.Spatial.DEFAULT_DISTANCE_ERROR_PCT);
    }

    public SpatialCriteria disjoint(String shapeWkt, double distErrorPercent) {
        return relatesToShape(shapeWkt, SpatialRelation.DISJOINT, distErrorPercent);
    }

    public SpatialCriteria within(String shapeWkt) {
        return within(shapeWkt, Constants.Documents.Indexing.Spatial.DEFAULT_DISTANCE_ERROR_PCT);
    }

    public SpatialCriteria within(String shapeWkt, double distErrorPercent) {
        return relatesToShape(shapeWkt, SpatialRelation.WITHIN, distErrorPercent);
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
