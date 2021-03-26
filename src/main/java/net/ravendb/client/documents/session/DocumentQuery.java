package net.ravendb.client.documents.session;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import net.ravendb.client.Constants;
import net.ravendb.client.Parameters;
import net.ravendb.client.documents.queries.*;
import net.ravendb.client.documents.session.loaders.IQueryIncludeBuilder;
import net.ravendb.client.documents.session.loaders.QueryIncludeBuilder;
import net.ravendb.client.documents.session.tokens.DeclareToken;
import net.ravendb.client.documents.session.tokens.FieldsToFetchToken;
import net.ravendb.client.documents.session.tokens.LoadToken;
import net.ravendb.client.primitives.Reference;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;

public class DocumentQuery<T> extends AbstractDocumentQuery<T, DocumentQuery<T>> implements IDocumentQuery<T> {

    public DocumentQuery(Class<T> clazz, InMemoryDocumentSessionOperations session, String indexName,
                         String collectionName, boolean isGroupBy) {
        this(clazz, session, indexName, collectionName, isGroupBy, null, null, null, null);
    }

    public DocumentQuery(Class<T> clazz, InMemoryDocumentSessionOperations session, String indexName,
                         String collectionName, boolean isGroupBy,
                         List<DeclareToken> declareTokens, List<LoadToken> loadTokens, String fromAlias) {
        this(clazz, session, indexName, collectionName, isGroupBy, declareTokens, loadTokens, fromAlias, null);
    }

    public DocumentQuery(Class<T> clazz, InMemoryDocumentSessionOperations session, String indexName,
                         String collectionName, boolean isGroupBy, List<DeclareToken> declareTokens,
                         List<LoadToken> loadTokens, String fromAlias, Boolean isProjectInto) {
        super(clazz, session, indexName, collectionName, isGroupBy, declareTokens, loadTokens, fromAlias, isProjectInto);
    }

    @Override
    public <TProjection> IDocumentQuery<TProjection> selectFields(Class<TProjection> projectionClass) {

        try {
            PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(projectionClass).getPropertyDescriptors();

            String[] projections = Arrays.stream(propertyDescriptors)
                    .filter(x -> !Object.class.equals(x.getReadMethod().getDeclaringClass())) // ignore class field etc,
                    .map(x -> x.getName())
                    .toArray(String[]::new);

            String[] fields = Arrays.stream(propertyDescriptors)
                    .filter(x -> !Object.class.equals(x.getReadMethod().getDeclaringClass())) // ignore class field etc,
                    .map(x -> x.getName())
                    .toArray(String[]::new);


            QueryData queryData = new QueryData(fields, projections);
            queryData.setProjectInto(true);
            return selectFields(projectionClass, queryData);
        } catch (IntrospectionException e) {
            throw new RuntimeException("Unable to project to class: " + projectionClass.getName() + e.getMessage(), e);
        }
    }


    @Override
    public IDocumentQuery<T> distinct() {
        _distinct();
        return this;
    }

    @Override
    public IDocumentQuery<T> orderByScore() {
        _orderByScore();
        return this;
    }

    @Override
    public IDocumentQuery<T> orderByScoreDescending() {
        _orderByScoreDescending();
        return this;
    }


    @Override
    public <TProjection> IDocumentQuery<TProjection> selectFields(Class<TProjection> projectionClass, String... fields) {
        QueryData queryData = new QueryData(fields, fields);
        queryData.setProjectInto(true);

        IDocumentQuery<TProjection> selectFields = selectFields(projectionClass, queryData);
        return selectFields;
    }

    @Override
    public <TProjection> IDocumentQuery<TProjection> selectFields(Class<TProjection> projectionClass, QueryData queryData) {
        queryData.setProjectInto(true);
        return createDocumentQueryInternal(projectionClass, queryData);
    }

    @Override
    public IDocumentQuery<T> waitForNonStaleResults() {
        _waitForNonStaleResults(null);
        return this;
    }

    @Override
    public IDocumentQuery<T> waitForNonStaleResults(Duration waitTimeout) {
        _waitForNonStaleResults(waitTimeout);
        return this;
    }

    @Override
    public IDocumentQuery<T> addParameter(String name, Object value) {
        _addParameter(name, value);
        return this;
    }

    @Override
    public IDocumentQuery<T> addOrder(String fieldName, boolean descending) {
        return addOrder(fieldName, descending, OrderingType.STRING);
    }

