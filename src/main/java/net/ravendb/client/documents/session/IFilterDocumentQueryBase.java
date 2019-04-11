package net.ravendb.client.documents.session;

import net.ravendb.client.documents.indexes.spatial.SpatialRelation;
import net.ravendb.client.documents.indexes.spatial.SpatialUnits;
import net.ravendb.client.documents.queries.SearchOperator;
import net.ravendb.client.documents.queries.moreLikeThis.MoreLikeThisBase;
import net.ravendb.client.documents.queries.spatial.DynamicSpatialField;
import net.ravendb.client.documents.queries.spatial.SpatialCriteria;
import net.ravendb.client.documents.queries.spatial.SpatialCriteriaFactory;

import java.util.Collection;
import java.util.function.Function;

public interface IFilterDocumentQueryBase<T, TSelf extends IDocumentQueryBase<T, TSelf>> extends IQueryBase<T, TSelf> {

    /**
     * Negate the next operation
     * @return Query instance
     */
    TSelf not();

    /**
     *  Add an AND to the query
     *  @return Query instance
     */
    TSelf andAlso();

    /**
     * Simplified method for closing a clause within the query
     * @return Query instance
     */
    TSelf closeSubclause();

    /**
     * Performs a query matching ALL of the provided values against the given field (AND)
     * @param fieldName Field name
     * @param values values to match
     * @return Query instance
     */
    TSelf containsAll(String fieldName, Collection<Object> values);

    //TBD expr TSelf ContainsAll<TValue>(Expression<Func<T, TValue>> propertySelector, IEnumerable<TValue> values);

    /**
     * Performs a query matching ANY of the provided values against the given field (OR)
     * @param fieldName Field name
     * @param values values to match
     * @return Query instance
     */
    TSelf containsAny(String fieldName, Collection<Object> values);

    //TBD expr TSelf ContainsAny<TValue>(Expression<Func<T, TValue>> propertySelector, IEnumerable<TValue> values);

    /**
     * Negate the next operation
     */
    void negateNext();

    /**
     *  Simplified method for opening a new clause within the query
     *  @return Query instance
     */
    TSelf openSubclause();

    /**
     * Add an OR to the query
     * @return Query instance
     */
    TSelf orElse();

    /**
     * Perform a search for documents which fields that match the searchTerms.
     * If there is more than a single term, each of them will be checked independently.
     *
     * Space separated terms e.g. 'John Adam' means that we will look in selected field for 'John'
     * or 'Adam'.
     * @param fieldName Field name
     * @param searchTerms Search terms
     * @return Query instance
     */
    TSelf search(String fieldName, String searchTerms);

    /**
     * Perform a search for documents which fields that match the searchTerms.
     * If there is more than a single term, each of them will be checked independently.
     *
     * Space separated terms e.g. 'John Adam' means that we will look in selected field for 'John'
     * or 'Adam'.
     * @param fieldName Field name
     * @param searchTerms Search terms
     * @param operator Search operator
     * @return Query instance
     */
    TSelf search(String fieldName, String searchTerms, SearchOperator operator);

    //TBD expr TSelf Search<TValue>(Expression<Func<T, TValue>> propertySelector, string searchTerms, SearchOperator @operator = SearchOperator.Or);

    /**
     * Filter the results from the index using the specified where clause.
     * @param fieldName Field name
     * @param whereClause Where clause
     * @return Query instance
     */
    TSelf whereLucene(String fieldName, String whereClause);

    /**
     * Filter the results from the index using the specified where clause.
     * @param fieldName Field name
     * @param whereClause Where clause
     * @param exact Use exact matcher
     * @return Query instance
     */
    TSelf whereLucene(String fieldName, String whereClause, boolean exact);

    /**
     * Matches fields where the value is between the specified start and end, inclusive
     * @param fieldName Field name
     * @param start Range start
     * @param end Range end
     * @return Query instance
     */
    TSelf whereBetween(String fieldName, Object start, Object end);

