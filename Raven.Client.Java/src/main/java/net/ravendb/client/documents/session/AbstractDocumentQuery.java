package net.ravendb.client.documents.session;

import com.google.common.base.Defaults;
import jdk.nashorn.internal.objects.annotations.Where;
import net.ravendb.client.Constants;
import net.ravendb.client.Parameters;
import net.ravendb.client.documents.commands.QueryCommand;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.indexes.spatial.SpatialRelation;
import net.ravendb.client.documents.indexes.spatial.SpatialUnits;
import net.ravendb.client.documents.queries.*;
import net.ravendb.client.documents.queries.spatial.SpatialCriteria;
import net.ravendb.client.documents.queries.spatial.SpatialDynamicField;
import net.ravendb.client.documents.session.operations.QueryOperation;
import net.ravendb.client.documents.session.tokens.*;
import net.ravendb.client.primitives.CleanCloseable;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.primitives.Tuple;
import org.apache.commons.lang3.StringUtils;

import javax.management.Query;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

/**
 * A query against a Raven index
 */
public abstract class AbstractDocumentQuery<T, TSelf extends AbstractDocumentQuery<T, TSelf>> implements IAbstractDocumentQuery<T> {

    protected Class<T> clazz;

    private final Map<String, String> _aliasToGroupByFieldName = new HashMap<>();

    protected QueryOperator defaultOperatator = QueryOperator.AND;

    //TBD: private readonly LinqPathProvider _linqPathProvider;

    protected final Set<Class> rootTypes = new HashSet<>();

    /**
     * Whether to negate the next operation
     */
    protected boolean negate;

    private String indexName;
    private String collectionName;
    private int _currentClauseDepth;

    protected String queryRaw;

    public String getIndexName() {
        return indexName;
    }

    public String getCollectionName() {
        return collectionName;
    }

    protected Tuple<String, Object> lastEquality;

    protected Parameters queryParameters = new Parameters();

    protected boolean isIntersect;

    protected boolean isGroupBy;

    protected final InMemoryDocumentSessionOperations theSession;

    protected Integer pageSize;

    protected List<QueryToken> selectTokens = new LinkedList<>();

    protected final FromToken fromToken;
    protected final DeclareToken declareToken;
    protected final List<LoadToken> loadTokens;
    protected FieldsToFetchToken fieldsToFetchToken;

    protected List<QueryToken> whereTokens = new LinkedList<>();

    protected List<QueryToken> groupByTokens = new LinkedList<>();

    protected List<QueryToken> orderByTokens = new LinkedList<>();

    protected int start;

    private final DocumentConventions _conventions;

    protected Duration timeout;

    protected boolean theWaitForNonStaleResults;

    protected Set<String> includes = new HashSet<>();

    /**
     * Holds the query stats
     */
    protected QueryStatistics queryStats = new QueryStatistics();

    protected boolean disableEntitiesTracking;

    protected boolean disableCaching;

    protected boolean showQueryTimings;

    protected boolean shouldExplainScores;

    public boolean isDistinct() {
        return selectTokens.isEmpty() ? false : selectTokens.get(0) instanceof DistinctToken;
    }

    /**
     * Gets the document convention from the query session
     */
    public DocumentConventions getConventions() {
        return _conventions;
    }

    /**
     * Gets the session associated with this document query
     */
    public IDocumentSession getSession() {
        return (IDocumentSession) theSession;
    }

    //TBD public IAsyncDocumentSession AsyncSession => (IAsyncDocumentSession)TheSession;

    public boolean isDynamicMapReduce() {
        return !groupByTokens.isEmpty();
    }

    protected Long cutoffEtag;

    private static Duration getDefaultTimeout() {
        return Duration.ofSeconds(15);
    }

    protected AbstractDocumentQuery(Class<T> clazz, InMemoryDocumentSessionOperations session, String indexName,
                                    String collectionName, boolean isGroupBy, DeclareToken declareToken,
                                    List<LoadToken> loadTokens) {
        this(clazz, session, indexName, collectionName, isGroupBy, declareToken, loadTokens, null);
    }

    protected AbstractDocumentQuery(Class<T> clazz, InMemoryDocumentSessionOperations session, String indexName,
                                    String collectionName, boolean isGroupBy, DeclareToken declareToken,
                                    List<LoadToken> loadTokens, String fromAlias) {
        this.clazz = clazz;
        rootTypes.add(clazz);
        this.isGroupBy = isGroupBy;
        this.indexName = indexName;
        this.collectionName = collectionName;
        this.fromToken = FromToken.create(indexName, collectionName, fromAlias);
        this.declareToken = declareToken;
        this.loadTokens = loadTokens;
        theSession = session;
        //TODO: AfterQueryExecuted(UpdateStatsAndHighlightings);
        _conventions = session == null ? new DocumentConventions() : session.getConventions();
        //TBD _linqPathProvider = new LinqPathProvider(_conventions);
    }

    public void _usingDefaultOperator(QueryOperator operator) {
        if (!whereTokens.isEmpty()) {
            throw new IllegalStateException("Default operator can only be set before any where clause is added.");
        }

        defaultOperatator = operator;
    }

    /**
     * Instruct the query to wait for non stale result for the specified wait timeout.
     */
    public void _waitForNonStaleResults(Duration waitTimeout) {
        theWaitForNonStaleResults = true;
        cutoffEtag = null;
        timeout = waitTimeout;
    }

    protected QueryOperation initializeQueryOperation() {
        IndexQuery indexQuery = getIndexQuery();

        return new QueryOperation(theSession, indexName, indexQuery, fieldsToFetchToken != null ? fieldsToFetchToken.projections : null,
                theWaitForNonStaleResults, timeout, disableEntitiesTracking, false, false);
    }

    public IndexQuery getIndexQuery() {
        String query = toString();
        IndexQuery indexQuery = generateIndexQuery(query);
        //TODO: BeforeQueryExecutedCallback?.Invoke(indexQuery);
        return indexQuery;
    }

    /**
     * Gets the fields for projection
     */
    public List<String> getProjectionFields() {
        return fieldsToFetchToken != null && fieldsToFetchToken.projections != null ? Arrays.asList(fieldsToFetchToken.projections) : Collections.emptyList();
    }

