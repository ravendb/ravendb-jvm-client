package net.ravendb.client.documents.session;

import jdk.nashorn.internal.objects.annotations.Where;
import net.ravendb.client.Parameters;
import net.ravendb.client.documents.commands.QueryCommand;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.queries.IndexQuery;
import net.ravendb.client.documents.queries.QueryFieldUtil;
import net.ravendb.client.documents.queries.QueryOperator;
import net.ravendb.client.documents.session.operations.QueryOperation;
import net.ravendb.client.documents.session.tokens.*;
import net.ravendb.client.primitives.CleanCloseable;
import net.ravendb.client.primitives.Tuple;

import javax.management.Query;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * A query against a Raven index
 */
public class AbstractDocumentQuery<T, TSelf extends AbstractDocumentQuery<T, TSelf>> implements IDocumentQueryCustomization, IAbstractDocumentQuery<T> {

    protected Class<T> clazz;

    /* TODO
      private readonly Dictionary<string, string> _aliasToGroupByFieldName = new Dictionary<string, string>();
*/

    protected QueryOperator defaultOperatator = QueryOperator.AND;

    /* TODO

        private readonly LinqPathProvider _linqPathProvider;

        protected readonly HashSet<Type> RootTypes = new HashSet<Type>
        {
            typeof (T)
        };

        private static Dictionary<Type, Func<object, string>> _implicitStringsCache = new Dictionary<Type, Func<object, string>>();

        */

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
    /* TODO
        protected readonly DeclareToken DeclareToken;

        protected readonly List<LoadToken> LoadTokens;

        protected internal FieldsToFetchToken FieldsToFetchToken;
*/
    protected List<QueryToken> whereTokens = new LinkedList<>();

    protected List<QueryToken> groupByTokens = new LinkedList<>();

    protected List<QueryToken> orderByTokens = new LinkedList<>();
    /* TODO
        /// <summary>
        ///   which record to start reading from
        /// </summary>
        protected int Start;*/

    private final DocumentConventions _conventions;

    /*TODO

        /// <summary>
        /// Timeout for this query
        /// </summary>
        protected TimeSpan? Timeout;
        /// <summary>
        /// Should we wait for non stale results
        /// </summary>
        protected bool TheWaitForNonStaleResults;
        /// <summary>
        /// The paths to include when loading the query
        /// </summary>
        protected HashSet<string> Includes = new HashSet<string>();

        /// <summary>
        /// Holds the query stats
        /// </summary>
        protected QueryStatistics QueryStats = new QueryStatistics();

        /// <summary>
        /// Determines if entities should be tracked and kept in memory
        /// </summary>
        protected bool DisableEntitiesTracking;

        /// <summary>
        /// Determine if query results should be cached.
        /// </summary>
        protected bool DisableCaching;

        /// <summary>
        /// Indicates if detailed timings should be calculated for various query parts (Lucene search, loading documents, transforming results). Default: false
        /// </summary>
        protected bool ShowQueryTimings;

        /// <summary>
        /// Determine if scores of query results should be explained
        /// </summary>
        protected bool ShouldExplainScores;

        public bool IsDistinct => SelectTokens.First?.Value is DistinctToken;

        /// <summary>
        /// Gets the document convention from the query session
        /// </summary>
        public DocumentConventions Conventions => _conventions;

        /// <summary>
        ///   Gets the session associated with this document query
        /// </summary>
        public IDocumentSession Session => (IDocumentSession)TheSession;
        public IAsyncDocumentSession AsyncSession => (IAsyncDocumentSession)TheSession;

        public bool IsDynamicMapReduce => GroupByTokens.Count > 0;

        protected long? CutoffEtag;

        private static TimeSpan DefaultTimeout
        {
            get
            {
                if (Debugger.IsAttached) // increase timeout if we are debugging
                    return TimeSpan.FromMinutes(15);

                return TimeSpan.FromSeconds(15);
            }
        }
*/

    protected AbstractDocumentQuery(Class<T> clazz, InMemoryDocumentSessionOperations session, String indexName,
                                    String collectionName, boolean isGroupBy) { // TODO:  DeclareToken declareToken,         List<LoadToken> loadTokens,         string fromAlias = null)
        this.clazz = clazz;
        //TODO:IsGroupBy = isGroupBy;

        this.indexName = indexName;
        this.collectionName = collectionName;
        this.fromToken = FromToken.create(indexName, collectionName, null); //TODO: from alias as 3-rd param
        /* TODO

            DeclareToken = declareToken;

            LoadTokens = loadTokens;
*/
        theSession = session;
        //TODO: AfterQueryExecuted(UpdateStatsAndHighlightings);
        _conventions = session == null ? new DocumentConventions() : session.getConventions();
        /* TODO

            _linqPathProvider = new LinqPathProvider(_conventions);
         */
    }

    /* TODO

        #region TSelf Members

        public void UsingDefaultOperator(QueryOperator @operator)
        {
            if (WhereTokens.Count != 0)
                throw new InvalidOperationException("Default operator can only be set before any where clause is added.");

            DefaultOperator = @operator;
        }

        /// <summary>
        ///   Instruct the query to wait for non stale result for the specified wait timeout.
        /// </summary>
        /// <param name = "waitTimeout">The wait timeout.</param>
        /// <returns></returns>
        public void WaitForNonStaleResults(TimeSpan waitTimeout)
        {
            TheWaitForNonStaleResults = true;
            CutoffEtag = null;
            Timeout = waitTimeout;
        }
        */

    protected QueryOperation initializeQueryOperation() {
        IndexQuery indexQuery = getIndexQuery();

        return new QueryOperation(theSession, indexName, indexQuery, null, false, null, false, false, false);
        //TODO: pass  FieldsToFetchToken?.Projections,         TheWaitForNonStaleResults,                  Timeout,                DisableEntitiesTracking
    }

