package net.ravendb.abstractions.data;

import net.ravendb.abstractions.basic.SharpEnum;
import net.ravendb.abstractions.indexing.SpatialOptions.SpatialRelation;
import net.ravendb.abstractions.indexing.SpatialOptions.SpatialUnits;
import net.ravendb.client.utils.UrlUtils;

/**
 * A query using spatial filtering
 */
public class SpatialIndexQuery extends IndexQuery {
  public static String getQueryShapeFromLatLon(double lat, double lng, double radius) {
    return String.format(Constants.getDefaultLocale(), "Circle(%.6f %.6f d=%.6f)", lng, lat, radius);
  }

  private String queryShape;
  private SpatialRelation spatialRelation;
  private double distanceErrorPercentage;
  private SpatialUnits radiusUnitOverride;

  private String spatialFieldName = Constants.DEFAULT_SPATIAL_FIELD_NAME;


  public String getSpatialFieldName() {
    return spatialFieldName;
  }

  public void setSpatialFieldName(String spatialFieldName) {
    this.spatialFieldName = spatialFieldName;
  }

  /**
   * Shape in WKT format.
   */
  public String getQueryShape() {
    return queryShape;
  }

  /**
   * Shape in WKT format.
   * @param queryShape
   */
  public void setQueryShape(String queryShape) {
    this.queryShape = queryShape;
  }

  /**
   * Spatial relation (Within, Contains, Disjoint, Intersects, Nearby)
   */
  public SpatialRelation getSpatialRelation() {
    return spatialRelation;
  }

  /**
   * Spatial relation (Within, Contains, Disjoint, Intersects, Nearby)
   * @param spatialRelation
   */
  public void setSpatialRelation(SpatialRelation spatialRelation) {
    this.spatialRelation = spatialRelation;
  }

  /**
   * A measure of acceptable error of the shape as a fraction. This effectively
   * inflates the size of the shape but should not shrink it.
   * {@value Default value is 0.025}
   */
  public double getDistanceErrorPercentage() {
    return distanceErrorPercentage;
  }

  /**
   * A measure of acceptable error of the shape as a fraction. This effectively
   * inflates the size of the shape but should not shrink it.
   * {@value Default value is 0.025}
   * @param distanceErrorPercentage
   */
  public void setDistanceErrorPercentage(double distanceErrorPercentage) {
    this.distanceErrorPercentage = distanceErrorPercentage;
  }

  /**
   * Overrides the units defined in the spatial index
   */
  public SpatialUnits getRadiusUnitOverride() {
    return radiusUnitOverride;
  }

  /**
   * Overrides the units defined in the spatial index
   * @param radiusUnitOverride
   */
  public void setRadiusUnitOverride(SpatialUnits radiusUnitOverride) {
    this.radiusUnitOverride = radiusUnitOverride;
  }

  /**
   * Initializes a new instance of the {@link #SpatialIndexQuery() SpatialIndexQuery} class.
   * @param query
   */
  public SpatialIndexQuery(IndexQuery query) {
    setQuery(query.getQuery());
    setStart(query.getStart());
    setCutoff(query.getCutoff());
    setCutoffEtag(query.getCutoffEtag());
    setWaitForNonStaleResultsAsOfNow(query.isWaitForNonStaleResultsAsOfNow());
    setPageSize(query.getPageSize());
    setFieldsToFetch(query.getFieldsToFetch());
    setSortedFields(query.getSortedFields());
    setHighlighterPreTags(query.getHighlighterPreTags());
    setHighlighterPostTags(query.getHighlighterPostTags());
    setHighlighterKeyName(query.getHighlighterKeyName());
    setHighlightedFields(query.getHighlightedFields());
    setTransformerParameters(query.getTransformerParameters());
    setResultsTransformer(query.getResultsTransformer());
    setDefaultOperator(query.getDefaultOperator());
    setAllowMultipleIndexEntriesForSameDocumentToResultTransformer(query.isAllowMultipleIndexEntriesForSameDocumentToResultTransformer());
  }

  /**
   * Initializes a new instance of the {@link SpatialIndexQuery} class.
   */
  public SpatialIndexQuery() {
    distanceErrorPercentage = Constants.DEFAULT_SPATIAL_DISTANCE_ERROR_PCT;
  }

  /**
   *  Gets the custom query string variables.
   */
  @Override
  protected String getCustomQueryStringVariables() {
    String unitsParam = "";
    if (radiusUnitOverride != null) {
      unitsParam = String.format("&spatialUnits=%s", SharpEnum.value(radiusUnitOverride));
    }
    return String.format(Constants.getDefaultLocale(), "queryShape=%s&spatialRelation=%s&spatialField=%s&distErrPrc=%.5f%s", UrlUtils.escapeDataString(queryShape), SharpEnum.value(spatialRelation), spatialFieldName,
        distanceErrorPercentage, unitsParam);
  }

}