    /**
     * Order the search results randomly
     */
    public void _randomOrdering() {
        assertNoRawQuery();
        orderByTokens.add(OrderByToken.random);
    }

    /**
     * Order the search results randomly using the specified seed
     * this is useful if you want to have repeatable random queries
     */
    public void _randomOrdering(String seed) {
        assertNoRawQuery();

        if (StringUtils.isBlank(seed)) {
            _randomOrdering();
            return;
        }

        orderByTokens.add(OrderByToken.createRandom(seed));
    }

    @Override
    public void _customSortUsing(String typeName) {
        _customSortUsing(typeName, false);
    }

    @Override
    public void _customSortUsing(String typeName, boolean descending) {
        if (descending) {
            _orderByDescending(Constants.Documents.Indexing.Fields.CUSTOM_SORT_FIELD_NAME + ";" + typeName);
            return;
        }

        _orderBy(Constants.Documents.Indexing.Fields.CUSTOM_SORT_FIELD_NAME + ";" + typeName);
    }

    protected void addGroupByAlias(String fieldName, String projectedName) {
        _aliasToGroupByFieldName.put(projectedName, fieldName);
    }

    private void assertNoRawQuery() {
        if (queryRaw != null) {
            throw new IllegalStateException("RawQuery was called, cannot modify this query by calling on operations that would modify the query (such as Where, Select, OrderBy, GroupBy, etc)");
        }
    }

    public void _rawQuery(String query) {
        if (!selectTokens.isEmpty() ||
                !whereTokens.isEmpty() ||
                !orderByTokens.isEmpty() ||
                !groupByTokens.isEmpty()) {
            throw new IllegalStateException("You can only use rawQuery on a new query, without applying any operations (such as where, select, orderBy, groupBy, etc)");
        }

        queryRaw = query;
    }

    public void _addParameter(String name, Object value) {
        if (queryParameters.containsKey(name)) {
            throw new IllegalStateException("The parameter " + name + " was already added");
        }

        queryParameters.put(name, value);
    }

    @Override
    public void _groupBy(String fieldName, String... fieldNames) {
        if (!fromToken.isDynamic()) {
            throw new IllegalStateException("GroupBy only works with dynamic queries");
        }

        assertNoRawQuery();
        isGroupBy = true;

        fieldName = ensureValidFieldName(fieldName, false);

        groupByTokens.add(GroupByToken.create(fieldName));

        if (fieldNames == null || fieldNames.length <= 0) {
            return;
        }

        for (String name : fieldNames) {
            fieldName = ensureValidFieldName(name, false);
            groupByTokens.add(GroupByToken.create(fieldName));
        }
    }

    @Override
    public void _groupByKey(String fieldName) {
        _groupByKey(fieldName, null);
    }

    @Override
    public void _groupByKey(String fieldName, String projectedName) {
        assertNoRawQuery();
        isGroupBy = true;

        if (projectedName != null && _aliasToGroupByFieldName.containsKey(projectedName)) {
            String aliasedFieldName = _aliasToGroupByFieldName.get(projectedName);
            if (fieldName == null || fieldName.equalsIgnoreCase(projectedName)) {
                fieldName = aliasedFieldName;
            }
        }

        selectTokens.add(GroupByKeyToken.create(fieldName, projectedName));
    }

    @Override
    public void _groupBySum(String fieldName) {
        _groupBySum(fieldName, null);
    }

    @Override
    public void _groupBySum(String fieldName, String projectedName) {
        assertNoRawQuery();
        isGroupBy = true;

        fieldName = ensureValidFieldName(fieldName, false);
        selectTokens.add(GroupBySumToken.create(fieldName, projectedName));
    }

    @Override
    public void _groupByCount() {
        _groupByCount(null);
    }

    @Override
    public void _groupByCount(String projectedName) {
        assertNoRawQuery();
        isGroupBy = true;

        selectTokens.add(GroupByCountToken.create(projectedName));
    }

    public void _whereTrue() {
        appendOperatorIfNeeded(whereTokens);
        negateIfNeeded(null);

        whereTokens.add(TrueToken.INSTANCE);
    }

    /**
     * Includes the specified path in the query, loading the document specified in that path
     */
    public void _include(String path) {
        includes.add(path);
    }

    //TBD: public void Include(Expression<Func<T, object>> path)

    public void _take(int count) {
        pageSize = count;
    }

    public void _skip(int count) {
        start = count;
    }

    /**
     * Filter the results from the index using the specified where clause.
     */
    public void _whereLucene(String fieldName, String whereClause) {
        fieldName = ensureValidFieldName(fieldName, false);

        appendOperatorIfNeeded(whereTokens);
        negateIfNeeded(fieldName);

        whereTokens.add(WhereToken.lucene(fieldName, addQueryParameter(whereClause)));
    }

    /**
     * Simplified method for opening a new clause within the query
     */
    public void _openSubclause() {
        _currentClauseDepth++;

        appendOperatorIfNeeded(whereTokens);
        negateIfNeeded(null);

        whereTokens.add(OpenSubclauseToken.INSTANCE);
    }

    /**
     * Simplified method for closing a clause within the query
     */
    public void _closeSubclause() {
        _currentClauseDepth--;

        whereTokens.add(CloseSubclauseToken.INSTANCE);
    }

    public void _whereEquals(String fieldName, Object value) {
        _whereEquals(fieldName, value, false);
    }

    public void _whereEquals(String fieldName, Object value, boolean exact) {
        WhereParams params = new WhereParams();
        params.setFieldName(fieldName);
        params.setValue(value);
        params.setExact(exact);
        _whereEquals(params);
    }

    public void _whereEquals(WhereParams whereParams) {
        if (negate) {
            negate = false;
            _whereNotEquals(whereParams);
            return;
        }

        Object transformToEqualValue = transformValue(whereParams);
        lastEquality = Tuple.create(whereParams.getFieldName(), transformToEqualValue);

        appendOperatorIfNeeded(whereTokens);

        whereParams.setFieldName(ensureValidFieldName(whereParams.getFieldName(), whereParams.isNestedPath()));
        whereTokens.add(WhereToken.equals(whereParams.getFieldName(), addQueryParameter(transformToEqualValue), whereParams.isExact()));

    }