    public IndexQuery getIndexQuery() {
        String query = toString();
        IndexQuery indexQuery = generateIndexQuery(query);
        //TODO: BeforeQueryExecutedCallback?.Invoke(indexQuery);
        return indexQuery;
    }

    /* TODO
        /// <summary>
        ///   Gets the fields for projection
        /// </summary>
        /// <returns></returns>
        public IEnumerable<string> GetProjectionFields()
        {
            return FieldsToFetchToken?.Projections ?? Enumerable.Empty<string>();
        }

        /// <summary>
        /// Order the search results randomly
        /// </summary>
        public void RandomOrdering()
        {
            AssertNoRawQuery();
            OrderByTokens.AddLast(OrderByToken.Random);
        }

        /// <summary>
        /// Order the search results randomly using the specified seed
        /// this is useful if you want to have repeatable random queries
        /// </summary>
        public void RandomOrdering(string seed)
        {
            AssertNoRawQuery();
            if (string.IsNullOrWhiteSpace(seed))
            {
                RandomOrdering();
                return;
            }
            OrderByTokens.AddLast(OrderByToken.CreateRandom(seed));
        }

        public void CustomSortUsing(string typeName, bool descending)
        {
            if (descending)
            {
                OrderByDescending(Constants.Documents.Indexing.Fields.CustomSortFieldName + ";" + typeName);
                return;
            }

            OrderBy(Constants.Documents.Indexing.Fields.CustomSortFieldName + ";" + typeName);
        }

        internal void AddGroupByAlias(string fieldName, string projectedName)
        {
            _aliasToGroupByFieldName[projectedName] = fieldName;
        }
*/

    private void assertNoRawQuery() {
        /* TODO
         if (QueryRaw != null)
                throw new InvalidOperationException(
                    "RawQuery was called, cannot modify this query by calling on operations that would modify the query (such as Where, Select, OrderBy, GroupBy, etc)");
         */
    }
    /*

        public void RawQuery(string query)
        {
            if (SelectTokens.Count != 0 ||
                WhereTokens.Count != 0 ||
                OrderByTokens.Count != 0 ||
                GroupByTokens.Count != 0)
                throw new InvalidOperationException("You can only use RawQuery on a new query, without applying any operations (such as Where, Select, OrderBy, GroupBy, etc)");
            QueryRaw = query;
        }

        public void AddParameter(string name, object value)
        {
            if (QueryParameters.ContainsKey(name))
                throw new InvalidOperationException("The parameter " + name + " was already added");
            QueryParameters[name] = value;
        }

        public void GroupBy(string fieldName, params string[] fieldNames)
        {
            if (FromToken.IsDynamic == false)
                throw new InvalidOperationException("GroupBy only works with dynamic queries.");
            AssertNoRawQuery();
            IsGroupBy = true;

            fieldName = EnsureValidFieldName(fieldName, isNestedPath: false);

            GroupByTokens.AddLast(GroupByToken.Create(fieldName));

            if (fieldNames == null || fieldNames.Length <= 0)
                return;

            foreach (var name in fieldNames)
            {
                fieldName = EnsureValidFieldName(name, isNestedPath: false);

                GroupByTokens.AddLast(GroupByToken.Create(fieldName));
            }
        }

        public void GroupByKey(string fieldName = null, string projectedName = null)
        {
            AssertNoRawQuery();
            IsGroupBy = true;

            if (projectedName != null && _aliasToGroupByFieldName.TryGetValue(projectedName, out var aliasedFieldName))
            {
                if (fieldName == null || fieldName.Equals(projectedName, StringComparison.Ordinal))
                    fieldName = aliasedFieldName;
            }

            SelectTokens.AddLast(GroupByKeyToken.Create(fieldName, projectedName));
        }

        public void GroupBySum(string fieldName, string projectedName = null)
        {
            AssertNoRawQuery();
            IsGroupBy = true;

            fieldName = EnsureValidFieldName(fieldName, isNestedPath: false);

            SelectTokens.AddLast(GroupBySumToken.Create(fieldName, projectedName));
        }

        public void GroupByCount(string projectedName = null)
        {
            AssertNoRawQuery();
            IsGroupBy = true;

            SelectTokens.AddLast(GroupByCountToken.Create(projectedName));
        }

*/
    protected void whereTrue() {
        appendOperatorIfNeeded(whereTokens);
        negateIfNeeded(null);

        whereTokens.add(TrueToken.INSTANCE);
    }
    /* TODO;

        /// <summary>
        ///   Includes the specified path in the query, loading the document specified in that path
        /// </summary>
        /// <param name = "path">The path.</param>
        public void Include(string path)
        {
            Includes.Add(path);
        }

        /// <summary>
        ///   This function exists solely to forbid in memory where clause on IDocumentQuery, because
        ///   that is nearly always a mistake.
        /// </summary>
        [Obsolete(
            @"
You cannot issue an in memory filter - such as Where(x=>x.Name == ""Ayende"") - on IDocumentQuery.
This is likely a bug, because this will execute the filter in memory, rather than in RavenDB.
Consider using session.Query<T>() instead of session.Advanced.DocumentQuery<T>. The session.Query<T>() method fully supports Linq queries, while session.Advanced.DocumentQuery<T>() is intended for lower level API access.
If you really want to do in memory filtering on the data returned from the query, you can use: session.Advanced.DocumentQuery<T>().ToList().Where(x=>x.Name == ""Ayende"")
"
            , true)]
        public IEnumerable<T> Where(Func<T, bool> predicate)
        {
            throw new NotSupportedException();
        }


        /// <summary>
        ///   This function exists solely to forbid in memory where clause on IDocumentQuery, because
        ///   that is nearly always a mistake.
        /// </summary>
        [Obsolete(
            @"
You cannot issue an in memory filter - such as Count(x=>x.Name == ""Ayende"") - on IDocumentQuery.
This is likely a bug, because this will execute the filter in memory, rather than in RavenDB.
Consider using session.Query<T>() instead of session.Advanced.DocumentQuery<T>. The session.Query<T>() method fully supports Linq queries, while session.Advanced.DocumentQuery<T>() is intended for lower level API access.
If you really want to do in memory filtering on the data returned from the query, you can use: session.Advanced.DocumentQuery<T>().ToList().Count(x=>x.Name == ""Ayende"")
"
            , true)]
        public int Count(Func<T, bool> predicate)
        {
            throw new NotSupportedException();
        }

        /// <summary>
        ///   Includes the specified path in the query, loading the document specified in that path
        /// </summary>
        /// <param name = "path">The path.</param>
        public void Include(Expression<Func<T, object>> path)
        {
            Include(path.ToPropertyPath());
        }


*/

