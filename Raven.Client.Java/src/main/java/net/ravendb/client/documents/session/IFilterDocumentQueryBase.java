package net.ravendb.client.documents.session;

import net.ravendb.client.documents.indexes.spatial.SpatialRelation;
import net.ravendb.client.documents.indexes.spatial.SpatialUnits;
import net.ravendb.client.documents.queries.SearchOperator;

import java.util.Collection;

public interface IFilterDocumentQueryBase<T, TSelf extends IDocumentQueryBase<T, TSelf>> extends IQueryBase<T, TSelf> {

    /**
     * Negate the next operation
     */
    TSelf not();

    /**
     *  Add an AND to the query
     */
    TSelf andAlso();

    /**
     * Simplified method for closing a clause within the query
     */
    TSelf closeSubclause();

    /**
     * Performs a query matching ALL of the provided values against the given field (AND)
     */
    TSelf containsAll(String fieldName, Collection<Object> values);

    //TBD TSelf ContainsAll<TValue>(Expression<Func<T, TValue>> propertySelector, IEnumerable<TValue> values);

    /**
     * Performs a query matching ANY of the provided values against the given field (OR)
     */
    TSelf containsAny(String fieldName, Collection<Object> values);

    //TBD TSelf ContainsAny<TValue>(Expression<Func<T, TValue>> propertySelector, IEnumerable<TValue> values);

    /**
     * Negate the next operation
     */
    void negateNext();

    /**
     *  Simplified method for opening a new clause within the query
     */
    TSelf openSubclause();

    /**
     * Add an OR to the query
     */
    TSelf orElse();

    /**
     * Perform a search for documents which fields that match the searchTerms.
     * If there is more than a single term, each of them will be checked independently.
     *
     * Space separated terms e.g. 'John Adam' means that we will look in selected field for 'John'
     * or 'Adam'.
     */
    TSelf search(String fieldName, String searchTerms);

    /**
     * Perform a search for documents which fields that match the searchTerms.
     * If there is more than a single term, each of them will be checked independently.
     *
     * Space separated terms e.g. 'John Adam' means that we will look in selected field for 'John'
     * or 'Adam'.
     */
    TSelf search(String fieldName, String searchTerms, SearchOperator operator);

    //TBD TSelf Search<TValue>(Expression<Func<T, TValue>> propertySelector, string searchTerms, SearchOperator @operator = SearchOperator.Or);

    /**
     * Filter the results from the index using the specified where clause.
     */
    TSelf whereLucene(String fieldName, String whereClause);

    /**
     * Matches fields where the value is between the specified start and end, exclusive
     */
    TSelf whereBetween(String fieldName, Object start, Object end);

    /**
     * Matches fields where the value is between the specified start and end, exclusive
     */
    TSelf whereBetween(String fieldName, Object start, Object end, boolean exact);

    //TBD TSelf WhereBetween<TValue>(Expression<Func<T, TValue>> propertySelector, TValue start, TValue end, bool exact = false);

    /**
     * Matches fields which ends with the specified value.
     */
    TSelf whereEndsWith(String fieldName, Object value);

    //TBD TSelf WhereEndsWith<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value);

    /**
     * Matches value
     */
    TSelf whereEquals(String fieldName, Object value);

    /**
     * Matches value
     */
    TSelf whereEquals(String fieldName, Object value, boolean exact);

    //TBD TSelf WhereEquals<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value, bool exact = false);

    /**
     * Matches value
     */
    TSelf whereEquals(WhereParams whereParams);

    /**
     * Not matches value
     */
    TSelf whereNotEquals(String fieldName, Object value);

    /**
     * Not matches value
     */
    TSelf whereNotEquals(String fieldName, Object value, boolean exact);

    //TBD TSelf WhereNotEquals<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value, bool exact = false);

    /**
     * Not matches value
     */
    TSelf whereNotEquals(WhereParams whereParams);

    /**
     * Matches fields where the value is greater than the specified value
     */
    TSelf whereGreaterThan(String fieldName, Object value);

    /**
     * Matches fields where the value is greater than the specified value
     */
    TSelf whereGreaterThan(String fieldName, Object value, boolean exact);

    //TBD  TSelf WhereGreaterThan<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value, bool exact = false);

    /**
     * Matches fields where the value is greater than or equal to the specified value
     */
    TSelf whereGreaterThanOrEqual(String fieldName, Object value);

    /**
     * Matches fields where the value is greater than or equal to the specified value
     */
    TSelf whereGreaterThanOrEqual(String fieldName, Object value, boolean exact);