    @Override
    public IDocumentQuery<T> addOrder(String fieldName, boolean descending, OrderingType ordering) {
        if (descending) {
            orderByDescending(fieldName, ordering);
        } else {
            orderBy(fieldName, ordering);
        }
        return this;
    }

    //TBD expr public IDocumentQuery<T> AddOrder<TValue>(Expression<Func<T, TValue>> propertySelector, bool descending, OrderingType ordering)


    public IDocumentQuery<T> openSubclause() {
        _openSubclause();
        return this;
    }

    @Override
    public IDocumentQuery<T> closeSubclause() {
        _closeSubclause();
        return this;
    }

    @Override
    public IDocumentQuery<T> negateNext() {
        _negateNext();
        return this;
    }

    @Override
    public IDocumentQuery<T> search(String fieldName, String searchTerms) {
        _search(fieldName, searchTerms);
        return this;
    }

    @Override
    public IDocumentQuery<T> search(String fieldName, String searchTerms, SearchOperator operator) {
        _search(fieldName, searchTerms, operator);
        return this;
    }

    //TBD expr public IDocumentQuery<T> Search<TValue>(Expression<Func<T, TValue>> propertySelector, string searchTerms, SearchOperator @operator)

    @Override
    public IDocumentQuery<T> intersect() {
        _intersect();
        return this;
    }

    @Override
    public IDocumentQuery<T> containsAny(String fieldName, Collection<?> values) {
        _containsAny(fieldName, values);
        return this;
    }

    //TBD expr public IDocumentQuery<T> ContainsAny<TValue>(Expression<Func<T, TValue>> propertySelector, IEnumerable<TValue> values)

    @Override
    public IDocumentQuery<T> containsAll(String fieldName, Collection<?> values) {
        _containsAll(fieldName, values);
        return this;
    }

    //TBD expr public IDocumentQuery<T> ContainsAll<TValue>(Expression<Func<T, TValue>> propertySelector, IEnumerable<TValue> values)

    @Override
    public IDocumentQuery<T> statistics(Reference<QueryStatistics> stats) {
        _statistics(stats);
        return this;
    }

    @Override
    public IDocumentQuery<T> usingDefaultOperator(QueryOperator queryOperator) {
        _usingDefaultOperator(queryOperator);
        return this;
    }

    @Override
    public IDocumentQuery<T> noTracking() {
        _noTracking();
        return this;
    }

    @Override
    public IDocumentQuery<T> noCaching() {
        _noCaching();
        return this;
    }

    @Override
    public IDocumentQuery<T> include(String path) {
        _include(path);
        return this;
    }

    @Override
    public IDocumentQuery<T> include(Consumer<IQueryIncludeBuilder> includes) {
        QueryIncludeBuilder includeBuilder = new QueryIncludeBuilder(getConventions());
        includes.accept(includeBuilder);
        _include(includeBuilder);
        return this;
    }

    //TBD expr IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.Include(Expression<Func<T, object>> path)

    @Override
    public IDocumentQuery<T> not() {
        negateNext();
        return this;
    }

    @Override
    public IDocumentQuery<T> take(int count) {
        _take(count);
        return this;
    }

    public IDocumentQuery<T> skip(int count) {
        _skip(count);
        return this;
    }

    @Override
    public IDocumentQuery<T> whereLucene(String fieldName, String whereClause) {
        _whereLucene(fieldName, whereClause, false);
        return this;
    }

    @Override
    public IDocumentQuery<T> whereLucene(String fieldName, String whereClause, boolean exact) {
        _whereLucene(fieldName, whereClause, exact);
        return this;
    }

    @Override
    public IDocumentQuery<T> whereEquals(String fieldName, Object value) {
        _whereEquals(fieldName, value, false);
        return this;
    }

    @Override
    public IDocumentQuery<T> whereEquals(String fieldName, Object value, boolean exact) {
        _whereEquals(fieldName, value, exact);
        return this;
    }

    @Override
    public IDocumentQuery<T> whereEquals(String fieldName, MethodCall method) {
        _whereEquals(fieldName, method);
        return this;
    }

    @Override
    public IDocumentQuery<T> whereEquals(String fieldName, MethodCall method, boolean exact) {
        _whereEquals(fieldName, method, exact);
        return this;
    }