    /**
     * Matches fields where the value is between the specified start and end, inclusive
     * @param fieldName Field name
     * @param start Range start
     * @param end Range end
     * @param exact Use exact matcher
     * @return Query instance
     */
    TSelf whereBetween(String fieldName, Object start, Object end, boolean exact);

    //TBD expr TSelf WhereBetween<TValue>(Expression<Func<T, TValue>> propertySelector, TValue start, TValue end, bool exact = false);

    /**
     * Matches fields which ends with the specified value.
     * @param fieldName Field name
     * @param value Value to use
     * @return Query instance
     */
    TSelf whereEndsWith(String fieldName, Object value);

    /**
     * Matches fields which ends with the specified value.
     * @param fieldName Field name
     * @param value Value to use
     * @param exact Use exact matcher
     * @return Query instance
     */
    TSelf whereEndsWith(String fieldName, Object value, boolean exact);

    //TBD expr TSelf WhereEndsWith<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value);

    /**
     * Matches value
     * @param fieldName Field name
     * @param value Value to use
     * @return Query instance
     */
    TSelf whereEquals(String fieldName, Object value);

    /**
     * Matches value
     * @param fieldName Field name
     * @param value Value to use
     * @param exact Use exact matcher
     * @return Query instance
     */
    TSelf whereEquals(String fieldName, Object value, boolean exact);

    /**
     * Matches value
     * @param fieldName Field name
     * @param method Method call
     * @return Query instance
     */
    TSelf whereEquals(String fieldName, MethodCall method);

    /**
     * Matches value
     * @param fieldName Field name
     * @param method Method call
     * @param exact Use exact matcher
     * @return Query instance
     */
    TSelf whereEquals(String fieldName, MethodCall method, boolean exact);

    //TBD expr TSelf WhereEquals<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value, bool exact = false);
    //TBD expr TSelf WhereEquals<TValue>(Expression<Func<T, TValue>> propertySelector, MethodCall value, bool exact = false);

    /**
     * Matches value
     * @param whereParams Where params
     * @return Query instance
     */
    TSelf whereEquals(WhereParams whereParams);

    /**
     * Not matches value
     * @param fieldName Field name
     * @param value Value to use
     * @return Query instance
     */
    TSelf whereNotEquals(String fieldName, Object value);

    /**
     * Not matches value
     * @param fieldName Field name
     * @param value Value to use
     * @param exact Use exact matcher
     * @return Query instance
     */
    TSelf whereNotEquals(String fieldName, Object value, boolean exact);

    /**
     * Not matches value
     * @param fieldName Field name
     * @param method Method call
     * @return Query instance
     */
    TSelf whereNotEquals(String fieldName, MethodCall method);

    /**
     * Not matches value
     * @param fieldName Field name
     * @param method Method call
     * @param exact Use exact matcher
     * @return Query instance
     */
    TSelf whereNotEquals(String fieldName, MethodCall method, boolean exact);

    //TBD expr TSelf WhereNotEquals<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value, bool exact = false);
    //TBD expr TSelf WhereNotEquals<TValue>(Expression<Func<T, TValue>> propertySelector, MethodCall value, bool exact = false);

    /**
     * Not matches value
     * @param whereParams Where params
     * @return Query instance
     */
    TSelf whereNotEquals(WhereParams whereParams);

    /**
     * Matches fields where the value is greater than the specified value
     * @param fieldName Field name
     * @param value Value to use
     * @return Query instance
     */
    TSelf whereGreaterThan(String fieldName, Object value);

    /**
     * Matches fields where the value is greater than the specified value
     * @param fieldName Field name
     * @param value Value to use
     * @param exact Use exact matcher
     * @return Query instance
     */
    TSelf whereGreaterThan(String fieldName, Object value, boolean exact);

    //TBD expr TSelf WhereGreaterThan<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value, bool exact = false);

    /**
     * Matches fields where the value is greater than or equal to the specified value
     * @param fieldName Field name
     * @param value Value to use
     * @return Query instance
     */
    TSelf whereGreaterThanOrEqual(String fieldName, Object value);

