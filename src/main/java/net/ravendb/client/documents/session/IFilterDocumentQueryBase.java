package net.ravendb.client.documents.session;

import net.ravendb.client.documents.indexes.spatial.SpatialRelation;
import net.ravendb.client.documents.indexes.spatial.SpatialUnits;
import net.ravendb.client.documents.queries.SearchOperator;
import net.ravendb.client.documents.queries.moreLikeThis.MoreLikeThisBase;
import net.ravendb.client.documents.queries.spatial.DynamicSpatialField;
import net.ravendb.client.documents.queries.spatial.SpatialCriteria;
import net.ravendb.client.documents.queries.spatial.SpatialCriteriaFactory;
import net.ravendb.client.DocumentationUrls;
import java.util.Collection;
import java.util.function.Function;

public interface IFilterDocumentQueryBase<T, TSelf extends IDocumentQueryBase<T, TSelf>> extends IQueryBase<T, TSelf> {

    /**
     * Negates the next subclause.
     * {@inheritDoc}
     * @see DocumentationUrls.Session.Querying#HowToUseNotOperator
     */
    TSelf not();

    /**
     * {@inheritDoc}
     * @see #andAlso(boolean)
     */
    TSelf andAlso();

    /**
     * Adds an 'AND' statement to the query.
     * @param wrapPreviousQueryClauses Wraps preceding clauses using parentheses.
     */
    TSelf andAlso(boolean wrapPreviousQueryClauses);

    /**
     * Closes previously opened subclause.
     */
    TSelf closeSubclause();

    /**
     * Matches documents with chosen field containing all provided values.
     * @param fieldName Name of the field to match values against.
     * @param values Values that the chosen field has to contain.
     */
    TSelf containsAll(String fieldName, Collection<?> values);

    //TBD expr TSelf ContainsAll<TValue>(Expression<Func<T, TValue>> propertySelector, IEnumerable<TValue> values);

    /**
     * Matches documents where the specified field contains any of the provided values.
     *
     * @param fieldName the name of the field to match values against.
     * @param values    the values where at least one must be contained in the {@code fieldName} value
     *                  for the document to match.
     */
    TSelf containsAny(String fieldName, Collection<?> values);

    //TBD expr TSelf ContainsAny<TValue>(Expression<Func<T, TValue>> propertySelector, IEnumerable<TValue> values);

    /**
     * Negates the next subclause.
     */
    TSelf negateNext();

    /**
     *  Opens a new subclause.
     */
    TSelf openSubclause();

    /**
     * Adds an 'OR' statement to the query.
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
     * Matches documents with value of chosen field matching searched terms.
     * @param fieldName Name of the field that searched terms will be checked against.
     * @param searchTerms Space separated terms to search. If there is more than a single term, each of them will be checked independently.
     * @param operator Operator to be used for relationship between terms. Default: Or.
     */
    TSelf search(String fieldName, String searchTerms, SearchOperator operator);

    //TBD expr TSelf Search<TValue>(Expression<Func<T, TValue>> propertySelector, string searchTerms, SearchOperator @operator = SearchOperator.Or);

    /**
     * {@inheritDoc}
     * @see #whereLucene(String, String, boolean)
     */
    TSelf whereLucene(String fieldName, String whereClause);

    /**
     * Matches documents with chosen field value meeting criteria of specified predicate in Lucene syntax.
     * @param fieldName Name of the field to get value from
     * @param whereClause Predicate in Lucene syntax.
     * @param exact Specifies if comparison is case sensitive.
     * {@inheritDoc}
     * @see DocumentationUrls.Session.Querying#HowToUseLucene
     */
    TSelf whereLucene(String fieldName, String whereClause, boolean exact);

    /**
     * Matches fields where the value is between the specified start and end, inclusive
     * @param fieldName Field name
     * @param start Range start
     * @param end Range end
     */
    TSelf whereBetween(String fieldName, Object start, Object end);

    /**
     * Matches documents with value of the chosen field between the specified start and end value, inclusive.
     * @param fieldName Name of the field to get value from.
     * @param start Start value.
     * @param end End value.
     * @param exact Specifies if comparison is case sensitive. Default: false.
     */
    TSelf whereBetween(String fieldName, Object start, Object end, boolean exact);