    public void _whereNotEquals(String fieldName, Object value) {
        _whereNotEquals(fieldName, value, false);
    }

    public void _whereNotEquals(String fieldName, Object value, boolean exact) {
        WhereParams params = new WhereParams();
        params.setFieldName(fieldName);
        params.setValue(value);
        params.setExact(exact);

        _whereNotEquals(params);
    }

    public void _whereNotEquals(WhereParams whereParams) {
        if (negate) {
            negate = false;
            _whereEquals(whereParams);
            return;
        }

        Object transformToEqualValue = transformValue(whereParams);
        lastEquality = Tuple.create(whereParams.getFieldName(), transformToEqualValue);

        appendOperatorIfNeeded(whereTokens);

        whereParams.setFieldName(ensureValidFieldName(whereParams.getFieldName(), whereParams.isNestedPath()));
        whereTokens.add(WhereToken.notEquals(whereParams.getFieldName(), addQueryParameter(transformToEqualValue), whereParams.isExact()));
    }

    public void negateNext() {
        negate = !negate;
    }

    /**
     * Check that the field has one of the specified value
     */
    @Override
    public void _whereIn(String fieldName, Collection<Object> values) {
        _whereIn(fieldName, values, false);
    }

    /**
     * Check that the field has one of the specified value
     */
    @Override
    public void _whereIn(String fieldName, Collection<Object> values, boolean exact) {
        fieldName = ensureValidFieldName(fieldName, false);

        appendOperatorIfNeeded(whereTokens);
        negateIfNeeded(fieldName);

        whereTokens.add(WhereToken.in(fieldName, addQueryParameter(transformCollection(fieldName, unpackCollection(values))), exact));
    }

    public void _whereStartsWith(String fieldName, Object value) {
        WhereParams whereParams = new WhereParams();
        whereParams.setFieldName(fieldName);
        whereParams.setValue(value);
        whereParams.setAllowWildcards(true);

        Object transformToEqualValue = transformValue(whereParams);

        lastEquality = Tuple.create(whereParams.getFieldName(), transformToEqualValue);

        appendOperatorIfNeeded(whereTokens);

        whereParams.setFieldName(ensureValidFieldName(whereParams.getFieldName(), whereParams.isNestedPath()));
        negateIfNeeded(whereParams.getFieldName());

        whereTokens.add(WhereToken.startsWith(whereParams.getFieldName(), addQueryParameter(transformToEqualValue), false));
    }

    /**
     * Matches fields which ends with the specified value.
     */
    public void _whereEndsWith(String fieldName, Object value) {
        WhereParams whereParams = new WhereParams();
        whereParams.setFieldName(fieldName);
        whereParams.setValue(value);
        whereParams.setAllowWildcards(true);

        Object transformToEqualValue = transformValue(whereParams);
        lastEquality = Tuple.create(whereParams.getFieldName(), transformToEqualValue);

        appendOperatorIfNeeded(whereTokens);

        whereParams.setFieldName(ensureValidFieldName(whereParams.getFieldName(), whereParams.isNestedPath()));
        negateIfNeeded(whereParams.getFieldName());

        whereTokens.add(WhereToken.endsWith(whereParams.getFieldName(), addQueryParameter(transformToEqualValue), false));
    }

    @Override
    public void _whereBetween(String fieldName, Object start, Object end) {
        _whereBetween(fieldName, start, end, false);
    }

    /**
     * Matches fields where the value is between the specified start and end, exclusive
     */
    @Override
    public void _whereBetween(String fieldName, Object start, Object end, boolean exact) {
        fieldName = ensureValidFieldName(fieldName, false);

        appendOperatorIfNeeded(whereTokens);
        negateIfNeeded(fieldName);

        WhereParams startParams = new WhereParams();
        startParams.setValue(start);
        startParams.setFieldName(fieldName);

        WhereParams endParams = new WhereParams();
        endParams.setValue(end);
        endParams.setFieldName(fieldName);

        String fromParameterName = addQueryParameter(start == null ? "*" : transformValue(startParams, true));
        String toParameterName = addQueryParameter(start == null ? "NULL" : transformValue(endParams, true));

        whereTokens.add(WhereToken.between(fieldName, fromParameterName, toParameterName, exact));
    }

    public void _whereGreaterThan(String fieldName, Object value) {
        _whereGreaterThan(fieldName, value, false);
    }

    /**
     * Matches fields where the value is greater than the specified value
     */
    public void _whereGreaterThan(String fieldName, Object value, boolean exact) {
        fieldName = ensureValidFieldName(fieldName, false);

        appendOperatorIfNeeded(whereTokens);
        negateIfNeeded(fieldName);
        WhereParams whereParams = new WhereParams();
        whereParams.setValue(value);
        whereParams.setFieldName(fieldName);

        whereTokens.add(WhereToken.greaterThan(fieldName, addQueryParameter(value == null ? "*" : transformValue(whereParams, true)), exact));
    }

    public void _whereGreaterThanOrEqual(String fieldName, Object value) {
        _whereGreaterThanOrEqual(fieldName, value, false);
    }

    /**
     * Matches fields where the value is greater than or equal to the specified value
     */
    public void _whereGreaterThanOrEqual(String fieldName, Object value, boolean exact) {
        fieldName = ensureValidFieldName(fieldName, false);

        appendOperatorIfNeeded(whereTokens);
        negateIfNeeded(fieldName);
        WhereParams whereParams = new WhereParams();
        whereParams.setValue(value);
        whereParams.setFieldName(fieldName);

        whereTokens.add(WhereToken.greaterThanOrEqual(fieldName, addQueryParameter(value == null ? "*" : transformValue(whereParams, true)), exact));
    }

    public void _whereLessThan(String fieldName, Object value) {
        _whereLessThan(fieldName, value);
    }