    //TBD expr IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.WhereEquals<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value, bool exact)
    //TBD expr IDocumentQuery<T> IFilterDocumentQueryBase<T, IDocumentQuery<T>>.WhereEquals<TValue>(Expression<Func<T, TValue>> propertySelector, MethodCall value, bool exact)

    @Override
    public IDocumentQuery<T> whereEquals(WhereParams whereParams) {
        _whereEquals(whereParams);
        return this;
    }

    @Override
    public IDocumentQuery<T> whereNotEquals(String fieldName, Object value) {
        _whereNotEquals(fieldName, value);
        return this;
    }

    @Override
    public IDocumentQuery<T> whereNotEquals(String fieldName, Object value, boolean exact) {
        _whereNotEquals(fieldName, value, exact);
        return this;
    }

    @Override
    public IDocumentQuery<T> whereNotEquals(String fieldName, MethodCall method) {
        _whereNotEquals(fieldName, method);
        return this;
    }

    @Override
    public IDocumentQuery<T> whereNotEquals(String fieldName, MethodCall method, boolean exact) {
        _whereNotEquals(fieldName, method, exact);
        return this;
    }

    //TBD expr IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.WhereNotEquals<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value, bool exact)
    //TBD expr IDocumentQuery<T> IFilterDocumentQueryBase<T, IDocumentQuery<T>>.WhereNotEquals<TValue>(Expression<Func<T, TValue>> propertySelector, MethodCall value, bool exact)

    @Override
    public IDocumentQuery<T> whereNotEquals(WhereParams whereParams) {
        _whereNotEquals(whereParams);
        return this;
    }

    @Override
    public IDocumentQuery<T> whereIn(String fieldName, Collection<?> values) {
        return whereIn(fieldName, values, false);
    }

    @Override
    public IDocumentQuery<T> whereIn(String fieldName, Collection<?> values, boolean exact) {
        _whereIn(fieldName, values, exact);
        return this;
    }

    //TBD expr public IDocumentQuery<T> WhereIn<TValue>(Expression<Func<T, TValue>> propertySelector, IEnumerable<TValue> values, bool exact = false)

    @Override
    public IDocumentQuery<T> whereStartsWith(String fieldName, Object value) {
        return whereStartsWith(fieldName, value, false);
    }

    @Override
    public IDocumentQuery<T> whereStartsWith(String fieldName, Object value, boolean exact) {
        _whereStartsWith(fieldName, value, exact);
        return this;
    }

    @Override
    public IDocumentQuery<T> whereEndsWith(String fieldName, Object value) {
        return whereEndsWith(fieldName, value, false);
    }

    @Override
    public IDocumentQuery<T> whereEndsWith(String fieldName, Object value, boolean exact) {
        _whereEndsWith(fieldName, value, exact);
        return this;
    }

    //TBD expr public IDocumentQuery<T> WhereEndsWith<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value)

    @Override
    public IDocumentQuery<T> whereBetween(String fieldName, Object start, Object end) {
        return whereBetween(fieldName, start, end, false);
    }

    @Override
    public IDocumentQuery<T> whereBetween(String fieldName, Object start, Object end, boolean exact) {
        _whereBetween(fieldName, start, end, exact);
        return this;
    }

    //TBD expr public IDocumentQuery<T> WhereBetween<TValue>(Expression<Func<T, TValue>> propertySelector, TValue start, TValue end, bool exact = false)

    @Override
    public IDocumentQuery<T> whereGreaterThan(String fieldName, Object value) {
        return whereGreaterThan(fieldName, value, false);
    }

    @Override
    public IDocumentQuery<T> whereGreaterThan(String fieldName, Object value, boolean exact) {
        _whereGreaterThan(fieldName, value, exact);
        return this;
    }

    @Override
    public IDocumentQuery<T> whereGreaterThanOrEqual(String fieldName, Object value) {
        return whereGreaterThanOrEqual(fieldName, value, false);
    }

    @Override
    public IDocumentQuery<T> whereGreaterThanOrEqual(String fieldName, Object value, boolean exact) {
        _whereGreaterThanOrEqual(fieldName, value, exact);
        return this;
    }

    //TBD expr public IDocumentQuery<T> WhereGreaterThan<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value, bool exact = false)
    //TBD expr public IDocumentQuery<T> WhereGreaterThanOrEqual<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value, bool exact = false)

    public IDocumentQuery<T> whereLessThan(String fieldName, Object value) {
        return whereLessThan(fieldName, value, false);
    }