    //TBD expr TSelf WhereBetween<TValue>(Expression<Func<T, TValue>> propertySelector, TValue start, TValue end, bool exact = false);

    /**
     * {@inheritDoc}
     * @see #whereEndsWith(String, Object, boolean)
     */
    TSelf whereEndsWith(String fieldName, Object value);

    /**
     * Matches documents with value of the chosen field ending with the specified value.
     * @param fieldName Name of the field to get value from.
     * @param value Value that the <code>fieldName</code> value has to end with in order to match the document.
     * @param exact Specifies if comparison is case sensitive.
     */
    TSelf whereEndsWith(String fieldName, Object value, boolean exact);

    //TBD expr TSelf WhereEndsWith<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value);

    /**
     * Matches documents with value of the chosen field equal to the specified value.
     * @param fieldName Name of the field to get value from.
     * @param value Value to compare with <code>fieldName</code> value.
     */
    TSelf whereEquals(String fieldName, Object value);

    /**
     * {@inheritDoc}
     * @see #whereEquals(String, Object)
     * @param exact Specifies if comparison is case sensitive. Default: false.
     */
    TSelf whereEquals(String fieldName, Object value, boolean exact);

    /**
     * Matches documents with value of the chosen field equal to the evaluated provided expression.
     * @param fieldName Name of the field to get value from.
     * @param method Expression to evaluate.
     */
    TSelf whereEquals(String fieldName, MethodCall method);

    /**
     * {@inheritDoc}
     * @see #whereEquals(String, MethodCall)
     * @param exact Specifies if comparison is case sensitive. Default: false.
     */
    TSelf whereEquals(String fieldName, MethodCall method, boolean exact);

    //TBD expr TSelf WhereEquals<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value, bool exact = false);
    //TBD expr TSelf WhereEquals<TValue>(Expression<Func<T, TValue>> propertySelector, MethodCall value, bool exact = false);

    /**
     * Matches documents that match specified <code>whereParams</code>.
     * @param whereParams WhereParams containing query parameters.
     */
    TSelf whereEquals(WhereParams whereParams);

    /**
     * Matches documents with value of the chosen field different than the specified value.
     * @param fieldName Name of the field to get value from.
     * @param value Value to compare with <code>fieldName</code> value.
     */
    TSelf whereNotEquals(String fieldName, Object value);

    /**
     * {@inheritDoc}
     * @see #whereNotEquals(String, Object)
     * @param exact Specifies if comparison is case sensitive. Default: false.
     */
    TSelf whereNotEquals(String fieldName, Object value, boolean exact);

    /**
     * Matches documents with value of the chosen field different than the evaluated provided expression.
     * @param fieldName Name of the field to get value from.
     * @param method Expression to evaluate.
     */
    TSelf whereNotEquals(String fieldName, MethodCall method);

    /**
     * {@inheritDoc}
     * @see #whereNotEquals(String, MethodCall)
     * @param exact Specifies if comparison is case sensitive. Default: false.
     */
    TSelf whereNotEquals(String fieldName, MethodCall method, boolean exact);

    //TBD expr TSelf WhereNotEquals<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value, bool exact = false);
    //TBD expr TSelf WhereNotEquals<TValue>(Expression<Func<T, TValue>> propertySelector, MethodCall value, bool exact = false);

    /**
     * Matches documents that do not match specified <code>whereParams</code>.
     * @param whereParams WhereParams containing query parameters.
     */
    TSelf whereNotEquals(WhereParams whereParams);

    /**
     * Matches documents with value of the chosen field greater than the specified value.
     * @param fieldName Field name
     * @param value Value to compare with <code>fieldName</code> value.
     */
    TSelf whereGreaterThan(String fieldName, Object value);

    /**
     * {@inheritDoc}
     * @see #whereGreaterThan(String, Object)
     * @param exact Specifies if comparison is case sensitive. Default: false.
     */
    TSelf whereGreaterThan(String fieldName, Object value, boolean exact);

    //TBD expr TSelf WhereGreaterThan<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value, bool exact = false);