    public void _whereLessThan(String fieldName, Object value, boolean exact) {
        fieldName = ensureValidFieldName(fieldName, false);

        appendOperatorIfNeeded(whereTokens);
        negateIfNeeded(fieldName);

        WhereParams whereParams = new WhereParams();
        whereParams.setValue(value);
        whereParams.setFieldName(fieldName);

        whereTokens.add(WhereToken.lessThan(fieldName,
                addQueryParameter(value == null ? "NULL" : transformValue(whereParams, true)), exact));
    }

    public void _whereLessThanOrEqual(String fieldName, Object value) {
        _whereLessThanOrEqual(fieldName, value);
    }

    public void _whereLessThanOrEqual(String fieldName, Object value, boolean exact) {
        appendOperatorIfNeeded(whereTokens);
        negateIfNeeded(fieldName);

        WhereParams whereParams = new WhereParams();
        whereParams.setValue(value);
        whereParams.setFieldName(fieldName);

        whereTokens.add(WhereToken.lessThanOrEqual(fieldName,
                addQueryParameter(value == null ? "NULL" : transformValue(whereParams, true)), exact));
    }

    public void _andAlso() {
        if (whereTokens.isEmpty()) {
            return;
        }

        if (whereTokens.get(whereTokens.size() - 1) instanceof QueryOperatorToken) {
            throw new IllegalStateException("Cannot add AND, previous token was already an operator token.");
        }

        whereTokens.add(QueryOperatorToken.AND);
    }

    /**
     * Add an OR to the query
     */
    public void _orElse() {
        if (whereTokens.isEmpty()) {
            return;
        }

        if (whereTokens.get(whereTokens.size() - 1) instanceof QueryOperatorToken) {
            throw new IllegalStateException("Cannot add OR, previous token was already an operator token.");
        }

        whereTokens.add(QueryOperatorToken.OR);
    }

    /**
     * Specifies a boost weight to the last where clause.
     * The higher the boost factor, the more relevant the term will be.
     *
     * boosting factor where 1.0 is default, less than 1.0 is lower weight, greater than 1.0 is higher weight
     *
     * http://lucene.apache.org/java/2_4_0/queryparsersyntax.html#Boosting%20a%20Term
     */
    @Override
    public void _boost(double boost) {
        if (boost == 1.0) {
            return;
        }

        if (whereTokens.isEmpty()) {
            throw new IllegalStateException("Missing where clause");
        }

        QueryToken whereToken = whereTokens.get(whereTokens.size() - 1);
        if (!(whereToken instanceof WhereToken)) {
            throw new IllegalStateException("Missing where clause");
        }

        if (boost <= 0.0) {
            throw new IllegalArgumentException("Boost factor must be a positive number");
        }

        ((WhereToken) whereToken).setBoost(boost);
    }

    /**
     * Specifies a fuzziness factor to the single word term in the last where clause
     *
     * 0.0 to 1.0 where 1.0 means closer match
     *
     * http://lucene.apache.org/java/2_4_0/queryparsersyntax.html#Fuzzy%20Searches
     */
    @Override
    public void _fuzzy(double fuzzy) {
        if (whereTokens.isEmpty()) {
            throw new IllegalStateException("Missing where clause");
        }

        QueryToken whereToken = whereTokens.get(whereTokens.size() - 1);
        if (!(whereToken instanceof WhereToken)) {
            throw new IllegalStateException("Missing where clause");
        }

        if (fuzzy < 0.0 || fuzzy > 1.0) {
            throw new IllegalArgumentException("Fuzzy distance must be between 0.0 and 1.0");
        }

        ((WhereToken) whereToken).setFuzzy(fuzzy);
    }

    /**
     * Specifies a proximity distance for the phrase in the last where clause
     *
     * http://lucene.apache.org/java/2_4_0/queryparsersyntax.html#Proximity%20Searches
     */
    @Override
    public void _proximity(int proximity) {
        if (whereTokens.isEmpty()) {
            throw new IllegalStateException("Missing where clause");
        }

        QueryToken whereToken = whereTokens.get(whereTokens.size() - 1);
        if (!(whereToken instanceof WhereToken)) {
            throw new IllegalStateException("Missing where clause");
        }

        if (proximity < 1) {
            throw new IllegalArgumentException("Proximity distance must be a positive number");
        }

        ((WhereToken) whereToken).setProximity(proximity);
    }

    /**
     * Order the results by the specified fields
     * The fields are the names of the fields to sort, defaulting to sorting by ascending.
     * You can prefix a field name with '-' to indicate sorting by descending or '+' to sort by ascending
     */
    public void _orderBy(String field) {
        _orderBy(field, OrderingType.STRING);
    }

    /**
     * Order the results by the specified fields
     * The fields are the names of the fields to sort, defaulting to sorting by ascending.
     * You can prefix a field name with '-' to indicate sorting by descending or '+' to sort by ascending
     */
    public void _orderBy(String field, OrderingType ordering) {
        assertNoRawQuery();
        String f = ensureValidFieldName(field, false);
        orderByTokens.add(OrderByToken.createAscending(f, ordering));
    }

    /**
     * Order the results by the specified fields
     * The fields are the names of the fields to sort, defaulting to sorting by descending.
     * You can prefix a field name with '-' to indicate sorting by descending or '+' to sort by ascending
     */
    public void _orderByDescending(String field) {
        _orderByDescending(field, OrderingType.STRING);
    }

    /**
     * Order the results by the specified fields
     * The fields are the names of the fields to sort, defaulting to sorting by descending.
     * You can prefix a field name with '-' to indicate sorting by descending or '+' to sort by ascending
     */
    public void _orderByDescending(String field, OrderingType ordering) {
        assertNoRawQuery();
        String f = ensureValidFieldName(field, false);
        orderByTokens.add(OrderByToken.createDescending(f, ordering));
    }

    public void _orderByScore() {
        assertNoRawQuery();

        orderByTokens.add(OrderByToken.scoreAscending);
    }

    public void _orderByScoreDescending() {
        assertNoRawQuery();
        orderByTokens.add(OrderByToken.scoreDescending);
    }