    /**
     * Matches fields where the value is greater than or equal to the specified value
     * @param fieldName Field name
     * @param value Value to use
     * @param exact Use exact matcher
     * @return Query instance
     */
    TSelf whereGreaterThanOrEqual(String fieldName, Object value, boolean exact);

    //TBD expr TSelf WhereGreaterThanOrEqual<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value, bool exact = false);

    /**
     * Check that the field has one of the specified values
     * @param fieldName Field name
     * @param values Values to use
     * @return Query instance
     */
    TSelf whereIn(String fieldName, Collection<Object> values);

    /**
     * Check that the field has one of the specified values
     * @param fieldName Field name
     * @param values Values to use
     * @param exact Use exact matcher
     * @return Query instance
     */
    TSelf whereIn(String fieldName, Collection<Object> values, boolean exact);

    //TBD expr TSelf WhereIn<TValue>(Expression<Func<T, TValue>> propertySelector, IEnumerable<TValue> values, bool exact = false);

    /**
     * Matches fields where the value is less than the specified value
     * @param fieldName Field name
     * @param value Value to use
     * @return Query instance
     */
    TSelf whereLessThan(String fieldName, Object value);

    /**
     * Matches fields where the value is less than the specified value
     * @param fieldName Field name
     * @param value Value to use
     * @param exact Use exact matcher
     * @return Query instance
     */
    TSelf whereLessThan(String fieldName, Object value, boolean exact);

    //TBD expr TSelf WhereLessThan<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value, bool exact = false);

    /**
     *  Matches fields where the value is less than or equal to the specified value
     *  @param fieldName Field name
     *  @param value Value to use
     *  @return Query instance
     */
    TSelf whereLessThanOrEqual(String fieldName, Object value);

    /**
     *  Matches fields where the value is less than or equal to the specified value
     *  @param fieldName Field name
     *  @param value Value to use
     *  @param exact Use exact matcher
     *  @return Query instance
     */
    TSelf whereLessThanOrEqual(String fieldName, Object value, boolean exact);

    //TBD expr TSelf WhereLessThanOrEqual<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value, bool exact = false);

    /**
     * Matches fields which starts with the specified value.
     * @param fieldName Name of the field.
     * @param value The value.
     * @return Query instance
     */
    TSelf whereStartsWith(String fieldName, Object value);

    /**
     * Matches fields which starts with the specified value.
     * @param fieldName Name of the field.
     * @param value The value.
     * @param exact Use exact matcher
     * @return Query instance
     */
    TSelf whereStartsWith(String fieldName, Object value, boolean exact);

    //TBD expr TSelf WhereStartsWith<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value);

    //TBD expr TSelf WhereExists<TValue>(Expression<Func<T, TValue>> propertySelector);

    /**
     * Check if the given field exists
     * @param fieldName Field name
     * @return Query instance
     */
    TSelf whereExists(String fieldName);

    //TBD expr TSelf WhereRegex<TValue>(Expression<Func<T, TValue>> propertySelector, string pattern);

    /**
     * Checks value of a given field against supplied regular expression pattern
     * @param fieldName Field name
     * @param pattern Regexp pattern
     * @return Query instance
     */
    TSelf whereRegex(String fieldName, String pattern);

    //TBD expr TSelf WithinRadiusOf<TValue>(Expression<Func<T, TValue>> propertySelector, double radius, double latitude, double longitude, SpatialUnits? radiusUnits = null, double distanceErrorPct = Constants.Documents.Indexing.Spatial.DefaultDistanceErrorPct);

    /**
     * Filter matches to be inside the specified radius
     * @param fieldName Spatial field name.
     * @param radius Radius (measured in units passed to radiusUnits parameter) in which matches should be found.
     * @param latitude Latitude pointing to a circle center.
     * @param longitude Longitude pointing to a circle center.
     * @return Query instance
     */
    TSelf withinRadiusOf(String fieldName, double radius, double latitude, double longitude);