    /**
     * Matches documents with value of the chosen field greater than or equal to the specified value.
     * @param fieldName Name of the field to get value from.
     * @param value Value to compare with <code>fieldName</code> value.
     */
    TSelf whereGreaterThanOrEqual(String fieldName, Object value);

    /**
     * {@inheritDoc}
     * @see #whereGreaterThanOrEqual(String, Object)
     * @param exact Specifies if comparison is case sensitive. Default: false.
     */
    TSelf whereGreaterThanOrEqual(String fieldName, Object value, boolean exact);

    //TBD expr TSelf WhereGreaterThanOrEqual<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value, bool exact = false);

    /**
     * Matches documents with value of the chosen field contained in provided values.
     * @param fieldName Name of the field to get value from.
     * @param values Values that have to contain <code>fieldName</code> value for the document to match.
     */
    TSelf whereIn(String fieldName, Collection<?> values);

    /**
     * {@inheritDoc}
     * @see #whereIn(String, Collection)
     * @param exact Specifies if comparison is case sensitive. Default: false.
     */
    TSelf whereIn(String fieldName, Collection<?> values, boolean exact);

    //TBD expr TSelf WhereIn<TValue>(Expression<Func<T, TValue>> propertySelector, IEnumerable<TValue> values, bool exact = false);

    /**
     * Matches documents with value of the chosen field less than the specified value.
     * @param fieldName Name of the field to get value from.
     * @param value Value to compare with <code>fieldName</code> value.
     */
    TSelf whereLessThan(String fieldName, Object value);

    /**
     * {@inheritDoc}
     * @see #whereLessThan(String, Object)
     * @param exact Specifies if comparison is case sensitive. Default: false.
     */
    TSelf whereLessThan(String fieldName, Object value, boolean exact);

    //TBD expr TSelf WhereLessThan<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value, bool exact = false);

    /**
     *  Matches documents with value of the chosen field less than or equal to the specified value.
     *  @param fieldName Name of the field to get value from.
     *  @param value Value to compare with <code>fieldName</code> value.
     */
    TSelf whereLessThanOrEqual(String fieldName, Object value);

    /**
     * {@inheritDoc}
     * @see #whereLessThanOrEqual(String, Object)
     * @param exact Specifies if comparison is case sensitive. Default: false.
     */
    TSelf whereLessThanOrEqual(String fieldName, Object value, boolean exact);

    //TBD expr TSelf WhereLessThanOrEqual<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value, bool exact = false);

    /**
     * Matches documents with value of the chosen field starting with the specified value.
     * @param fieldName Name of the field to get value from.
     * @param value Value that the <code>fieldName</code> value has to start with in order to match the document.
     */
    TSelf whereStartsWith(String fieldName, Object value);

    /**
     * {@inheritDoc}
     * @see #whereStartsWith(String, Object)
     * @param exact Specifies if comparison is case sensitive. Default: false.
     */
    TSelf whereStartsWith(String fieldName, Object value, boolean exact);

    //TBD expr TSelf WhereStartsWith<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value);

    //TBD expr TSelf WhereExists<TValue>(Expression<Func<T, TValue>> propertySelector);

    /**
     * Matches documents with existing given field.
     * @param fieldName Name of the field to check the existence of.
     */
    TSelf whereExists(String fieldName);

    //TBD expr TSelf WhereRegex<TValue>(Expression<Func<T, TValue>> propertySelector, string pattern);

    /**
     * Matches documents with the value of a given field matched by provided regular expression.
     * @param fieldName Name of the field to get value from.
     * @param pattern Regular expression pattern to check <code>fieldName</code> value against.
     */
    TSelf whereRegex(String fieldName, String pattern);

    //TBD expr TSelf WithinRadiusOf<TValue>(Expression<Func<T, TValue>> propertySelector, double radius, double latitude, double longitude, SpatialUnits? radiusUnits = null, double distanceErrorPct = Constants.Documents.Indexing.Spatial.DefaultDistanceErrorPct);