    /**
     * Instructs the query to wait for non stale results as of the cutoff etag.
     */
    public void _waitForNonStaleResultsAsOf(long cutoffEtag) {
        _waitForNonStaleResultsAsOf(cutoffEtag, getDefaultTimeout());
    }

    /**
     * Instructs the query to wait for non stale results as of the cutoff etag.
     */
    public void _waitForNonStaleResultsAsOf(long cutoffEtag, Duration waitTimeout) {
        theWaitForNonStaleResults = true;
        timeout = waitTimeout;
        this.cutoffEtag = cutoffEtag;
    }

    /**
     * EXPERT ONLY: Instructs the query to wait for non stale results.
     * This shouldn't be used outside of unit tests unless you are well aware of the implications
     */
    public void _waitForNonStaleResults() {
        _waitForNonStaleResults(getDefaultTimeout());
    }

    /**
     * Provide statistics about the query, such as total count of matching records
     */
    public void _statistics(Reference<QueryStatistics> stats) {
        stats.value = queryStats;
    }

    /**
     * Called externally to raise the after query executed callback
     */
    public void invokeAfterQueryExecuted(QueryResult result) {
        for (Consumer<QueryResult> consumer : afterQueryExecutedCallback) {
            consumer.accept(result);
        }
    }

    //TBD public void InvokeAfterStreamExecuted(BlittableJsonReaderObject result)

    /**
     * Generates the index query.
     */
    protected IndexQuery generateIndexQuery(String query) {
        IndexQuery indexQuery = new IndexQuery();
        indexQuery.setQuery(query);
        indexQuery.setStart(start);
        indexQuery.setCutoffEtag(cutoffEtag);
        indexQuery.setWaitForNonStaleResults(theWaitForNonStaleResults);
        indexQuery.setWaitForNonStaleResultsTimeout(timeout);
        indexQuery.setQueryParameters(queryParameters);
        indexQuery.setDisableCaching(disableCaching);
        indexQuery.setShowTimings(showQueryTimings);
        indexQuery.setExplainScores(shouldExplainScores);

        if (pageSize != null) {
            indexQuery.setPageSize(pageSize);
        }
        return indexQuery;
    }

    /**
     * Perform a search for documents which fields that match the searchTerms.
     * If there is more than a single term, each of them will be checked independently.
     */
    @Override
    public void _search(String fieldName, String searchTerms) {
        _search(fieldName, searchTerms, SearchOperator.OR);
    }

    /**
     * Perform a search for documents which fields that match the searchTerms.
     * If there is more than a single term, each of them will be checked independently.
     */
    @Override
    public void _search(String fieldName, String searchTerms, SearchOperator operator) {
        boolean hasWhiteSpace = searchTerms.chars().anyMatch(x -> Character.isWhitespace(x));
        lastEquality = Tuple.create(fieldName, hasWhiteSpace ? "(" + searchTerms + ")" : searchTerms);

        appendOperatorIfNeeded(whereTokens);

        fieldName = ensureValidFieldName(fieldName, false);
        negateIfNeeded(fieldName);

        whereTokens.add(WhereToken.search(fieldName, addQueryParameter(searchTerms), operator));
    }

    @Override
    public String toString() {
        if (queryRaw != null) {
            return queryRaw;
        }

        if (_currentClauseDepth != 0) {
            throw new IllegalStateException("A clause was not closed correctly within this query, current clause depth = " + _currentClauseDepth);
        }

        StringBuilder queryText = new StringBuilder();
        buildDeclare(queryText);
        buildFrom(queryText);
        buildGroupBy(queryText);
        buildWhere(queryText);
        buildOrderBy(queryText);

        buildLoad(queryText);
        buildSelect(queryText);
        buildInclude(queryText);

        return queryText.toString();
    }

    private void buildInclude(StringBuilder queryText) {
        if (includes == null || includes.isEmpty()) {
            return;
        }

        queryText.append(" INCLUDE ");
        boolean first = true;
        for (String include : includes) {
            if (!first) {
                queryText.append(",");
            }
            first = false;

            boolean requiredQuotes = false;

            for (int i = 0; i < include.length(); i++) {
                char ch = include.charAt(i);
                if (!Character.isLetterOrDigit(ch) && ch != '_' && ch != '.') {
                    requiredQuotes = true;
                    break;
                }
            }

            if (requiredQuotes) {
                queryText.append("'").append(include.replaceAll("'", "\\'")).append("'");
            } else {
                queryText.append(include);
            }
        }
    }

    /**
     * The last term that we asked the query to use equals on
     */
    @Override
    public Tuple<String, Object> getLastEqualityTerm() {
        return lastEquality;
    }

    @Override
    public void _intersect() {
        if (whereTokens.size() > 0) {
            QueryToken last = whereTokens.get(whereTokens.size() - 1);
            if (last instanceof WhereToken || last instanceof CloseSubclauseToken) {
                isIntersect = true;

                whereTokens.add(IntersectMarkerToken.INSTANCE);
                return;
            }
        }

        throw new IllegalStateException("Cannot add INTERSECT at this point.");
    }

    public void _whereExists(String fieldName) {
        fieldName = ensureValidFieldName(fieldName, false);

        appendOperatorIfNeeded(whereTokens);
        negateIfNeeded(fieldName);

        whereTokens.add(WhereToken.exists(fieldName));
    }

    @Override
    public void _containsAny(String fieldName, Collection<Object> values) {
        fieldName = ensureValidFieldName(fieldName, false);

        appendOperatorIfNeeded(whereTokens);
        negateIfNeeded(fieldName);

        Collection<Object> array = transformCollection(fieldName, unpackCollection(values));
        if (array.isEmpty()) {
            whereTokens.add(TrueToken.INSTANCE);
            return;
        }

        whereTokens.add(WhereToken.in(fieldName, addQueryParameter(array), false));
    }

    @Override
    public void _containsAll(String fieldName, Collection<Object> values) {
        fieldName = ensureValidFieldName(fieldName, false);

        appendOperatorIfNeeded(whereTokens);
        negateIfNeeded(fieldName);

        Collection<Object> array = transformCollection(fieldName, unpackCollection(values));

        if (array.isEmpty()) {
            whereTokens.add(TrueToken.INSTANCE);
            return;
        }

        whereTokens.add(WhereToken.allIn(fieldName, addQueryParameter(array)));
    }