    protected void _take(int count) {
        pageSize = count;
    }

    protected void _skip(int count) {
        //tODO:  Start = count;
    }

    /*


        /// <summary>
        ///   Filter the results from the index using the specified where clause.
        /// </summary>
        public void WhereLucene(string fieldName, string whereClause)
        {
            fieldName = EnsureValidFieldName(fieldName, isNestedPath: false);

            AppendOperatorIfNeeded(WhereTokens);
            NegateIfNeeded(fieldName);

            WhereTokens.AddLast(WhereToken.Lucene(fieldName, AddQueryParameter(whereClause)));
        }

        /// <summary>
        ///   Simplified method for opening a new clause within the query
        /// </summary>
        /// <returns></returns>
        public void OpenSubclause()
        {
            _currentClauseDepth++;

            AppendOperatorIfNeeded(WhereTokens);
            NegateIfNeeded(null);

            WhereTokens.AddLast(OpenSubclauseToken.Instance);
        }

        /// <summary>
        ///   Simplified method for closing a clause within the query
        /// </summary>
        /// <returns></returns>
        public void CloseSubclause()
        {
            _currentClauseDepth--;

            WhereTokens.AddLast(CloseSubclauseToken.Instance);
        }

*/
    protected void _whereEquals(String fieldName, Object value, boolean exact) {
        WhereParams params = new WhereParams();
        params.setFieldName(fieldName);
        params.setValue(value);
        params.setExact(exact);
        _whereEquals(params);
    }

