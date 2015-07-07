package net.ravendb.client.document;

import com.mysema.query.types.Expression;
import com.mysema.query.types.Path;
import net.ravendb.abstractions.basic.Reference;
import net.ravendb.abstractions.closure.Action1;
import net.ravendb.abstractions.data.Etag;
import net.ravendb.abstractions.data.IndexQuery;
import net.ravendb.abstractions.indexing.SpatialOptions.SpatialRelation;
import net.ravendb.abstractions.indexing.SpatialOptions.SpatialUnits;
import net.ravendb.client.FieldHighlightings;
import net.ravendb.client.IDocumentQueryCustomization;
import net.ravendb.client.shard.ShardReduceFunction;
import net.ravendb.client.spatial.SpatialCriteria;

import java.util.Date;


public class DocumentQueryCustomization implements IDocumentQueryCustomization {

  private DocumentQuery<?> delegate;
  public DocumentQueryCustomization(DocumentQuery< ? > delegate) {
    super();
    this.delegate = delegate;
  }

  @Override
  public IDocumentQueryCustomization waitForNonStaleResultsAsOfLastWrite() {
    delegate.waitForNonStaleResultsAsOfLastWrite();
    return this;
  }

  @Override
  public IDocumentQueryCustomization waitForNonStaleResultsAsOfLastWrite(long waitTimeout) {
    delegate.waitForNonStaleResultsAsOfLastWrite(waitTimeout);
    return this;
  }

  @Override
  public IDocumentQueryCustomization transformResults(ShardReduceFunction func) {
    delegate.transformResults(func);
    return this;
  }

  @Override
  public IDocumentQueryCustomization waitForNonStaleResultsAsOfNow() {
    delegate.waitForNonStaleResultsAsOfNow();
    return this;
  }

  @Override
  public IDocumentQueryCustomization waitForNonStaleResultsAsOfNow(long waitTimeout) {
    delegate.waitForNonStaleResultsAsOfNow(waitTimeout);
    return this;
  }

  @Override
  public IDocumentQueryCustomization waitForNonStaleResultsAsOf(Date cutOff) {
    delegate.waitForNonStaleResultsAsOf(cutOff);
    return this;
  }

  @Override
  public IDocumentQueryCustomization waitForNonStaleResultsAsOf(Date cutOff, long waitTimeout) {
    delegate.waitForNonStaleResultsAsOf(cutOff, waitTimeout);
    return this;
  }

  @Override
  public IDocumentQueryCustomization waitForNonStaleResultsAsOf(Etag cutOffEtag) {
    delegate.waitForNonStaleResultsAsOf(cutOffEtag);
    return this;
  }

  @Override
  public IDocumentQueryCustomization waitForNonStaleResultsAsOf(Etag cutOffEtag, long waitTimeout) {
    delegate.waitForNonStaleResultsAsOf(cutOffEtag, waitTimeout);
    return this;
  }

  @Override
  public IDocumentQueryCustomization waitForNonStaleResults() {
    delegate.waitForNonStaleResults();
    return this;
  }

  @Override
  public IDocumentQueryCustomization include(Path< ? > path) {
    delegate.include(path);
    return this;
  }

  @Override
  public IDocumentQueryCustomization include(String path) {
    delegate.include(path);
    return this;
  }

  @Override
  public IDocumentQueryCustomization include(Class<?> targetClass, Path< ? > path) {
    delegate.include(targetClass, path);
    return this;
  }

  @Override
  public IDocumentQueryCustomization waitForNonStaleResults(long waitTimeout) {
    delegate.waitForNonStaleResults(waitTimeout);
    return this;
  }

  @Override
  public IDocumentQueryCustomization withinRadiusOf(double radius, double latitude, double longitude) {
    delegate.withinRadiusOf(radius, latitude, longitude);
    return this;
  }

  @Override
  public IDocumentQueryCustomization withinRadiusOf(String fieldName, double radius, double latitude, double longitude) {
    delegate.withinRadiusOf(fieldName, radius, latitude, longitude);
    return this;
  }

  @Override
  public IDocumentQueryCustomization withinRadiusOf(double radius, double latitude, double longitude, SpatialUnits radiusUnits) {
    delegate.withinRadiusOf(radius, latitude, longitude, radiusUnits);
    return this;
  }

  @Override
  public IDocumentQueryCustomization withinRadiusOf(String fieldName, double radius, double latitude, double longitude, SpatialUnits radiusUnits) {
    delegate.withinRadiusOf(fieldName, radius, latitude, longitude, radiusUnits);
    return this;
  }

  @Override
  public IDocumentQueryCustomization relatesToShape(String fieldName, String shapeWKT, SpatialRelation rel) {
    delegate.relatesToShape(fieldName, shapeWKT, rel);
    return this;
  }