    /**
     * Matches documents with the value of specified field in radius of given spatial circle.
     * @param fieldName Spatial field name.
     * @param radius Radius (measured in units passed to radiusUnits parameter) in which matches should be found.
     * @param latitude Latitude of a circle center.
     * @param longitude Longitude of a circle center.
     */
    TSelf withinRadiusOf(String fieldName, double radius, double latitude, double longitude);

    /**
     * Matches documents with the value of specified field in radius of given spatial circle.
     * @param fieldName Spatial field name.
     * @param radius Radius (measured in units passed to radiusUnits parameter) in which matches should be found.
     * @param latitude Latitude of a circle center.
     * @param longitude Longitude of a circle center.
     * @param radiusUnits Units that the radius was measured in (kilometers or miles).
     */
    TSelf withinRadiusOf(String fieldName, double radius, double latitude, double longitude, SpatialUnits radiusUnits);

    /**
     * Matches documents with the value of specified field in radius of given spatial circle.
     * @param fieldName Spatial field name.
     * @param radius Radius (measured in units passed to radiusUnits parameter) in which matches should be found.
     * @param latitude Latitude of a circle center.
     * @param longitude Longitude of a circle center.
     * @param radiusUnits Units that the radius was measured in (kilometers or miles).
     * @param distanceErrorPct Allowed error percentage. Default: 0.025.
     * @return Query instance
     */
    TSelf withinRadiusOf(String fieldName, double radius, double latitude, double longitude, SpatialUnits radiusUnits, double distanceErrorPct);


    //TBD expr TSelf RelatesToShape<TValue>(Expression<Func<T, TValue>> propertySelector, string shapeWkt, SpatialRelation relation, double distanceErrorPct = Constants.Documents.Indexing.Spatial.DefaultDistanceErrorPct);

    /**
     * Matches documents with the value of specified field in relation with the provided WKT shape.
     * @param fieldName Spatial field name to get the value from.
     * @param shapeWkt String representing the WKT shape.
     * @param relation Spatial relation to check (Within, Contains, Disjoint, Intersects).
     */
    TSelf relatesToShape(String fieldName, String shapeWkt, SpatialRelation relation);

    /**
     * {@inheritDoc}
     * @see #relatesToShape(String, String, SpatialRelation)
     * @param distanceErrorPct Allowed error percentage. Default: 0.025.
     */
    TSelf relatesToShape(String fieldName, String shapeWkt, SpatialRelation relation, double distanceErrorPct);

    /**
     * Matches documents with the value of specified field in relation with the provided WKT shape.
     * @param fieldName Spatial field name to get the value from.
     * @param shapeWkt String representing the WKT shape.
     * @param relation Spatial relation to check (Within, Contains, Disjoint, Intersects).
     * @param units Units to be used (kilometers or miles).
     * @param distanceErrorPct Allowed error percentage. Default: 0.025.
     * @return Query instance
     */
    TSelf relatesToShape(String fieldName, String shapeWkt, SpatialRelation relation, SpatialUnits units, double distanceErrorPct);

    //TBD expr IDocumentQuery<T> Spatial(Expression<Func<T, object>> path, Func<SpatialCriteriaFactory, SpatialCriteria> clause);

    /**
     * Matches documents based on provided spatial criteria created by factory.
     * @param fieldName Name of spatial field to get value from.
     * @param clause Function creating spatial criteria.
     */
    IDocumentQuery<T> spatial(String fieldName, Function<SpatialCriteriaFactory, SpatialCriteria> clause);
    /**
     * Matches documents based on provided spatial criteria created by factory.
     * @param field Dynamic spatial field to get value from.
     * @param clause Function creating spatial criteria.
     */
    IDocumentQuery<T> spatial(DynamicSpatialField field, Function<SpatialCriteriaFactory, SpatialCriteria> clause);

    //TBD expr IDocumentQuery<T> spatial(Function<SpatialDynamicFieldFactory<T>, DynamicSpatialField> field, Function<SpatialCriteriaFactory, SpatialCriteria> clause);
    /**
     * {@inheritDoc}
     * @see MoreLikeThisBase
     * @param moreLikeThis Specified MoreLikeThisQuery.
     */
    IDocumentQuery<T> moreLikeThis(MoreLikeThisBase moreLikeThis);

}