    @Override
    public void _addRootType(Class clazz) {
        rootTypes.add(clazz);
    }

    //TBD public string GetMemberQueryPathForOrderBy(Expression expression)
    //TBD public string GetMemberQueryPath(Expression expression)


    @Override
    public void _distinct() {
        if (isDistinct()) {
            throw new IllegalStateException("The is already a distinct query");
        }

        if (selectTokens.isEmpty()) {
            selectTokens.add(DistinctToken.INSTANCE);
        } else {
            selectTokens.add(0, DistinctToken.INSTANCE);
        }
    }

    private void UpdateStatsAndHighlightings(QueryResult queryResult) {
        queryStats.updateQueryStats(queryResult);
        //TODO: QueryStats.UpdateQueryStats(queryResult);
        //TBD: Highlightings.Update(queryResult);
    }

    private void buildSelect(StringBuilder writer) {
        if (selectTokens.isEmpty()) {
            return;
        }

        writer.append(" SELECT ");
        if (selectTokens.size() == 1 && selectTokens.get(0) instanceof DistinctToken) {
            selectTokens.get(0).writeTo(writer);
            writer.append(" *");

            return;
        }

        for (int i = 0; i < selectTokens.size(); i++) {
            QueryToken token = selectTokens.get(i);
            if (i > 0 && (selectTokens.get(i - 1) instanceof DistinctToken)) {
                writer.append(",");
            }

            addSpaceIfNeeded(i > 0 ? selectTokens.get(i - 1) : null, token, writer);

            token.writeTo(writer);
        }
    }

    private void buildFrom(StringBuilder writer) {
        fromToken.writeTo(writer);
    }

    private void buildDeclare(StringBuilder writer) {
        if (declareToken != null) {
            declareToken.writeTo(writer);
        }
    }

    private void buildLoad(StringBuilder writer) {
        if (loadTokens == null || loadTokens.isEmpty()) {
            return;
        }

        writer.append(" LOAD ");

        for (int i = 0; i < loadTokens.size(); i++) {
            if (i != 0) {
                writer.append(", ");
            }

            loadTokens.get(i).writeTo(writer);
        }
    }

    private void buildWhere(StringBuilder writer) {
        if (whereTokens.isEmpty()) {
            return;
        }

        writer
                .append(" WHERE ");

        if (isIntersect) {
            writer
                    .append("intersect(");
        }

        for (int i = 0; i < whereTokens.size(); i++) {
            addSpaceIfNeeded(i > 0 ? whereTokens.get(i - 1) : null, whereTokens.get(i), writer);
            whereTokens.get(i).writeTo(writer);
        }

        if (isIntersect) {
            writer.append(") ");
        }
    }

    private void buildGroupBy(StringBuilder writer) {
        if (groupByTokens.isEmpty()) {
            return;
        }

        writer
                .append(" GROUP BY ");

        boolean isFirst = true;

        for (QueryToken token : groupByTokens) {
            if (!isFirst) {
                writer.append(", ");
            }

            token.writeTo(writer);
            isFirst = false;
        }
    }

    private void buildOrderBy(StringBuilder writer) {
        if (orderByTokens.isEmpty()) {
            return;
        }

        writer
                .append(" ORDER BY ");

        boolean isFirst = true;

        for (QueryToken token : orderByTokens) {
            if (!isFirst) {
                writer.append(", ");
            }

            token.writeTo(writer);
            isFirst = false;
        }
    }

    private static void addSpaceIfNeeded(QueryToken previousToken, QueryToken currentToken, StringBuilder writer) {
        if (previousToken == null) {
            return;
        }

        if (previousToken instanceof OpenSubclauseToken || currentToken instanceof CloseSubclauseToken || currentToken instanceof IntersectMarkerToken) {
            return;
        }
        writer.append(" ");
    }

    private void appendOperatorIfNeeded(List<QueryToken> tokens) {
        assertNoRawQuery();

        if (tokens.isEmpty()) {
            return;
        }

        QueryToken lastToken = tokens.get(tokens.size() - 1);
        if (!(lastToken instanceof WhereToken) && !(lastToken instanceof CloseSubclauseToken)) {
            return;
        }

        WhereToken lastWhere = null;

        for (int i = tokens.size() - 1; i >= 0; i--) {
            if (tokens.get(i) instanceof WhereToken) {
                lastWhere = (WhereToken) tokens.get(i);
                break;
            }
        }

        QueryOperatorToken token = defaultOperatator == QueryOperator.AND ? QueryOperatorToken.AND : QueryOperatorToken.OR;

        if (lastWhere != null && lastWhere.getSearchOperator() != null) {
            token = QueryOperatorToken.OR; // default to OR operator after search if AND was not specified explicitly
        }

        tokens.add(token);
    }

    private Collection<Object> transformCollection(String fieldName, Collection<Object> values) {
        List<Object> result = new ArrayList<>();
        for (Object value : values) {
            if (value instanceof Collection) {
                for (Object transformedValue : transformCollection(fieldName, (Collection)value)) {
                    result.add(transformedValue);
                }
            } else {
                WhereParams nestedWhereParams = new WhereParams();
                nestedWhereParams.setAllowWildcards(true);
                nestedWhereParams.setFieldName(fieldName);
                nestedWhereParams.setValue(value);

                result.add(transformValue(nestedWhereParams));
            }
        }
        return result;
    }

    private void negateIfNeeded(String fieldName) {
        if (!negate) {
            return;
        }

        negate = false;

        if (whereTokens.isEmpty() || whereTokens.get(whereTokens.size() - 1) instanceof OpenSubclauseToken) {
            if (fieldName != null) {
                _whereExists(fieldName);
            } else {
                _whereTrue();
            }
            _andAlso();
        }

        whereTokens.add(NegateToken.INSTANCE);
    }