    public IDocumentQuery<T> whereLessThan(String fieldName, Object value, boolean exact) {
        _whereLessThan(fieldName, value, exact);
        return this;
    }

    //TBD expr public IDocumentQuery<T> WhereLessThanOrEqual<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value, bool exact = false)

    public IDocumentQuery<T> whereLessThanOrEqual(String fieldName, Object value) {
        return whereLessThanOrEqual(fieldName, value, false);
    }

    public IDocumentQuery<T> whereLessThanOrEqual(String fieldName, Object value, boolean exact) {
        _whereLessThanOrEqual(fieldName, value, exact);
        return this;
    }

    //TBD expr public IDocumentQuery<T> WhereLessThanOrEqual<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value, bool exact = false)
    //TBD expr public IDocumentQuery<T> WhereExists<TValue>(Expression<Func<T, TValue>> propertySelector)

    @Override
    public IDocumentQuery<T> whereExists(String fieldName) {
        _whereExists(fieldName);
        return this;
    }

    //TBD expr IDocumentQuery<T> IFilterDocumentQueryBase<T, IDocumentQuery<T>>.WhereRegex<TValue>(Expression<Func<T, TValue>> propertySelector, string pattern)
    @Override
    public IDocumentQuery<T> whereRegex(String fieldName, String pattern) {
        _whereRegex(fieldName, pattern);
        return this;
    }

    public IDocumentQuery<T> andAlso() {
        _andAlso();
        return this;
    }

    @Override
    public IDocumentQuery<T> orElse() {
        _orElse();
        return this;
    }

    @Override
    public IDocumentQuery<T> fuzzy(double fuzzy) {
        _fuzzy(fuzzy);
        return this;
    }

    @Override
    public IDocumentQuery<T> proximity(int proximity) {
        _proximity(proximity);
        return this;
    }

    @Override
    public IDocumentQuery<T> randomOrdering() {
        _randomOrdering();
        return this;
    }

    @Override
    public IDocumentQuery<T> randomOrdering(String seed) {
        _randomOrdering(seed);
        return this;
    }

    //TBD 4.1 public IDocumentQuery<T> customSortUsing(String typeName, boolean descending)


    @Override
    public <TResult> IDocumentQuery<TResult> ofType(Class<TResult> tResultClass) {
        return createDocumentQueryInternal(tResultClass);
    }

    public IDocumentQuery<T> orderBy(String field) {
        return orderBy(field, OrderingType.STRING);
    }

    @Override
    public IDocumentQuery<T> orderBy(String field, String sorterName) {
        _orderBy(field, sorterName);
        return this;
    }

    public IDocumentQuery<T> orderBy(String field, OrderingType ordering) {
        _orderBy(field, ordering);
        return this;
    }

    //TBD expr public IDocumentQuery<T> OrderBy<TValue>(params Expression<Func<T, TValue>>[] propertySelectors)


    @Override
    public IDocumentQuery<T> orderByDescending(String field, String sorterName) {
        _orderByDescending(field, sorterName);
        return this;
    }

    public IDocumentQuery<T> orderByDescending(String field) {
        return orderByDescending(field, OrderingType.STRING);
    }

    public IDocumentQuery<T> orderByDescending(String field, OrderingType ordering) {
        _orderByDescending(field, ordering);
        return this;
    }

    //TBD expr public IDocumentQuery<T> OrderByDescending<TValue>(params Expression<Func<T, TValue>>[] propertySelectors)



    public <TResult> DocumentQuery<TResult> createDocumentQueryInternal(Class<TResult> resultClass) {
        return createDocumentQueryInternal(resultClass, null);
    }

