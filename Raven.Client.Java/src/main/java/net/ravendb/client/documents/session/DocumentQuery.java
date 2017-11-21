package net.ravendb.client.documents.session;

import net.ravendb.client.Constants;
import net.ravendb.client.documents.indexes.spatial.SpatialRelation;
import net.ravendb.client.documents.indexes.spatial.SpatialUnits;
import net.ravendb.client.documents.queries.QueryResult;
import net.ravendb.client.documents.queries.SearchOperator;
import net.ravendb.client.documents.session.tokens.DeclareToken;
import net.ravendb.client.documents.session.tokens.LoadToken;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

public class DocumentQuery<T> extends AbstractDocumentQuery<T, DocumentQuery<T>> implements IDocumentQuery<T> {

    public DocumentQuery(Class<T> clazz, InMemoryDocumentSessionOperations session, String indexName, String collectionName, boolean isGroupBy) {
        this(clazz, session, indexName, collectionName, isGroupBy, null, null, null);
    }

    public DocumentQuery(Class<T> clazz, InMemoryDocumentSessionOperations session, String indexName, String collectionName, boolean isGroupBy,
                         DeclareToken declareToken, List<LoadToken> loadTokens, String fromAlias) {
        super(clazz, session, indexName, collectionName, isGroupBy, declareToken, loadTokens, fromAlias);
    }

    /* TODO

        /// <inheritdoc />
        public IDocumentQuery<TProjection> SelectFields<TProjection>()
        {
            var propertyInfos = ReflectionUtil.GetPropertiesAndFieldsFor<TProjection>(BindingFlags.Public | BindingFlags.NonPublic | BindingFlags.Instance).ToList();
            var projections = propertyInfos.Select(x => x.Name).ToArray();
            var identityProperty = Conventions.GetIdentityProperty(typeof(TProjection));
            var fields = propertyInfos.Select(p => p == identityProperty ? Constants.Documents.Indexing.Fields.DocumentIdFieldName : p.Name).ToArray();
            return SelectFields<TProjection>(new QueryData(fields, projections));
        }

*/

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
    public IDocumentQuery<T> explainScores() {
        shouldExplainScores = true;
        return this;
    }

    /* TODO

        /// <inheritdoc />
        public IDocumentQuery<TProjection> SelectFields<TProjection>(params string[] fields)
        {
            return SelectFields<TProjection>(new QueryData(fields, fields));
        }

        /// <inheritdoc />
        public IDocumentQuery<TProjection> SelectFields<TProjection>(QueryData queryData)
        {
            return CreateDocumentQueryInternal<TProjection>(queryData);
        }
*/

    public IDocumentQuery<T> waitForNonStaleResults(Duration waitTimeout) {
        _waitForNonStaleResults(waitTimeout);
        return this;
    }

    /* TODO

        /// <inheritdoc />
        IRawDocumentQuery<T> IQueryBase<T, IRawDocumentQuery<T>>.WaitForNonStaleResults(TimeSpan waitTimeout)
        {
            WaitForNonStaleResults(waitTimeout);
            return this;
        }
*/

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

    //TBD public IDocumentQuery<T> AddOrder<TValue>(Expression<Func<T, TValue>> propertySelector, bool descending, OrderingType ordering)
    /* TODO


        void IQueryBase<T, IDocumentQuery<T>>.AfterQueryExecuted(Action<QueryResult> action)
        {
            AfterQueryExecuted(action);
        }

        void IQueryBase<T, IRawDocumentQuery<T>>.AfterQueryExecuted(Action<QueryResult> action)
        {
            AfterQueryExecuted(action);
        }

        void IQueryBase<T, IDocumentQuery<T>>.AfterStreamExecuted(Action<BlittableJsonReaderObject> action)
        {
            AfterStreamExecuted(action);
        }

        void IQueryBase<T, IRawDocumentQuery<T>>.AfterStreamExecuted(Action<BlittableJsonReaderObject> action)
        {
            AfterStreamExecuted(action);
        }

*/

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
    public IDocumentQuery<T> search(String fieldName, String searchTerms) {
        _search(fieldName, searchTerms);
        return this;
    }