    //TBD TSelf WhereGreaterThanOrEqual<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value, bool exact = false);

    /**
     * Check that the field has one of the specified values
     */
    TSelf whereIn(String fieldName, Collection<Object> values);

    /**
     * Check that the field has one of the specified values
     */
    TSelf whereIn(String fieldName, Collection<Object> values, boolean exact);

    //TBD TSelf WhereIn<TValue>(Expression<Func<T, TValue>> propertySelector, IEnumerable<TValue> values, bool exact = false);

    /**
     * Matches fields where the value is less than the specified value
     */
    TSelf whereLessThan(String fieldName, Object value);

    /**
     * Matches fields where the value is less than the specified value
     */
    TSelf whereLessThan(String fieldName, Object value, boolean exact);

    //TBD TSelf WhereLessThan<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value, bool exact = false);

    /**
     *  Matches fields where the value is less than or equal to the specified value
     */
    TSelf whereLessThanOrEqual(String fieldName, Object value);

    /**
     *  Matches fields where the value is less than or equal to the specified value
     */
    TSelf whereLessThanOrEqual(String fieldName, Object value, boolean exact);

    //TBD TSelf WhereLessThanOrEqual<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value, bool exact = false);

    /**
     * Matches fields which starts with the specified value.
     * @param fieldName Name of the field.
     * @param value The value.
     */
    TSelf whereStartsWith(String fieldName, Object value);

    //TBD TSelf WhereStartsWith<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value);

    //TBD TSelf WhereExists<TValue>(Expression<Func<T, TValue>> propertySelector);

    /**
     * Check if the given field exists
     */
    TSelf whereExists(String fieldName);


    //TBD TSelf WithinRadiusOf<TValue>(Expression<Func<T, TValue>> propertySelector, double radius, double latitude, double longitude, SpatialUnits? radiusUnits = null, double distanceErrorPct = Constants.Documents.Indexing.Spatial.DefaultDistanceErrorPct);

    /**
     * Filter matches to be inside the specified radius
     * @param fieldName Spatial field name.
     * @param radius Radius (measured in units passed to radiusUnits parameter) in which matches should be found.
     * @param latitude Latitude pointing to a circle center.
     * @param longitude Longitude pointing to a circle center.
     */
    TSelf withinRadiusOf(String fieldName, double radius, double latitude, double longitude);

    /**
     * Filter matches to be inside the specified radius
     * @param fieldName Spatial field name.
     * @param radius Radius (measured in units passed to radiusUnits parameter) in which matches should be found.
     * @param latitude Latitude pointing to a circle center.
     * @param longitude Longitude pointing to a circle center.
     * @param radiusUnits Units that will be used to measure distances (Kilometers, Miles).
     */
    TSelf withinRadiusOf(String fieldName, double radius, double latitude, double longitude, SpatialUnits radiusUnits);

    /**
     * Filter matches to be inside the specified radius
     * @param fieldName Spatial field name.
     * @param radius Radius (measured in units passed to radiusUnits parameter) in which matches should be found.
     * @param latitude Latitude pointing to a circle center.
     * @param longitude Longitude pointing to a circle center.
     * @param radiusUnits Units that will be used to measure distances (Kilometers, Miles).
     */
    TSelf withinRadiusOf(String fieldName, double radius, double latitude, double longitude, SpatialUnits radiusUnits, double distanceErrorPct);


    //TBD TSelf RelatesToShape<TValue>(Expression<Func<T, TValue>> propertySelector, string shapeWKT, SpatialRelation relation, double distanceErrorPct = Constants.Documents.Indexing.Spatial.DefaultDistanceErrorPct);

    /**
     * Filter matches based on a given shape - only documents with the shape defined in fieldName that
     * have a relation rel with the given shapeWKT will be returned
     * @param fieldName Spatial field name.
     * @param shapeWKT WKT formatted shape
     * @param relation Spatial relation to check (Within, Contains, Disjoint, Intersects, Nearby)
     */
    TSelf relatesToShape(String fieldName, String shapeWKT, SpatialRelation relation);

    /**
     * Filter matches based on a given shape - only documents with the shape defined in fieldName that
     * have a relation rel with the given shapeWKT will be returned
     * @param fieldName Spatial field name.
     * @param shapeWKT WKT formatted shape
     * @param relation Spatial relation to check (Within, Contains, Disjoint, Intersects, Nearby)
     * @param distanceErrorPct The allowed error percentage. By default: 0.025
     */
    TSelf relatesToShape(String fieldName, String shapeWKT, SpatialRelation relation, double distanceErrorPct);

    TSelf cmpXChg(String key, T value);

}