    @SuppressWarnings("unchecked")
    public <TResult> DocumentQuery<TResult> createDocumentQueryInternal(Class<TResult> resultClass, QueryData queryData) {
        FieldsToFetchToken newFieldsToFetch;

        if (queryData != null && queryData.getFields().length > 0) {
            String[] fields = queryData.getFields();

            if (!isGroupBy) {
                Field identityProperty = getConventions().getIdentityProperty(resultClass);

                if (identityProperty != null) {
                    fields = Arrays.stream(queryData.getFields())
                            .map(p -> p.equals(identityProperty.getName()) ? Constants.Documents.Indexing.Fields.DOCUMENT_ID_FIELD_NAME : p)
                            .toArray(String[]::new);
                }
            }

            Reference<String> sourceAliasReference = new Reference<>();
            getSourceAliasIfExists(resultClass, queryData, fields, sourceAliasReference);
            newFieldsToFetch = FieldsToFetchToken.create(fields, queryData.getProjections(), queryData.isCustomFunction(), sourceAliasReference.value);
        } else {
            newFieldsToFetch = null;
        }

        if (newFieldsToFetch != null) {
            updateFieldsToFetchToken(newFieldsToFetch);
        }

        DocumentQuery query = new DocumentQuery<>(resultClass,
                theSession,
                getIndexName(),
                getCollectionName(),
                isGroupBy,
                queryData != null ? queryData.getDeclareTokens() : null,
                queryData != null ? queryData.getLoadTokens() : null,
                queryData != null ? queryData.getFromAlias() : null,
                queryData != null ? queryData.isProjectInto() : null);

        query.queryRaw = queryRaw;
        query.pageSize = pageSize;
        query.selectTokens = new LinkedList<>(selectTokens);
        query.fieldsToFetchToken = fieldsToFetchToken;
        query.whereTokens = new LinkedList<>(whereTokens);
        query.orderByTokens = new LinkedList<>(orderByTokens);
        query.queryParameters = new Parameters(queryParameters);
        query.start = start;
        query.timeout = timeout;
        query.queryStats = queryStats;
        query.theWaitForNonStaleResults = theWaitForNonStaleResults;
        query.negate = negate;
        query.documentIncludes = new HashSet<>(documentIncludes);
        query.rootTypes = Sets.newHashSet(clazz);
        query.beforeQueryExecutedCallback = beforeQueryExecutedCallback;
        query.afterQueryExecutedCallback = afterQueryExecutedCallback;
        query.afterStreamExecutedCallback = afterStreamExecutedCallback;
        query.disableEntitiesTracking = disableEntitiesTracking;
        query.disableCaching = disableCaching;
        query.isIntersect = isIntersect;
        query.defaultOperator = defaultOperator;

        return query;
    }


    //TBD expr IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.Highlight(Expression<Func<T, object>> path, int fragmentLength, int fragmentCount, out Highlightings highlightings)
    //TBD expr IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.Highlight(Expression<Func<T, object>> path, int fragmentLength, int fragmentCount, HighlightingOptions options, out Highlightings highlightings)
    //TBD expr public IDocumentQuery<T> Spatial(Expression<Func<T, object>> path, Func<SpatialCriteriaFactory, SpatialCriteria> clause)



    //TBD expr public IDocumentQuery<T> Spatial(Func<SpatialDynamicFieldFactory<T>, DynamicSpatialField> field, Func<SpatialCriteriaFactory, SpatialCriteria> clause)
    //TBD expr IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.WithinRadiusOf<TValue>(Expression<Func<T, TValue>> propertySelector, double radius, double latitude, double longitude, SpatialUnits? radiusUnits, double distanceErrorPct)


    @Override
    public IDocumentQuery<T> orderByDistance(String fieldName, double latitude, double longitude) {
        orderByDistance(fieldName, latitude, longitude, 0);
        return this;
    }

    @Override
    public IDocumentQuery<T> orderByDistance(String fieldName, double latitude, double longitude, double roundFactor) {
        _orderByDistance(fieldName, latitude, longitude, roundFactor);
        return this;
    }

    //TBD expr IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.OrderByDistance<TValue>(Expression<Func<T, TValue>> propertySelector, string shapeWkt)

    @Override
    public IDocumentQuery<T> orderByDistance(String fieldName, String shapeWkt) {
        _orderByDistance(fieldName, shapeWkt);
        return this;
    }


    @Override
    public IDocumentQuery<T> orderByDistanceDescending(String fieldName, double latitude, double longitude) {
        return orderByDistanceDescending(fieldName, latitude, longitude, 0);
    }

    @Override
    public IDocumentQuery<T> orderByDistanceDescending(String fieldName, double latitude, double longitude, double roundFactor) {
        _orderByDistanceDescending(fieldName, latitude, longitude, roundFactor);
        return this;
    }

    //TBD expr IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.OrderByDistanceDescending<TValue>(Expression<Func<T, TValue>> propertySelector, string shapeWkt)

    @Override
    public IDocumentQuery<T> orderByDistanceDescending(String fieldName, String shapeWkt) {
        _orderByDistanceDescending(fieldName, shapeWkt);
        return this;
    }


}