    private static Collection<Object> unpackCollection(Collection items) {
        List<Object> results = new ArrayList<>();

        for (Object item : items) {
            if (item instanceof Collection) {
                for (Object nested : unpackCollection((Collection)item)) {
                    results.add(nested);
                }
            } else {
                results.add(item);
            }
        }

        return results;
    }
    private String ensureValidFieldName(String fieldName, boolean isNestedPath) {
        if (theSession == null || theSession.getConventions() == null || isNestedPath || isGroupBy) {
            return QueryFieldUtil.escapeIfNecessary(fieldName);
        }

        for (Class rootType : rootTypes) {
            Field identityProperty = theSession.getConventions().getIdentityProperty(rootType);
            if (identityProperty != null && identityProperty.getName().equals(fieldName)) { //TODO: verify casing
                return Constants.Documents.Indexing.Fields.DOCUMENT_ID_FIELD_NAME;
            }
        }

        return QueryFieldUtil.escapeIfNecessary(fieldName);
    }

    private Object transformValue(WhereParams whereParams) {
        return transformValue(whereParams, false);
    }

    private Object transformValue(WhereParams whereParams, boolean forRange) {
        if (whereParams.getValue() == null) {
            return null;
        }

        if ("".equals(whereParams.getValue())) {
            return "";
        }

        Class<?> clazz = whereParams.getValue().getClass();
        if (Date.class.equals(clazz)) {
            return whereParams.getValue();
        }

        if (String.class.equals(clazz)) {
            return whereParams.getValue();
        }

        if (Integer.class.equals(clazz)) {
            return whereParams.getValue();
        }

        if (Long.class.equals(clazz)) {
            return whereParams.getValue();
        }

        if (Float.class.equals(clazz)) {
            return whereParams.getValue();
        }

        if (Double.class.equals(clazz)) {
            return whereParams.getValue();
        }

        //TODO timespan - duration ?

        if (String.class.equals(clazz)) {
            return whereParams.getValue();
        }

        if (Boolean.class.equals(clazz)) {
            return whereParams.getValue();
        }

        if (clazz.isEnum()) {
            return whereParams.getValue();
        }

        /* TODO

            if (_conventions.TryConvertValueForQuery(whereParams.FieldName, whereParams.Value, forRange, out var strVal))
                return strVal;

         */
        return whereParams.getValue();

    }

    private String addQueryParameter(Object value) {
        String parameterName = "p" + queryParameters.size();
        queryParameters.put(parameterName, value);
        return parameterName;
    }

    protected void updateFieldsToFetchToken(FieldsToFetchToken fieldsToFetch) {
        this.fieldsToFetchToken = fieldsToFetch;

        if (selectTokens.isEmpty()) {
            selectTokens.add(fieldsToFetch);
        } else {
            Optional<QueryToken> fetchToken = selectTokens.stream()
                    .filter(x -> x instanceof FieldsToFetchToken)
                    .findFirst();

            if (fetchToken.isPresent()) {
                int idx = selectTokens.indexOf(fetchToken.get());
                selectTokens.set(idx, fieldsToFetch);
            } else {
                selectTokens.add(fieldsToFetch);
            }
        }
    }

    protected List<Consumer<IndexQuery>> beforeQueryExecutedCallback = new ArrayList<>();

    protected List<Consumer<QueryResult>> afterQueryExecutedCallback = new ArrayList<>();

    //TBD protected Action<BlittableJsonReaderObject> AfterStreamExecutedCallback;

    protected QueryOperation queryOperation;

    public QueryOperation getQueryOperation() {
        return queryOperation;
    }

    public void _addBeforeQueryExecutedListener(Consumer<IndexQuery> action) {
        beforeQueryExecutedCallback.add(action);
    }

    public void _removeBeforeQueryExecutedListener(Consumer<IndexQuery> action) {
        beforeQueryExecutedCallback.remove(action);
    }

    public void _addAfterQueryExecutedListener(Consumer<QueryResult> action) {
        afterQueryExecutedCallback.add(action);
    }

    public void _removeAfterQueryExecutedListener(Consumer<QueryResult> action) {
        afterQueryExecutedCallback.remove(action);
    }

    //TBD public IDocumentQueryCustomization AfterStreamExecuted(Action<BlittableJsonReaderObject> action)

    public void _noTracking() {
        disableEntitiesTracking = true;
    }

    public void _noCaching() {
        disableCaching = true;
    }

    public void _showTimings() {
        showQueryTimings = true;
    }

    /*TODO
        IDocumentQueryCustomization IDocumentQueryCustomization.RandomOrdering()
        {
            RandomOrdering();
            return this;
        }

        IDocumentQueryCustomization IDocumentQueryCustomization.RandomOrdering(string seed)
        {
            RandomOrdering(seed);
            return this;
        }

        IDocumentQueryCustomization IDocumentQueryCustomization.CustomSortUsing(string typeName)
        {
            CustomSortUsing(typeName, false);
            return this;
        }

        IDocumentQueryCustomization IDocumentQueryCustomization.CustomSortUsing(string typeName, bool descending)
        {
            CustomSortUsing(typeName, descending);
            return this;
        }

        /// <inheritdoc />
        IDocumentQueryCustomization IDocumentQueryCustomization.WaitForNonStaleResults(TimeSpan waitTimeout)
        {
            WaitForNonStaleResults(waitTimeout);
            return this;
        }

        /// <inheritdoc />
        IDocumentQueryCustomization IDocumentQueryCustomization.WaitForNonStaleResults()
        {
            WaitForNonStaleResults();
            return this;
        }

        /// <inheritdoc />
        IDocumentQueryCustomization IDocumentQueryCustomization.WaitForNonStaleResultsAsOf(long cutOffEtag)
        {
            WaitForNonStaleResultsAsOf(cutOffEtag);
            return this;
        }

        /// <inheritdoc />
        IDocumentQueryCustomization IDocumentQueryCustomization.WaitForNonStaleResultsAsOf(long cutOffEtag, TimeSpan waitTimeout)
        {
            WaitForNonStaleResultsAsOf(cutOffEtag, waitTimeout);
            return this;
        }

        */

