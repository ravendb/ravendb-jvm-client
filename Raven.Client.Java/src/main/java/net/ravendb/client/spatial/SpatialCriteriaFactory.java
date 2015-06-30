package net.ravendb.client.spatial;

import net.ravendb.abstractions.data.SpatialIndexQuery;
import net.ravendb.abstractions.indexing.SpatialOptions.SpatialRelation;

public class SpatialCriteriaFactory {


  public SpatialCriteria relatesToShape(Object shape, SpatialRelation relation) {
    SpatialCriteria criteria = new SpatialCriteria();
    criteria.setShape(shape);
    criteria.setRelation(relation);
    return criteria;
  }

  public SpatialCriteria intersects(Object shape) {
    return relatesToShape(shape, SpatialRelation.INTERSECTS);
  }

  public SpatialCriteria contains(Object shape) {
    return relatesToShape(shape, SpatialRelation.CONTAINS);
  }

  public SpatialCriteria disjoint(Object shape) {
    return relatesToShape(shape, SpatialRelation.DISJOINT);
  }

  public SpatialCriteria within(Object shape) {
    return relatesToShape(shape, SpatialRelation.WITHIN);
  }

  /**
   * Order of parameters in this method is inconsistent with the rest of the API (x = longitude, y = latitude). Please use 'WithinRadius'.
   */
  @SuppressWarnings("boxing")
  @Deprecated
  public SpatialCriteria withinRadiusOf(double radius, double x, double y) {
    return relatesToShape(SpatialIndexQuery.getQueryShapeFromLatLon(y, x, radius), SpatialRelation.WITHIN);
  }

  public SpatialCriteria withinRadius(double radius, double latitude, double longitude) {
    return relatesToShape(SpatialIndexQuery.getQueryShapeFromLatLon(latitude, longitude, radius), SpatialRelation.WITHIN);
  }

}