    protected void _whereEquals(WhereParams whereParams) {
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

    protected void _whereNotEquals(String fieldName, Object value) {
        _whereNotEquals(fieldName, value, false);
    }

    protected void _whereNotEquals(String fieldName, Object value, boolean exact) {
        WhereParams params = new WhereParams();
        params.setFieldName(fieldName);
        params.setValue(value);
        params.setExact(exact);

        _whereNotEquals(params);
    }

    protected void _whereNotEquals(WhereParams whereParams) {
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

    protected void negateNext() {
        negate = !negate;
    }

    /* TODO

        /// <summary>
        /// Check that the field has one of the specified value
        /// </summary>
        public void WhereIn(string fieldName, IEnumerable<object> values, bool exact = false)
        {
            fieldName = EnsureValidFieldName(fieldName, isNestedPath: false);

            AppendOperatorIfNeeded(WhereTokens);
            NegateIfNeeded(fieldName);

            WhereTokens.AddLast(WhereToken.In(fieldName, AddQueryParameter(TransformEnumerable(fieldName, UnpackEnumerable(values)).ToArray()), exact));
        }

*/
    protected void _whereStartsWith(String fieldName, Object value) {
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
    protected void _whereEndsWith(String fieldName, Object value) {
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

    /* TODO:

        /// <summary>
        ///   Matches fields where the value is between the specified start and end, exclusive
        /// </summary>
        /// <param name = "fieldName">Name of the field.</param>
        /// <param name = "start">The start.</param>
        /// <param name = "end">The end.</param>
        /// <returns></returns>
        public void WhereBetween(string fieldName, object start, object end, bool exact = false)
        {
            fieldName = EnsureValidFieldName(fieldName, isNestedPath: false);

            AppendOperatorIfNeeded(WhereTokens);
            NegateIfNeeded(fieldName);

            var fromParameterName = AddQueryParameter(start == null ? "*" : TransformValue(new WhereParams { Value = start, FieldName = fieldName }, forRange: true));
            var toParameterName = AddQueryParameter(end == null ? "NULL" : TransformValue(new WhereParams { Value = end, FieldName = fieldName }, forRange: true));

            WhereTokens.AddLast(WhereToken.Between(fieldName, fromParameterName, toParameterName, exact));
        }

        /// <summary>
        ///   Matches fields where the value is greater than the specified value
        /// </summary>
        /// <param name = "fieldName">Name of the field.</param>
        /// <param name = "value">The value.</param>
        public void WhereGreaterThan(string fieldName, object value, bool exact = false)
        {
            fieldName = EnsureValidFieldName(fieldName, isNestedPath: false);

            AppendOperatorIfNeeded(WhereTokens);
            NegateIfNeeded(fieldName);

            WhereTokens.AddLast(WhereToken.GreaterThan(fieldName, AddQueryParameter(value == null ? "*" : TransformValue(new WhereParams { Value = value, FieldName = fieldName }, forRange: true)), exact));
        }

        /// <summary>
        ///   Matches fields where the value is greater than or equal to the specified value
        /// </summary>
        /// <param name = "fieldName">Name of the field.</param>
        /// <param name = "value">The value.</param>
        public void WhereGreaterThanOrEqual(string fieldName, object value, bool exact = false)
        {
            fieldName = EnsureValidFieldName(fieldName, isNestedPath: false);

            AppendOperatorIfNeeded(WhereTokens);
            NegateIfNeeded(fieldName);

            WhereTokens.AddLast(WhereToken.GreaterThanOrEqual(fieldName, AddQueryParameter(value == null ? "*" : TransformValue(new WhereParams { Value = value, FieldName = fieldName }, forRange: true)), exact));
        }

        /// <summary>
        ///   Matches fields where the value is less than the specified value
        /// </summary>
        /// <param name = "fieldName">Name of the field.</param>
        /// <param name = "value">The value.</param>
        public void WhereLessThan(string fieldName, object value, bool exact = false)
        {
            fieldName = EnsureValidFieldName(fieldName, isNestedPath: false);

            AppendOperatorIfNeeded(WhereTokens);
            NegateIfNeeded(fieldName);

            WhereTokens.AddLast(WhereToken.LessThan(fieldName, AddQueryParameter(value == null ? "NULL" : TransformValue(new WhereParams { Value = value, FieldName = fieldName }, forRange: true)), exact));
        }

        /// <summary>
        ///   Matches fields where the value is less than or equal to the specified value
        /// </summary>
        /// <param name = "fieldName">Name of the field.</param>
        /// <param name = "value">The value.</param>
        public void WhereLessThanOrEqual(string fieldName, object value, bool exact = false)
        {
            AppendOperatorIfNeeded(WhereTokens);
            NegateIfNeeded(fieldName);

            WhereTokens.AddLast(WhereToken.LessThanOrEqual(fieldName, AddQueryParameter(value == null ? "NULL" : TransformValue(new WhereParams { Value = value, FieldName = fieldName }, forRange: true)), exact));
        }

    */
    protected void _andAlso() {
        if (whereTokens.isEmpty()) {
            return;
        }

        if (whereTokens.get(whereTokens.size() - 1) instanceof QueryOperatorToken) {
            throw new IllegalStateException("Cannot add AND, previous token was already an operator token.");
        }

        whereTokens.add(QueryOperatorToken.AND);

    }
    /* TODO

        /// <summary>
        ///   Add an OR to the query
        /// </summary>
        public void OrElse()
        {
            if (WhereTokens.Last == null)
                return;

            if (WhereTokens.Last.Value is QueryOperatorToken)
                throw new InvalidOperationException("Cannot add OR, previous token was already an operator token.");

            WhereTokens.AddLast(QueryOperatorToken.Or);
        }

        /// <summary>
        ///   Specifies a boost weight to the last where clause.
        ///   The higher the boost factor, the more relevant the term will be.
        /// </summary>
        /// <param name = "boost">boosting factor where 1.0 is default, less than 1.0 is lower weight, greater than 1.0 is higher weight</param>
        /// <returns></returns>
        /// <remarks>
        ///   http://lucene.apache.org/java/2_4_0/queryparsersyntax.html#Boosting%20a%20Term
        /// </remarks>
        public void Boost(decimal boost)
        {
            if (boost == 1m) // 1.0 is the default
                return;

            var whereToken = WhereTokens.Last?.Value as WhereToken;
            if (whereToken == null)
                throw new InvalidOperationException("Missing where clause");

            if (boost <= 0m)
                throw new ArgumentOutOfRangeException(nameof(boost), "Boost factor must be a positive number");

            whereToken.Boost = boost;
        }

        /// <summary>
        ///   Specifies a fuzziness factor to the single word term in the last where clause
        /// </summary>
        /// <param name = "fuzzy">0.0 to 1.0 where 1.0 means closer match</param>
        /// <returns></returns>
        /// <remarks>
        ///   http://lucene.apache.org/java/2_4_0/queryparsersyntax.html#Fuzzy%20Searches
        /// </remarks>
        public void Fuzzy(decimal fuzzy)
        {
            var whereToken = WhereTokens.Last?.Value as WhereToken;
            if (whereToken == null)
            {
                throw new InvalidOperationException("Missing where clause");
            }

            if (fuzzy < 0m || fuzzy > 1m)
            {
                throw new ArgumentOutOfRangeException(nameof(fuzzy), "Fuzzy distance must be between 0.0 and 1.0");
            }

            //var ch = QueryText[QueryText.Length - 1]; // TODO [ppekrol]
            //if (ch == '"' || ch == ']')
            //{
            //    // this check is overly simplistic
            //    throw new InvalidOperationException("Fuzzy factor can only modify single word terms");
            //}

            whereToken.Fuzzy = fuzzy;
        }

        /// <summary>
        ///   Specifies a proximity distance for the phrase in the last where clause
        /// </summary>
        /// <param name = "proximity">number of words within</param>
        /// <returns></returns>
        /// <remarks>
        ///   http://lucene.apache.org/java/2_4_0/queryparsersyntax.html#Proximity%20Searches
        /// </remarks>
        public void Proximity(int proximity)
        {
            var whereToken = WhereTokens.Last?.Value as WhereToken;
            if (whereToken == null)
            {
                throw new InvalidOperationException("Missing where clause");
            }

            if (proximity < 1)
            {
                throw new ArgumentOutOfRangeException(nameof(proximity), "Proximity distance must be a positive number");
            }

            //if (QueryText[QueryText.Length - 1] != '"') // TODO [ppekrol]
            //{
            //    // this check is overly simplistic
            //    throw new InvalidOperationException("Proximity distance can only modify a phrase");
            //}

            whereToken.Proximity = proximity;
        }

        /// <summary>
        ///   Order the results by the specified fields
        ///   The fields are the names of the fields to sort, defaulting to sorting by ascending.
        ///   You can prefix a field name with '-' to indicate sorting by descending or '+' to sort by ascending
        /// </summary>
        public void OrderBy(string field, OrderingType ordering = OrderingType.String)
        {
            AssertNoRawQuery();
            var f = EnsureValidFieldName(field, isNestedPath: false);
            OrderByTokens.AddLast(OrderByToken.CreateAscending(f, ordering));
        }

        /// <summary>
        ///   Order the results by the specified fields
        ///   The fields are the names of the fields to sort, defaulting to sorting by descending.
        ///   You can prefix a field name with '-' to indicate sorting by descending or '+' to sort by ascending
        /// </summary>
        /// <param name = "fields">The fields.</param>
        public void OrderByDescending(string field, OrderingType ordering = OrderingType.String)
        {
            AssertNoRawQuery();
            var f = EnsureValidFieldName(field, isNestedPath: false);
            OrderByTokens.AddLast(OrderByToken.CreateDescending(f, ordering));
        }

        public void OrderByScore()
        {
            AssertNoRawQuery();
            OrderByTokens.AddLast(OrderByToken.ScoreAscending);
        }

        public void OrderByScoreDescending()
        {
            AssertNoRawQuery();
            OrderByTokens.AddLast(OrderByToken.ScoreDescending);
        }

        /// <summary>
        /// Instructs the query to wait for non stale results as of the cutoff etag.
        /// </summary>
        public void WaitForNonStaleResultsAsOf(long cutOffEtag)
        {
            WaitForNonStaleResultsAsOf(cutOffEtag, DefaultTimeout);
        }

        /// <summary>
        /// Instructs the query to wait for non stale results as of the cutoff etag.
        /// </summary>
        public void WaitForNonStaleResultsAsOf(long cutOffEtag, TimeSpan waitTimeout)
        {
            TheWaitForNonStaleResults = true;
            Timeout = waitTimeout;
            CutoffEtag = cutOffEtag;
        }

        /// <summary>
        ///   EXPERT ONLY: Instructs the query to wait for non stale results.
        ///   This shouldn't be used outside of unit tests unless you are well aware of the implications
        /// </summary>
        public void WaitForNonStaleResults()
        {
            WaitForNonStaleResults(DefaultTimeout);
        }

        /// <summary>
        /// Provide statistics about the query, such as total count of matching records
        /// </summary>
        public void Statistics(out QueryStatistics stats)
        {
            stats = QueryStats;
        }

        /// <summary>
        /// Called externally to raise the after query executed callback
        /// </summary>
        public void InvokeAfterQueryExecuted(QueryResult result)
        {
            AfterQueryExecutedCallback?.Invoke(result);
        }

        /// <summary>
        /// Called externally to raise the after stream executed callback
        /// </summary>
        public void InvokeAfterStreamExecuted(BlittableJsonReaderObject result)
        {
            AfterStreamExecutedCallback?.Invoke(result);
        }

        #endregion

*/

    /**
     * Generates the index query.
     */
    protected IndexQuery generateIndexQuery(String query) {
        IndexQuery indexQuery = new IndexQuery();
        indexQuery.setQuery(query);
        /* TODO
         Start = Start,
                CutoffEtag = CutoffEtag,
                WaitForNonStaleResults = TheWaitForNonStaleResults,
                WaitForNonStaleResultsTimeout = Timeout,
                */
        indexQuery.setQueryParameters(queryParameters);
        /* TODO
                DisableCaching = DisableCaching,
                ShowTimings = ShowQueryTimings,
                ExplainScores = ShouldExplainScores
         */

        if (pageSize != null) {
            indexQuery.setPageSize(pageSize);
        }
        return indexQuery;
    }

    /* TODO

        /// <summary>
        /// Perform a search for documents which fields that match the searchTerms.
        /// If there is more than a single term, each of them will be checked independently.
        /// </summary>
        public void Search(string fieldName, string searchTerms, SearchOperator @operator = SearchOperator.Or)
        {
            var hasWhiteSpace = searchTerms.Any(char.IsWhiteSpace);
            LastEquality = new KeyValuePair<string, object>(fieldName,
                hasWhiteSpace ? "(" + searchTerms + ")" : searchTerms
            );

            AppendOperatorIfNeeded(WhereTokens);

            fieldName = EnsureValidFieldName(fieldName, isNestedPath: false);
            NegateIfNeeded(fieldName);

            WhereTokens.AddLast(WhereToken.Search(fieldName, AddQueryParameter(searchTerms), @operator));
        }

*/

    @Override
    public String toString() {
        if (queryRaw != null) {
            return queryRaw;
        }

        if (_currentClauseDepth != 0) {
            throw new IllegalStateException("A clause was not closed correctly within this query, current clause depth = " + _currentClauseDepth);
        }

        StringBuilder queryText = new StringBuilder();

//TODO        buildDeclare(queryText)
        buildFrom(queryText);
        //TODO: BuildGroupBy(queryText);
        buildWhere(queryText);
        /* TODO
            BuildOrderBy(queryText);
            BuildLoad(queryText);
            BuildSelect(queryText);
            BuildInclude(queryText);

         */

        return queryText.toString();
    }

    /*

        private void BuildInclude(StringBuilder queryText)
        {
            if (Includes == null || Includes.Count == 0)
                return;

            queryText.Append(" INCLUDE ");
            bool first = true;
            foreach (var include in Includes)
            {
                if (first == false)
                    queryText.Append(",");
                first = false;
                var requiredQuotes = false;
                for (int i = 0; i < include.Length; i++)
                {
                    var ch = include[i];
                    if (char.IsLetterOrDigit(ch) == false && ch != '_' && ch != '.')
                    {
                        requiredQuotes = true;
                        break;
                    }
                }
                if (requiredQuotes)
                {
                    queryText.Append("'").Append(include.Replace("'", "\\'")).Append("'");
                }
                else
                {
                    queryText.Append(include);
                }
            }
        }

        /// <summary>
        /// The last term that we asked the query to use equals on
        /// </summary>
        public KeyValuePair<string, object> GetLastEqualityTerm(bool isAsync = false)
        {
            return LastEquality;
        }

        public void Intersect()
        {
            var last = WhereTokens.Last?.Value;
            if (last is WhereToken || last is CloseSubclauseToken)
            {
                IsIntersect = true;

                WhereTokens.AddLast(IntersectMarkerToken.Instance);
            }
            else
                throw new InvalidOperationException("Cannot add INTERSECT at this point.");
        }

*/
    protected void _whereExists(String fieldName) {
        fieldName = ensureValidFieldName(fieldName, false);

        appendOperatorIfNeeded(whereTokens);
        negateIfNeeded(fieldName);

        whereTokens.add(WhereToken.exists(fieldName));
    }
    /*
        public void ContainsAny(string fieldName, IEnumerable<object> values)
        {
            fieldName = EnsureValidFieldName(fieldName, isNestedPath: false);

            AppendOperatorIfNeeded(WhereTokens);
            NegateIfNeeded(fieldName);

            var array = TransformEnumerable(fieldName, UnpackEnumerable(values))
                .ToArray();

            if (array.Length == 0)
            {
                WhereTokens.AddLast(TrueToken.Instance);
                return;
            }

            WhereTokens.AddLast(WhereToken.In(fieldName, AddQueryParameter(array), exact: false));
        }

        public void ContainsAll(string fieldName, IEnumerable<object> values)
        {
            fieldName = EnsureValidFieldName(fieldName, isNestedPath: false);

            AppendOperatorIfNeeded(WhereTokens);
            NegateIfNeeded(fieldName);

            var array = TransformEnumerable(fieldName, UnpackEnumerable(values))
                .ToArray();

            if (array.Length == 0)
            {
                WhereTokens.AddLast(TrueToken.Instance);
                return;
            }

            WhereTokens.AddLast(WhereToken.AllIn(fieldName, AddQueryParameter(array)));
        }

        public void AddRootType(Type type)
        {
            RootTypes.Add(type);
        }

        public string GetMemberQueryPathForOrderBy(Expression expression)
        {
            var memberQueryPath = GetMemberQueryPath(expression);
            return memberQueryPath;
        }

        public string GetMemberQueryPath(Expression expression)
        {
            var result = _linqPathProvider.GetPath(expression);
            result.Path = result.Path.Substring(result.Path.IndexOf('.') + 1);

            if (expression.NodeType == ExpressionType.ArrayLength)
                result.Path += ".Length";

            var propertyName = IndexName == null || FromToken.IsDynamic
                ? _conventions.FindPropertyNameForDynamicIndex(typeof(T), IndexName, "", result.Path)
                : _conventions.FindPropertyNameForIndex(typeof(T), IndexName, "", result.Path);
            return propertyName;
        }

        public void Distinct()
        {
            if (IsDistinct)
                throw new InvalidOperationException("This is already a distinct query.");

            SelectTokens.AddFirst(DistinctToken.Instance);
        }

        private void UpdateStatsAndHighlightings(QueryResult queryResult)
        {
            QueryStats.UpdateQueryStats(queryResult);
            Highlightings.Update(queryResult);
        }

        private void BuildSelect(StringBuilder writer)
        {
            if (SelectTokens.Count == 0)
                return;

            writer
                .Append(" SELECT ");

            var token = SelectTokens.First;
            if (SelectTokens.Count == 1 && token.Value is DistinctToken)
            {
                token.Value.WriteTo(writer);
                writer.Append(" *");

                return;
            }

            while (token != null)
            {
                if (token.Previous != null && token.Previous.Value is DistinctToken == false)
                    writer.Append(",");

                AddSpaceIfNeeded(token.Previous?.Value, token.Value, writer);

                token.Value.WriteTo(writer);

                token = token.Next;
            }
        }
*/
    private void buildFrom(StringBuilder writer) {
        fromToken.writeTo(writer);
    }

    /*
        private void BuildDeclare(StringBuilder writer)
        {
            DeclareToken?.WriteTo(writer);
        }

        private void BuildLoad(StringBuilder writer)
        {
            if (LoadTokens == null || LoadTokens.Count == 0)
                return;

            writer.Append(" LOAD ");

            for (int i = 0; i < LoadTokens.Count; i++)
            {
                if (i != 0)
                    writer.Append(", ");
                LoadTokens[i].WriteTo(writer);
            }
        }

*/
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

    /* TODO
        private void BuildGroupBy(StringBuilder writer)
        {
            if (GroupByTokens.Count == 0)
                return;

            writer
                .Append(" GROUP BY ");

            var token = GroupByTokens.First;
            while (token != null)
            {
                if (token.Previous != null)
                    writer.Append(", ");

                token.Value.WriteTo(writer);

                token = token.Next;
            }
        }

        private void BuildOrderBy(StringBuilder writer)
        {
            if (OrderByTokens.Count == 0)
                return;

            writer
                .Append(" ORDER BY ");

            var token = OrderByTokens.First;
            while (token != null)
            {
                if (token.Previous != null)
                    writer.Append(", ");

                token.Value.WriteTo(writer);

                token = token.Next;
            }

        }
        */
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
    /* TODO
        private void AppendOperatorIfNeeded(LinkedList<QueryToken> tokens)
        {
            var token = DefaultOperator == QueryOperator.And ? QueryOperatorToken.And : QueryOperatorToken.Or;

            if (lastWhere?.SearchOperator != null)
                token = QueryOperatorToken.Or;

            tokens.AddLast(token);
        }

        private IEnumerable<object> TransformEnumerable(string fieldName, IEnumerable<object> values)
        {
            foreach (var value in values)
            {
                var enumerable = value as IEnumerable;
                if (enumerable != null && value is string == false)
                {
                    foreach (var transformedValue in TransformEnumerable(fieldName, enumerable.Cast<object>()))
                        yield return transformedValue;

                    continue;
                }

                var nestedWhereParams = new WhereParams
                {
                    AllowWildcards = true,
                    FieldName = fieldName,
                    Value = value
                };

                yield return TransformValue(nestedWhereParams);
            }
        }
        */

    private void negateIfNeeded(String fieldName) {
        if (!negate) {
            return;
        }

        negate = false;

        if (whereTokens.isEmpty() || whereTokens.get(whereTokens.size() - 1) instanceof OpenSubclauseToken) {
            if (fieldName != null) {
                _whereExists(fieldName);
            } else {
                whereTrue();
            }
            _andAlso();
        }

        whereTokens.add(NegateToken.INSTANCE);
    }

    /* TODO

        private static IEnumerable<object> UnpackEnumerable(IEnumerable items)
        {
            foreach (var item in items)
            {
                var enumerable = item as IEnumerable;
                if (enumerable != null && item is string == false)
                {
                    foreach (var nested in UnpackEnumerable(enumerable))
                    {
                        yield return nested;
                    }
                }
                else
                {
                    yield return item;
                }
            }
        }
*/
    private String ensureValidFieldName(String fieldName, boolean isNestedPath) {
        if (theSession == null || theSession.getConventions() == null || isNestedPath || isGroupBy) {
            return QueryFieldUtil.escapeIfNecessary(fieldName);
        }

        /* TODO
          foreach (var rootType in RootTypes)
            {
                var identityProperty = TheSession.Conventions.GetIdentityProperty(rootType);
                if (identityProperty != null && identityProperty.Name == fieldName)
                {
                    return Constants.Documents.Indexing.Fields.DocumentIdFieldName;
                }
            }
         */

        return QueryFieldUtil.escapeIfNecessary(fieldName);

    }
    /* TODO

        private static Func<object, string> GetImplicitStringConversion(Type type)
        {
            if (type == null)
                return null;

            Func<object, string> value;
            var localStringsCache = _implicitStringsCache;
            if (localStringsCache.TryGetValue(type, out value))
                return value;

            var methodInfo = type.GetMethod("op_Implicit", new[] { type });

            if (methodInfo == null || methodInfo.ReturnType != typeof(string))
            {
                _implicitStringsCache = new Dictionary<Type, Func<object, string>>(localStringsCache)
                {
                    {type, null}
                };
                return null;
            }

            var arg = Expression.Parameter(typeof(object), "self");

            var func = (Func<object, string>)Expression.Lambda(Expression.Call(methodInfo, Expression.Convert(arg, type)), arg).Compile();

            _implicitStringsCache = new Dictionary<Type, Func<object, string>>(localStringsCache)
            {
                {type, func}
            };
            return func;
        }
        */

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
          if (whereParams.Value is ValueType)
                return Convert.ToString(whereParams.Value, CultureInfo.InvariantCulture);

            var result = GetImplicitStringConversion(whereParams.Value.GetType());
            if (result != null)
                return result(whereParams.Value);

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

    /* TODO

        protected void UpdateFieldsToFetchToken(FieldsToFetchToken fieldsToFetch)
        {
            FieldsToFetchToken = fieldsToFetch;

            if (SelectTokens.Count == 0)
            {
                SelectTokens.AddLast(fieldsToFetch);
            }
            else
            {
                var current = SelectTokens.First;
                var replaced = false;

                while (current != null)
                {
                    if (current.Value is FieldsToFetchToken)
                    {
                        current.Value = fieldsToFetch;
                        replaced = true;
                        break;
                    }

                    current = current.Next;
                }

                if (replaced == false)
                    SelectTokens.AddLast(fieldsToFetch);
            }
        }


          protected Action<IndexQuery> BeforeQueryExecutedCallback;

        protected Action<QueryResult> AfterQueryExecutedCallback;

        protected Action<BlittableJsonReaderObject> AfterStreamExecutedCallback;

*/

    protected QueryOperation queryOperation;

    public QueryOperation getQueryOperation() {
        return queryOperation;
    }

    /* TODO

        /// <inheritdoc />
        public IDocumentQueryCustomization BeforeQueryExecuted(Action<IndexQuery> action)
        {
            BeforeQueryExecutedCallback += action;
            return this;
        }

        /// <inheritdoc />
        public IDocumentQueryCustomization AfterQueryExecuted(Action<QueryResult> action)
        {
            AfterQueryExecutedCallback += action;
            return this;
        }

        /// <inheritdoc />
        public IDocumentQueryCustomization AfterStreamExecuted(Action<BlittableJsonReaderObject> action)
        {
            AfterStreamExecutedCallback += action;
            return this;
        }

        /// <inheritdoc />
        public IDocumentQueryCustomization NoTracking()
        {
            DisableEntitiesTracking = true;
            return this;
        }

        /// <inheritdoc />
        public IDocumentQueryCustomization NoCaching()
        {
            DisableCaching = true;
            return this;
        }

        /// <inheritdoc />
        public IDocumentQueryCustomization ShowTimings()
        {
            ShowQueryTimings = true;
            return this;
        }

        /// <inheritdoc />
        IDocumentQueryCustomization IDocumentQueryCustomization.RandomOrdering()
        {
            RandomOrdering();
            return this;
        }

        /// <inheritdoc />
        IDocumentQueryCustomization IDocumentQueryCustomization.RandomOrdering(string seed)
        {
            RandomOrdering(seed);
            return this;
        }

        /// <inheritdoc />
        IDocumentQueryCustomization IDocumentQueryCustomization.CustomSortUsing(string typeName)
        {
            CustomSortUsing(typeName, false);
            return this;
        }

        /// <inheritdoc />
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
          /// <summary>
        ///   The fields to highlight
        /// </summary>
        protected List<HighlightedField> HighlightedFields = new List<HighlightedField>();

        /// <summary>
        ///   Highlighter pre tags
        /// </summary>
        protected string[] HighlighterPreTags = new string[0];

        /// <summary>
        ///   Highlighter post tags
        /// </summary>
        protected string[] HighlighterPostTags = new string[0];

        /// <summary>
        ///   Highlighter key
        /// </summary>
        protected string HighlighterKeyName;

        /// <summary>
        /// Holds the query highlights
        /// </summary>
        protected QueryHighlightings Highlightings = new QueryHighlightings();

        /// <inheritdoc />
        public void SetHighlighterTags(string preTag, string postTag)
        {
            SetHighlighterTags(new[] { preTag }, new[] { postTag });
        }

        /// <inheritdoc />
        public void Highlight(string fieldName, int fragmentLength, int fragmentCount, string fragmentsField)
        {
            throw new NotImplementedException("This feature is not yet implemented");
            //HighlightedFields.Add(new HighlightedField(fieldName, fragmentLength, fragmentCount, fragmentsField));
        }

        /// <inheritdoc />
        public void Highlight(string fieldName, int fragmentLength, int fragmentCount, out FieldHighlightings fieldHighlightings)
        {
            throw new NotImplementedException("This feature is not yet implemented");
            //HighlightedFields.Add(new HighlightedField(fieldName, fragmentLength, fragmentCount, null));
            //fieldHighlightings = Highlightings.AddField(fieldName);
        }

        /// <inheritdoc />
        public void Highlight(string fieldName, string fieldKeyName, int fragmentLength, int fragmentCount, out FieldHighlightings fieldHighlightings)
        {
            throw new NotImplementedException("This feature is not yet implemented");
            //HighlighterKeyName = fieldKeyName;
            //HighlightedFields.Add(new HighlightedField(fieldName, fragmentLength, fragmentCount, null));
            //fieldHighlightings = Highlightings.AddField(fieldName);
        }

        /// <inheritdoc />
        public void SetHighlighterTags(string[] preTags, string[] postTags)
        {
            throw new NotImplementedException("This feature is not yet implemented");
            //HighlighterPreTags = preTags;
            //HighlighterPostTags = postTags;
        }

         protected void WithinRadiusOf(string fieldName, double radius, double latitude, double longitude, SpatialUnits? radiusUnits, double distErrorPercent)
        {
            fieldName = EnsureValidFieldName(fieldName, isNestedPath: false);

            AppendOperatorIfNeeded(WhereTokens);
            NegateIfNeeded(fieldName);

            WhereTokens.AddLast(WhereToken.Within(fieldName, ShapeToken.Circle(AddQueryParameter(radius), AddQueryParameter(latitude), AddQueryParameter(longitude), radiusUnits), distErrorPercent));
        }

        protected void Spatial(string fieldName, string shapeWKT, SpatialRelation relation, double distErrorPercent)
        {
            fieldName = EnsureValidFieldName(fieldName, isNestedPath: false);

            AppendOperatorIfNeeded(WhereTokens);
            NegateIfNeeded(fieldName);

            var wktToken = ShapeToken.Wkt(AddQueryParameter(shapeWKT));
            QueryToken relationToken;
            switch (relation)
            {
                case SpatialRelation.Within:
                    relationToken = WhereToken.Within(fieldName, wktToken, distErrorPercent);
                    break;
                case SpatialRelation.Contains:
                    relationToken = WhereToken.Contains(fieldName, wktToken, distErrorPercent);
                    break;
                case SpatialRelation.Disjoint:
                    relationToken = WhereToken.Disjoint(fieldName, wktToken, distErrorPercent);
                    break;
                case SpatialRelation.Intersects:
                    relationToken = WhereToken.Intersects(fieldName, wktToken, distErrorPercent);
                    break;
                default:
                    throw new ArgumentOutOfRangeException(nameof(relation), relation, null);
            }

            WhereTokens.AddLast(relationToken);
        }

        public void Spatial(SpatialDynamicField dynamicField, SpatialCriteria criteria)
        {
            AppendOperatorIfNeeded(WhereTokens);
            NegateIfNeeded(null);

            WhereTokens.AddLast(criteria.ToQueryToken(dynamicField.ToField(EnsureValidFieldName), AddQueryParameter));
        }

        /// <inheritdoc />
        public void Spatial(string fieldName, SpatialCriteria criteria)
        {
            fieldName = EnsureValidFieldName(fieldName, isNestedPath: false);

            AppendOperatorIfNeeded(WhereTokens);
            NegateIfNeeded(fieldName);

            WhereTokens.AddLast(criteria.ToQueryToken(fieldName, AddQueryParameter));
        }

        /// <inheritdoc />
        public void OrderByDistance(string fieldName, double latitude, double longitude)
        {
            OrderByTokens.AddLast(OrderByToken.CreateDistanceAscending(fieldName, AddQueryParameter(latitude), AddQueryParameter(longitude)));
        }

        /// <inheritdoc />
        public void OrderByDistance(string fieldName, string shapeWkt)
        {
            OrderByTokens.AddLast(OrderByToken.CreateDistanceAscending(fieldName, AddQueryParameter(shapeWkt)));
        }

        /// <inheritdoc />
        public void OrderByDistanceDescending(string fieldName, double latitude, double longitude)
        {
            OrderByTokens.AddLast(OrderByToken.CreateDistanceDescending(fieldName, AddQueryParameter(latitude), AddQueryParameter(longitude)));
        }

        /// <inheritdoc />
        public void OrderByDistanceDescending(string fieldName, string shapeWkt)
        {
            OrderByTokens.AddLast(OrderByToken.CreateDistanceDescending(fieldName, AddQueryParameter(shapeWkt)));
        }
     */

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
        //TODO: InvokeAfterQueryExecuted(QueryOperation.CurrentQueryResults);
    }

    @Override
    public Iterator<T> iterator() {
        initSync();

        return queryOperation.complete(clazz).iterator();
    }

    public List<T> toList() {
        return EnumerableUtils.toList(iterator());
    }
}