    @Override
    public IDocumentQuery<T> search(String fieldName, String searchTerms, SearchOperator operator) {
        _search(fieldName, searchTerms, operator);
        return this;
    }

    //TBD public IDocumentQuery<T> Search<TValue>(Expression<Func<T, TValue>> propertySelector, string searchTerms, SearchOperator @operator)

    @Override
    public IDocumentQuery<T> intersect() {
        _intersect();
        return this;
    }

    @Override
    public IDocumentQuery<T> containsAny(String fieldName, Collection<Object> values) {
        _containsAny(fieldName, values);
        return this;
    }

    //TBD public IDocumentQuery<T> ContainsAny<TValue>(Expression<Func<T, TValue>> propertySelector, IEnumerable<TValue> values)

    @Override
    public IDocumentQuery<T> containsAll(String fieldName, Collection<Object> values) {
        _containsAll(fieldName, values);
        return this;
    }

    //TBD public IDocumentQuery<T> ContainsAll<TValue>(Expression<Func<T, TValue>> propertySelector, IEnumerable<TValue> values)


    /*TODO

        /// <inheritdoc />
        IDocumentQuery<T> IQueryBase<T, IDocumentQuery<T>>.Statistics(out QueryStatistics stats)
        {
            Statistics(out stats);
            return this;
        }

        /// <inheritdoc />
        IRawDocumentQuery<T> IQueryBase<T, IRawDocumentQuery<T>>.Statistics(out QueryStatistics stats)
        {
            Statistics(out stats);
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IQueryBase<T, IDocumentQuery<T>>.UsingDefaultOperator(QueryOperator queryOperator)
        {
            UsingDefaultOperator(queryOperator);
            return this;
        }

        /// <inheritdoc />
        IRawDocumentQuery<T> IQueryBase<T, IRawDocumentQuery<T>>.UsingDefaultOperator(QueryOperator queryOperator)
        {
            UsingDefaultOperator(queryOperator);
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IQueryBase<T, IDocumentQuery<T>>.NoTracking()
        {
            NoTracking();
            return this;
        }

        /// <inheritdoc />
        IRawDocumentQuery<T> IQueryBase<T, IRawDocumentQuery<T>>.NoTracking()
        {
            NoTracking();
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IQueryBase<T, IDocumentQuery<T>>.NoCaching()
        {
            NoCaching();
            return this;
        }

        /// <inheritdoc />
        IRawDocumentQuery<T> IQueryBase<T, IRawDocumentQuery<T>>.NoCaching()
        {
            NoCaching();
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IQueryBase<T, IDocumentQuery<T>>.ShowTimings()
        {
            ShowTimings();
            return this;
        }

        /// <inheritdoc />
        IRawDocumentQuery<T> IQueryBase<T, IRawDocumentQuery<T>>.ShowTimings()
        {
            ShowTimings();
            return this;
        }

*/


    @Override
    public IDocumentQuery<T> include(String path) {
        _include(path);
        return this;
    }
    //TODO: IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.Include(Expression<Func<T, object>> path)

    @Override
    public IDocumentQuery<T> not() {
        negateNext();
        return this;
    }