    /**
     * Filter matches to be inside the specified radius
     * @param fieldName Spatial field name.
     * @param radius Radius (measured in units passed to radiusUnits parameter) in which matches should be found.
     * @param latitude Latitude pointing to a circle center.
     * @param longitude Longitude pointing to a circle center.
     * @param radiusUnits Units that will be used to measure distances (Kilometers, Miles).
     * @return Query instance
     */
    TSelf withinRadiusOf(String fieldName, double radius, double latitude, double longitude, SpatialUnits radiusUnits);

    /**
     * Filter matches to be inside the specified radius
     * @param fieldName Spatial field name.
     * @param radius Radius (measured in units passed to radiusUnits parameter) in which matches should be found.
     * @param latitude Latitude pointing to a circle center.
     * @param longitude Longitude pointing to a circle center.
     * @param radiusUnits Units that will be used to measure distances (Kilometers, Miles).
     * @param distanceErrorPct Distance error percent
     * @return Query instance
     */
    TSelf withinRadiusOf(String fieldName, double radius, double latitude, double longitude, SpatialUnits radiusUnits, double distanceErrorPct);


    //TBD expr TSelf RelatesToShape<TValue>(Expression<Func<T, TValue>> propertySelector, string shapeWkt, SpatialRelation relation, double distanceErrorPct = Constants.Documents.Indexing.Spatial.DefaultDistanceErrorPct);

    /**
     * Filter matches based on a given shape - only documents with the shape defined in fieldName that
     * have a relation rel with the given shapeWkt will be returned
     * @param fieldName Spatial field name.
     * @param shapeWkt WKT formatted shape
     * @param relation Spatial relation to check (Within, Contains, Disjoint, Intersects, Nearby)
     * @return Query instance
     */
    TSelf relatesToShape(String fieldName, String shapeWkt, SpatialRelation relation);

    /**
     * Filter matches based on a given shape - only documents with the shape defined in fieldName that
     * have a relation rel with the given shapeWkt will be returned
     * @param fieldName Spatial field name.
     * @param shapeWkt WKT formatted shape
     * @param relation Spatial relation to check (Within, Contains, Disjoint, Intersects, Nearby)
     * @param distanceErrorPct The allowed error percentage. By default: 0.025
     * @return Query instance
     */
    TSelf relatesToShape(String fieldName, String shapeWkt, SpatialRelation relation, double distanceErrorPct);

    /**
     * Filter matches based on a given shape - only documents with the shape defined in fieldName that
     * have a relation rel with the given shapeWkt will be returned
     * @param fieldName Spatial field name.
     * @param shapeWkt WKT formatted shape
     * @param relation Spatial relation to check (Within, Contains, Disjoint, Intersects, Nearby)
     * @param units SpatialUnits
     * @param distanceErrorPct The allowed error percentage. By default: 0.025
     * @return Query instance
     */
    TSelf relatesToShape(String fieldName, String shapeWkt, SpatialRelation relation, SpatialUnits units, double distanceErrorPct);

    //TBD expr IDocumentQuery<T> Spatial(Expression<Func<T, object>> path, Func<SpatialCriteriaFactory, SpatialCriteria> clause);

    /**
     * Ability to use one factory to determine spatial shape that will be used in query.
     * @param fieldName Field name
     * @param clause Spatial criteria factory
     * @return Query instance
     */
    IDocumentQuery<T> spatial(String fieldName, Function<SpatialCriteriaFactory, SpatialCriteria> clause);

    IDocumentQuery<T> spatial(DynamicSpatialField field, Function<SpatialCriteriaFactory, SpatialCriteria> clause);

    //TBD expr IDocumentQuery<T> spatial(Function<SpatialDynamicFieldFactory<T>, DynamicSpatialField> field, Function<SpatialCriteriaFactory, SpatialCriteria> clause);

    IDocumentQuery<T> moreLikeThis(MoreLikeThisBase moreLikeThis);

}