  @Override
  public IDocumentQueryCustomization spatial(String fieldName, SpatialCriteria criteria) {
    delegate.spatial(fieldName, criteria);
    return this;
  }

  @Override
  public IDocumentQueryCustomization showTimings() {
    delegate.showTimings();
    return this;
  }

  @Override
  public IDocumentQueryCustomization sortByDistance() {
    delegate.sortByDistance();
    return this;
  }

  @Override
  public IDocumentQueryCustomization sortByDistance(double lat, double lng) {
    delegate.sortByDistance(lat, lng);
    return this;
  }

  @Override
  public IDocumentQueryCustomization sortByDistance(double lat, double lng, String sortedFieldName) {
    delegate.sortByDistance(lat, lng, sortedFieldName);
    return this;
  }

  @Override
  public IDocumentQueryCustomization randomOrdering() {
    delegate.randomOrdering();
    return this;
  }

  @Override
  public IDocumentQueryCustomization randomOrdering(String seed) {
    delegate.randomOrdering(seed);
    return this;
  }

  @Override
  public IDocumentQueryCustomization customSortUsing(String typeName) {
    delegate.customSortUsing(typeName);
    return this;
  }

  @Override
  public IDocumentQueryCustomization customSortUsing(String typeName, boolean descending) {
    delegate.customSortUsing(typeName, descending);
    return this;
  }

  @Override
  public IDocumentQueryCustomization beforeQueryExecution(Action1<IndexQuery> action) {
    delegate.beforeQueryExecution(action);
    return this;
  }

  @Override
  public IDocumentQueryCustomization highlight(String fieldName, int fragmentLength, int fragmentCount, String fragmentsField) {
    delegate.highlight(fieldName, fragmentLength, fragmentCount, fragmentsField);
    return this;
  }

  @Override
  public IDocumentQueryCustomization highlight(String fieldName, int fragmentLength, int fragmentCount, Reference<FieldHighlightings> highlightings) {
    delegate.highlight(fieldName, fragmentLength, fragmentCount, highlightings);
    return this;
  }

  @Override
  public IDocumentQueryCustomization highlight(String fieldName, String fieldKeyName, int fragmentLength, int fragmentCount, Reference<FieldHighlightings> highlightings) {
    delegate.highlight(fieldName, fieldKeyName, fragmentLength, fragmentCount, highlightings);
    return this;
  }

  @Override
  public IDocumentQueryCustomization setAllowMultipleIndexEntriesForSameDocumentToResultTransformer(boolean value) {
    delegate.setAllowMultipleIndexEntriesForSameDocumentToResultTransformer(value);
    return this;
  }

  @Override
  public IDocumentQueryCustomization setHighlighterTags(String preTag, String postTag) {
    delegate.setHighlighterTags(preTag, postTag);
    return this;
  }

  @Override
  public IDocumentQueryCustomization setHighlighterTags(String[] preTags, String[] postTags) {
    delegate.setHighlighterTags(preTags, postTags);
    return this;
  }

  @Override
  public IDocumentQueryCustomization noTracking() {
    delegate.noTracking();
    return this;
  }

  @Override
  public IDocumentQueryCustomization noCaching() {
    delegate.noCaching();
    return this;
  }

  @Override
  public String toString() {
    return delegate.toString();
  }

  @Override
  public IDocumentQueryCustomization addOrder(String fieldName) {
    delegate.addOrder(fieldName);
    return this;
  }

  @Override
  public IDocumentQueryCustomization addOrder(String fieldName, boolean descending) {
    delegate.addOrder(fieldName, descending);
    return this;
  }

  @Override
  public IDocumentQueryCustomization addOrder(String fieldName, boolean descending, Class fieldType) {
    delegate.addOrder(fieldName, descending, fieldType);
    return this;
  }

  @Override
  public IDocumentQueryCustomization addOrder(Expression<?> propertySelector) {
    delegate.addOrder(propertySelector);
    return this;
  }

  @Override
  public IDocumentQueryCustomization addOrder(Expression<?> propertySelector, boolean descending) {
    delegate.addOrder(propertySelector, descending);
    return this;
  }

  @Override
  public IDocumentQueryCustomization alphaNumericOrdering(String fieldName) {
    delegate.alphaNumericOrdering(fieldName);
    return this;
  }

  @Override
  public IDocumentQueryCustomization alphaNumericOrdering(String fieldName, boolean descending) {
    delegate.alphaNumericOrdering(fieldName, descending);
    return this;
  }

  @Override
  public IDocumentQueryCustomization alphaNumericOrdering(Expression<?> propertySelector) {
    delegate.alphaNumericOrdering(propertySelector);
    return this;
  }

  @Override
  public IDocumentQueryCustomization alphaNumericOrdering(Expression<?> propertySelector, boolean descending) {
    delegate.alphaNumericOrdering(propertySelector, descending);
    return this;
  }
}