    public QueryResult getQueryResult() {
        initSync();

        return queryOperation.getCurrentQueryResults().createSnapshot();
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

    /* TODO
        IRawDocumentQuery<T> IQueryBase<T, IRawDocumentQuery<T>>.Skip(int count)
        {
            Skip(count);
            return this;
        }
*/

    @Override
    public IDocumentQuery<T> whereLucene(String fieldName, String whereClause) {
        _whereLucene(fieldName, whereClause);
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

    //TBD IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.WhereEquals<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value, bool exact)

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

    //TBD IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.WhereNotEquals<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value, bool exact)

    @Override
    public IDocumentQuery<T> whereNotEquals(WhereParams whereParams) {
        _whereNotEquals(whereParams);
        return this;
    }

    @Override
    public IDocumentQuery<T> whereIn(String fieldName, Collection<Object> values) {
        return whereIn(fieldName, values, false);
    }

    @Override
    public IDocumentQuery<T> whereIn(String fieldName, Collection<Object> values, boolean exact) {
        _whereIn(fieldName, values, exact);
        return this;
    }

    //TBD public IDocumentQuery<T> WhereIn<TValue>(Expression<Func<T, TValue>> propertySelector, IEnumerable<TValue> values, bool exact = false)

    @Override
    public IDocumentQuery<T> whereStartsWith(String fieldName, Object value) {
        _whereStartsWith(fieldName, value);
        return this;
    }

    @Override
    public IDocumentQuery<T> whereEndsWith(String fieldName, Object value) {
        _whereEndsWith(fieldName, value);
        return this;
    }

    //TBD: public IDocumentQuery<T> WhereEndsWith<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value)

    @Override
    public IDocumentQuery<T> whereBetween(String fieldName, Object start, Object end) {
        return whereBetween(fieldName, start, end, false);
    }

    @Override
    public IDocumentQuery<T> whereBetween(String fieldName, Object start, Object end, boolean exact) {
        _whereBetween(fieldName, start, end, exact);
        return this;
    }

    //TBD public IDocumentQuery<T> WhereBetween<TValue>(Expression<Func<T, TValue>> propertySelector, TValue start, TValue end, bool exact = false)

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

    //TBD public IDocumentQuery<T> WhereGreaterThan<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value, bool exact = false)
    //TBD public IDocumentQuery<T> WhereGreaterThanOrEqual<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value, bool exact = false)

    public IDocumentQuery<T> whereLessThan(String fieldName, Object value) {
        return whereLessThan(fieldName, value, false);
    }

    public IDocumentQuery<T> whereLessThan(String fieldName, Object value, boolean exact) {
        _whereLessThan(fieldName, value, exact);
        return this;
    }

    //TBD public IDocumentQuery<T> WhereLessThanOrEqual<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value, bool exact = false)

    public IDocumentQuery<T> whereLessThanOrEqual(String fieldName, Object value) {
        return whereLessThanOrEqual(fieldName, value, false);
    }

    public IDocumentQuery<T> whereLessThanOrEqual(String fieldName, Object value, boolean exact) {
        _whereLessThanOrEqual(fieldName, value, exact);
        return this;
    }

    //TBD public IDocumentQuery<T> WhereLessThanOrEqual<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value, bool exact = false)
    //TBD public IDocumentQuery<T> WhereExists<TValue>(Expression<Func<T, TValue>> propertySelector)

    @Override
    public IDocumentQuery<T> whereExists(String fieldName) {
        _whereExists(fieldName);
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
    public IDocumentQuery<T> boost(double boost) {
        _boost(boost);
        return this;
    }

    @Override
    public IDocumentQuery<T> fuzzy(double fuzzy) {
        _fuzzy(fuzzy);
        return this;
    }

    @Override
    public IDocumentQuery<T> proximity(int proxomity) {
        _proximity(proxomity);
        return this;
    }

    @Override
    public IDocumentQuery<T> randomOrdering() {
        _randomOrdering();
        return this;
    }

    @Override
    public IDocumentQuery<T> randomOrdering(String seed) {
        randomOrdering(seed);
        return this;
    }

    @Override
    public IDocumentQuery<T> customSortUsing(String typeName, boolean descending) {
        _customSortUsing(typeName, descending);
        return this;
    }

    /* TODO


        /// <inheritdoc />
        IGroupByDocumentQuery<T> IDocumentQuery<T>.GroupBy(string fieldName, params string[] fieldNames)
        {
            GroupBy(fieldName, fieldNames);
            return new GroupByDocumentQuery<T>(this);
        }

        /// <inheritdoc />
        IDocumentQuery<T> IRawDocumentQuery<T>.AddParameter(string name, object value)
        {
            AddParameter(name, value);
            return this;
        }

        /// <inheritdoc />
        public IDocumentQuery<TResult> OfType<TResult>()
        {
            return CreateDocumentQueryInternal<TResult>();
        }
*/

    public IDocumentQuery<T> orderBy(String field) {
        return orderBy(field, OrderingType.STRING);
    }

    public IDocumentQuery<T> orderBy(String field, OrderingType ordering) {
        _orderBy(field, ordering);
        return this;
    }

    /*

        /// <inheritdoc />
        public IDocumentQuery<T> OrderBy<TValue>(params Expression<Func<T, TValue>>[] propertySelectors)
        {
            foreach (var item in propertySelectors)
            {
                OrderBy(GetMemberQueryPathForOrderBy(item), OrderingUtil.GetOrderingOfType(item.ReturnType));
            }
            return this;
        }
        */

    public IDocumentQuery<T> orderByDescending(String field) {
        return orderByDescending(field, OrderingType.STRING);
    }

    public IDocumentQuery<T> orderByDescending(String field, OrderingType ordering) {
        _orderByDescending(field, ordering);
        return this;
    }

    /*TODO


        /// <inheritdoc />
        public IDocumentQuery<T> OrderByDescending<TValue>(params Expression<Func<T, TValue>>[] propertySelectors)
        {
            foreach (var item in propertySelectors)
            {
                OrderByDescending(GetMemberQueryPathForOrderBy(item), OrderingUtil.GetOrderingOfType(item.ReturnType));
            }

            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IQueryBase<T, IDocumentQuery<T>>.WaitForNonStaleResultsAsOf(long cutOffEtag)
        {
            WaitForNonStaleResultsAsOf(cutOffEtag);
            return this;
        }

        /// <inheritdoc />
        IRawDocumentQuery<T> IQueryBase<T, IRawDocumentQuery<T>>.WaitForNonStaleResultsAsOf(long cutOffEtag)
        {
            WaitForNonStaleResultsAsOf(cutOffEtag);
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IQueryBase<T, IDocumentQuery<T>>.WaitForNonStaleResultsAsOf(long cutOffEtag, TimeSpan waitTimeout)
        {
            WaitForNonStaleResultsAsOf(cutOffEtag, waitTimeout);
            return this;
        }

        /// <inheritdoc />
        IRawDocumentQuery<T> IQueryBase<T, IRawDocumentQuery<T>>.WaitForNonStaleResultsAsOf(long cutOffEtag, TimeSpan waitTimeout)
        {
            WaitForNonStaleResultsAsOf(cutOffEtag, waitTimeout);
            return this;
        }
*/
    public IDocumentQuery<T> waitForNonStaleResults() {
        _waitForNonStaleResults();
        return this;
    }
    /* TODO:
        /// <inheritdoc />
        IRawDocumentQuery<T> IQueryBase<T, IRawDocumentQuery<T>>.WaitForNonStaleResults()
        {
            WaitForNonStaleResults();
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IQueryBase<T, IDocumentQuery<T>>.BeforeQueryExecuted(Action<IndexQuery> beforeQueryExecuted)
        {
            BeforeQueryExecuted(beforeQueryExecuted);
            return this;
        }

        /// <inheritdoc />
        IRawDocumentQuery<T> IQueryBase<T, IRawDocumentQuery<T>>.BeforeQueryExecuted(Action<IndexQuery> beforeQueryExecuted)
        {
            BeforeQueryExecuted(beforeQueryExecuted);
            return this;
        }

        /// <inheritdoc />
        IEnumerator IEnumerable.GetEnumerator()
        {
            return GetEnumerator();
        }

        /// <inheritdoc />
        public IEnumerator<T> GetEnumerator()
        {
            InitSync();
            return QueryOperation.Complete<T>().GetEnumerator();
        }

        /// <inheritdoc />
        public T First()
        {
            return ExecuteQueryOperation(1).First();
        }

        /// <inheritdoc />
        public T FirstOrDefault()
        {
            return ExecuteQueryOperation(1).FirstOrDefault();
        }

        /// <inheritdoc />
        public T Single()
        {
            return ExecuteQueryOperation(2).Single();
        }

        /// <inheritdoc />
        public T SingleOrDefault()
        {
            return ExecuteQueryOperation(2).SingleOrDefault();
        }

        private IEnumerable<T> ExecuteQueryOperation(int take)
        {
            if (PageSize.HasValue == false || PageSize > take)
                Take(take);

            InitSync();

            return QueryOperation.Complete<T>();
        }

        /// <inheritdoc />
        public int Count()
        {
            Take(0);
            var queryResult = GetQueryResult();
            return queryResult.TotalResults;
        }

        /// <inheritdoc />
        public Lazy<IEnumerable<T>> Lazily()
        {
            return Lazily(null);
        }

        /// <inheritdoc />
        public Lazy<int> CountLazily()
        {
            if (QueryOperation == null)
            {
                Take(0);
                QueryOperation = InitializeQueryOperation();
            }


            var lazyQueryOperation = new LazyQueryOperation<T>(TheSession.Conventions, QueryOperation, AfterQueryExecutedCallback);

            return ((DocumentSession)TheSession).AddLazyCountOperation(lazyQueryOperation);
        }

        /// <inheritdoc />
        public Lazy<IEnumerable<T>> Lazily(Action<IEnumerable<T>> onEval)
        {
            if (QueryOperation == null)
            {
                QueryOperation = InitializeQueryOperation();
            }

            var lazyQueryOperation = new LazyQueryOperation<T>(TheSession.Conventions, QueryOperation, AfterQueryExecutedCallback);
            return ((DocumentSession)TheSession).AddLazyOperation(lazyQueryOperation, onEval);
        }

        private DocumentQuery<TResult> CreateDocumentQueryInternal<TResult>(QueryData queryData = null)
        {
            var newFieldsToFetch = queryData != null && queryData.Fileds.Length > 0
                ? FieldsToFetchToken.Create(queryData.Fileds, queryData.Projections.ToArray(), queryData.IsCustomFunction)
                : null;

            if (newFieldsToFetch != null)
                UpdateFieldsToFetchToken(newFieldsToFetch);

            var query = new DocumentQuery<TResult>(
                TheSession,
                IndexName,
                CollectionName,
                IsGroupBy,
                queryData?.DeclareToken,
                queryData?.LoadTokens,
                queryData?.FromAlias)
            {
                QueryRaw = QueryRaw,
                PageSize = PageSize,
                SelectTokens = SelectTokens,
                FieldsToFetchToken = FieldsToFetchToken,
                WhereTokens = WhereTokens,
                OrderByTokens = OrderByTokens,
                GroupByTokens = GroupByTokens,
                QueryParameters = QueryParameters,
                Start = Start,
                Timeout = Timeout,
                CutoffEtag = CutoffEtag,
                QueryStats = QueryStats,
                TheWaitForNonStaleResults = TheWaitForNonStaleResults,
                Negate = Negate,
                Includes = new HashSet<string>(Includes),
                RootTypes = { typeof(T) },
                BeforeQueryExecutedCallback = BeforeQueryExecutedCallback,
                AfterQueryExecutedCallback = AfterQueryExecutedCallback,
                AfterStreamExecutedCallback = AfterStreamExecutedCallback,
                HighlightedFields = new List<HighlightedField>(HighlightedFields),
                HighlighterPreTags = HighlighterPreTags,
                HighlighterPostTags = HighlighterPostTags,
                DisableEntitiesTracking = DisableEntitiesTracking,
                DisableCaching = DisableCaching,
                ShowQueryTimings = ShowQueryTimings,
                LastEquality = LastEquality,
                ShouldExplainScores = ShouldExplainScores,
                IsIntersect = IsIntersect,
                DefaultOperator = DefaultOperator
            };

            query.AfterQueryExecuted(AfterQueryExecutedCallback);
            return query;
        }

          /// <inheritdoc />
        public FacetedQueryResult GetFacets(string facetSetupDoc, int facetStart, int? facetPageSize)
        {
            var q = GetIndexQuery();
            var query = FacetQuery.Create(q, facetSetupDoc, null, facetStart, facetPageSize, Conventions);

            var command = new GetFacetsCommand(Conventions, TheSession.Context, query);
            TheSession.RequestExecutor.Execute(command, TheSession.Context);

            return command.Result;
        }

        /// <inheritdoc />
        public FacetedQueryResult GetFacets(List<Facet> facets, int facetStart, int? facetPageSize)
        {
            var q = GetIndexQuery();
            var query = FacetQuery.Create(q, null, facets, facetStart, facetPageSize, Conventions);

            var command = new GetFacetsCommand(Conventions, TheSession.Context, query);
            TheSession.RequestExecutor.Execute(command, TheSession.Context);

            return command.Result;
        }

        /// <inheritdoc />
        public Lazy<FacetedQueryResult> GetFacetsLazy(string facetSetupDoc, int facetStart, int? facetPageSize)
        {
            var q = GetIndexQuery();
            var query = FacetQuery.Create(q, facetSetupDoc, null, facetStart, facetPageSize, Conventions);

            var lazyFacetsOperation = new LazyFacetsOperation(Conventions, query);
            return ((DocumentSession)TheSession).AddLazyOperation(lazyFacetsOperation, (Action<FacetedQueryResult>)null);
        }

        /// <inheritdoc />
        public Lazy<FacetedQueryResult> GetFacetsLazy(List<Facet> facets, int facetStart, int? facetPageSize)
        {
            var q = GetIndexQuery();
            var query = FacetQuery.Create(q, null, facets, facetStart, facetPageSize, Conventions);

            var lazyFacetsOperation = new LazyFacetsOperation(Conventions, query);
            return ((DocumentSession)TheSession).AddLazyOperation(lazyFacetsOperation, (Action<FacetedQueryResult>)null);
        }  /// <inheritdoc />


        */
    //TBD IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.Highlight(string fieldName, int fragmentLength, int fragmentCount, string fragmentsField)
    //TBD IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.Highlight(string fieldName, int fragmentLength, int fragmentCount, out FieldHighlightings highlightings)
    //TBD IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.Highlight(string fieldName,string fieldKeyName, int fragmentLength,int fragmentCount,out FieldHighlightings highlightings)
    //TBD IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.Highlight<TValue>(Expression<Func<T, TValue>> propertySelector, int fragmentLength, int fragmentCount, Expression<Func<T, IEnumerable>> fragmentsPropertySelector)
    //TBD IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.Highlight<TValue>(Expression<Func<T, TValue>> propertySelector, int fragmentLength, int fragmentCount, out FieldHighlightings fieldHighlightings)
    //TBD IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.Highlight<TValue>(Expression<Func<T, TValue>> propertySelector, Expression<Func<T, TValue>> keyPropertySelector, int fragmentLength, int fragmentCount, out FieldHighlightings fieldHighlightings)
    //TBD IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.SetHighlighterTags(string preTag, string postTag)
    //TBD IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.SetHighlighterTags(string[] preTags, string[] postTags)
    //TBD public IDocumentQuery<T> Spatial(Expression<Func<T, object>> path, Func<SpatialCriteriaFactory, SpatialCriteria> clause)

    /* TODO

        /// <inheritdoc />
        public IDocumentQuery<T> Spatial(string fieldName, Func<SpatialCriteriaFactory, SpatialCriteria> clause)
        {
            var criteria = clause(SpatialCriteriaFactory.Instance);
            Spatial(fieldName, criteria);
            return this;
        }

        public IDocumentQuery<T> Spatial(SpatialDynamicField field, Func<SpatialCriteriaFactory, SpatialCriteria> clause)
        {
            var criteria = clause(SpatialCriteriaFactory.Instance);
            Spatial(field, criteria);
            return this;
        }

        /// <inheritdoc />
        public IDocumentQuery<T> Spatial(Func<SpatialDynamicFieldFactory<T>, SpatialDynamicField> field, Func<SpatialCriteriaFactory, SpatialCriteria> clause)
        {
            var criteria = clause(SpatialCriteriaFactory.Instance);
            var dynamicField = field(new SpatialDynamicFieldFactory<T>());
            Spatial(dynamicField, criteria);
            return this;
        }
*/
    //TBD IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.WithinRadiusOf<TValue>(Expression<Func<T, TValue>> propertySelector, double radius, double latitude, double longitude, SpatialUnits? radiusUnits, double distanceErrorPct)

    @Override
    public IDocumentQuery<T> withinRadiusOf(String fieldName, double radius, double latitude, double longitude) {
        return withinRadiusOf(fieldName, radius, latitude, longitude, null, Constants.Documents.Indexing.Spatial.DEFAULT_DISTANCE_ERROR_PCT);
    }

    @Override
    public IDocumentQuery<T> withinRadiusOf(String fieldName, double radius, double latitude, double longitude, SpatialUnits radiusUnits) {
        return withinRadiusOf(fieldName, radius, latitude, longitude, radiusUnits, Constants.Documents.Indexing.Spatial.DEFAULT_DISTANCE_ERROR_PCT);
    }

    @Override
    public IDocumentQuery<T> withinRadiusOf(String fieldName, double radius, double latitude, double longitude, SpatialUnits radiusUnits, double distanceErrorPct) {
        _withinRadiusOf(fieldName, radius, latitude, longitude, radiusUnits, distanceErrorPct);
        return this;
    }

    //TBD IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.RelatesToShape<TValue>(Expression<Func<T, TValue>> propertySelector, string shapeWKT, SpatialRelation relation, double distanceErrorPct)

    @Override
    public IDocumentQuery<T> relatesToShape(String fieldName, String shapeWKT, SpatialRelation relation) {
        return relatesToShape(fieldName, shapeWKT, relation, Constants.Documents.Indexing.Spatial.DEFAULT_DISTANCE_ERROR_PCT);
    }

    @Override
    public IDocumentQuery<T> relatesToShape(String fieldName, String shapeWKT, SpatialRelation relation, double distanceErrorPct) {
        _spatial(fieldName, shapeWKT, relation, distanceErrorPct);
        return this;
    }

    //TBD IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.OrderByDistance<TValue>(Expression<Func<T, TValue>> propertySelector, double latitude, double longitude)

    @Override
    public IDocumentQuery<T> orderByDistance(String fieldName, double latitude, double longitude) {
        _orderByDistance(fieldName, latitude, longitude);
        return this;
    }

    //TBD IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.OrderByDistance<TValue>(Expression<Func<T, TValue>> propertySelector, string shapeWkt)

    @Override
    public IDocumentQuery<T> orderByDistance(String fieldName, String shapeWkt) {
        orderByDistance(fieldName, shapeWkt);
        return this;
    }

    //TBD IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.OrderByDistanceDescending<TValue>(Expression<Func<T, TValue>> propertySelector, double latitude, double longitude)

    @Override
    public IDocumentQuery<T> orderByDistanceDescending(String fieldName, double latitude, double longitude) {
        _orderByDistanceDescending(fieldName, latitude, longitude);
        return this;
    }

    //TBD IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.OrderByDistanceDescending<TValue>(Expression<Func<T, TValue>> propertySelector, string shapeWkt)

    @Override
    public IDocumentQuery<T> orderByDistanceDescending(String fieldName, String shapeWkt) {
        _orderByDistanceDescending(fieldName, shapeWkt);
        return this;
    }
}