    //TBD protected List<HighlightedField> HighlightedFields = new List<HighlightedField>();
    //TBD protected string[] HighlighterPreTags = new string[0];
    //TBD protected string[] HighlighterPostTags = new string[0];
    //TBD protected string HighlighterKeyName;
    //TBD protected QueryHighlightings Highlightings = new QueryHighlightings();
    //TBD public void SetHighlighterTags(string preTag, string postTag)
    //TBD public void Highlight(string fieldName, int fragmentLength, int fragmentCount, string fragmentsField)
    //TBD public void Highlight(string fieldName, int fragmentLength, int fragmentCount, out FieldHighlightings fieldHighlightings)
    //TBD public void Highlight(string fieldName, string fieldKeyName, int fragmentLength, int fragmentCount, out FieldHighlightings fieldHighlightings)
    //TBD public void SetHighlighterTags(string[] preTags, string[] postTags)

    protected void _withinRadiusOf(String fieldName, double radius, double latitude, double longitude, SpatialUnits radiusUnits, double distErrorPercent) {
        fieldName = ensureValidFieldName(fieldName, false);

        appendOperatorIfNeeded(whereTokens);
        negateIfNeeded(fieldName);

        whereTokens.add(WhereToken.within(fieldName, ShapeToken.circle(addQueryParameter(radius), addQueryParameter(latitude), addQueryParameter(longitude), radiusUnits), distErrorPercent));
    }

    protected void _spatial(String fieldName, String shapeWKT, SpatialRelation relation, double distErrorPercent) {
        fieldName = ensureValidFieldName(fieldName, false);

        appendOperatorIfNeeded(whereTokens);
        negateIfNeeded(fieldName);

        ShapeToken wktToken = ShapeToken.wkt(addQueryParameter(shapeWKT));

        QueryToken relationToken;
        switch (relation) {
            case WITHIN:
                relationToken = WhereToken.within(fieldName, wktToken, distErrorPercent);
                break;
            case CONTAINS:
                relationToken = WhereToken.contains(fieldName, wktToken, distErrorPercent);
                break;
            case DISJOINT:
                relationToken = WhereToken.disjoint(fieldName, wktToken, distErrorPercent);
                break;
            case INTERSECTS:
                relationToken = WhereToken.intersects(fieldName, wktToken, distErrorPercent);
                break;
            default:
                throw new IllegalArgumentException();
        }

        whereTokens.add(relationToken);
    }

    @Override
    public void _spatial(SpatialDynamicField dynamicField, SpatialCriteria criteria) {
        appendOperatorIfNeeded(whereTokens);
        negateIfNeeded(null);

        whereTokens.add(criteria.toQueryToken(dynamicField.toField(this::ensureValidFieldName), this::addQueryParameter));
    }

    @Override
    public void _spatial(String fieldName, SpatialCriteria criteria) {
        fieldName = ensureValidFieldName(fieldName, false);

        appendOperatorIfNeeded(whereTokens);
        negateIfNeeded(fieldName);

        whereTokens.add(criteria.toQueryToken(fieldName, this::addQueryParameter));
    }

    @Override
    public void _orderByDistance(String fieldName, double latitude, double longitude) {
        orderByTokens.add(OrderByToken.createDistanceAscending(fieldName, addQueryParameter(latitude), addQueryParameter(longitude)));
    }

    @Override
    public void _orderByDistance(String fieldName, String shapeWkt) {
        orderByTokens.add(OrderByToken.createDistanceAscending(fieldName, addQueryParameter(shapeWkt)));
    }

    public void _orderByDistanceDescending(String fieldName, double latitude, double longitude) {
        orderByTokens.add(OrderByToken.createDistanceDescending(fieldName, addQueryParameter(latitude), addQueryParameter(longitude)));
    }

    public void _orderByDistanceDescending(String fieldName, String shapeWkt) {
        orderByTokens.add(OrderByToken.createDistanceDescending(fieldName, addQueryParameter(shapeWkt)));
    }

    protected void initSync() {
        if (queryOperation != null) {
            return;
        }
        /* TODO
            var beforeQueryExecutedEventArgs = new BeforeQueryExecutedEventArgs(TheSession, this);
            TheSession.OnBeforeQueryExecutedInvoke(beforeQueryExecutedEventArgs);
            */

        queryOperation = initializeQueryOperation();
        executeActualQuery();
    }

    private void executeActualQuery() {
        try (CleanCloseable context = queryOperation.enterQueryContext()) {
            queryOperation.logQuery();

            QueryCommand command = queryOperation.createRequest();
            theSession.getRequestExecutor().execute(command);
            queryOperation.setResult(command.getResult());
        }
        invokeAfterQueryExecuted(queryOperation.getCurrentQueryResults());
    }

    @Override
    public Iterator<T> iterator() {
        initSync();

        return queryOperation.complete(clazz).iterator();
    }

    public List<T> toList() {
        return EnumerableUtils.toList(iterator());
    }

    public QueryResult getQueryResult() {
        initSync();

        return queryOperation.getCurrentQueryResults().createSnapshot();
    }

    public T first() {
        Collection<T> result = executeQueryOperation(1);
        return result.isEmpty() ? null : result.stream().findFirst().get();
    }

    public T firstOrDefault() {
        Collection<T> result = executeQueryOperation(1);
        return result.stream().findFirst().orElseGet(() -> Defaults.defaultValue(clazz));
    }

    public T single() {
        Collection<T> result = executeQueryOperation(2);
        if (result.size() > 1) {
            throw new IllegalStateException("Expected single result, got: " + result.size());
        }
        return result.stream().findFirst().orElse(null);
    }

    public T singleOrDefault() {
        Collection<T> result = executeQueryOperation(2);
        if (result.size() > 1) {
            throw new IllegalStateException("Expected single result, got: " + result.size());
        }
        if (result.isEmpty()) {
            return Defaults.defaultValue(clazz);
        }
        return result.stream().findFirst().get();
    }

    public int count() {
        _take(0);
        QueryResult queryResult = getQueryResult();
        return queryResult.getTotalResults();
    }

    private Collection<T> executeQueryOperation(int take) {
        if (pageSize == null || pageSize > take) {
            _take(take);
        }

        initSync();

        return queryOperation.complete(clazz);
    }
}
