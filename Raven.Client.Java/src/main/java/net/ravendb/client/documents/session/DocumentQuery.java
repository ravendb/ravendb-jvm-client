package net.ravendb.client.documents.session;

public class DocumentQuery<T> extends AbstractDocumentQuery<T, DocumentQuery<T>> implements IDocumentQuery<T> {

    public DocumentQuery(Class<T> clazz, InMemoryDocumentSessionOperations session, String indexName, String collectionName, boolean isGroupBy) {
        super(clazz, session, indexName, collectionName, isGroupBy); //TODO:DeclareToken declareToken = null, List<LoadToken> loadTokens = null, string fromAlias = null
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

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.Distinct()
        {
            Distinct();
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.OrderByScore()
        {
            OrderByScore();
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.OrderByScoreDescending()
        {
            OrderByScoreDescending();
            return this;
        }

        /// <inheritdoc />
        public IDocumentQuery<T> ExplainScores()
        {
            ShouldExplainScores = true;
            return this;
        }

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

        /// <inheritdoc />
        IDocumentQuery<T> IQueryBase<T, IDocumentQuery<T>>.WaitForNonStaleResults(TimeSpan waitTimeout)
        {
            WaitForNonStaleResults(waitTimeout);
            return this;
        }

        /// <inheritdoc />
        IRawDocumentQuery<T> IQueryBase<T, IRawDocumentQuery<T>>.WaitForNonStaleResults(TimeSpan waitTimeout)
        {
            WaitForNonStaleResults(waitTimeout);
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.AddOrder(string fieldName, bool descending, OrderingType ordering)
        {
            if (descending)
                OrderByDescending(fieldName, ordering);
            else
                OrderBy(fieldName, ordering);

            return this;
        }

        /// <inheritdoc />
        public IDocumentQuery<T> AddOrder<TValue>(Expression<Func<T, TValue>> propertySelector, bool descending, OrderingType ordering)
        {
            var fieldName = GetMemberQueryPath(propertySelector.Body);
            if (descending)
                OrderByDescending(fieldName, ordering);
            else
                OrderBy(fieldName, ordering);

            return this;
        }

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
        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.OpenSubclause()
        {
            OpenSubclause();
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.CloseSubclause()
        {
            CloseSubclause();
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.Search(string fieldName, string searchTerms, SearchOperator @operator)
        {
            Search(fieldName, searchTerms, @operator);
            return this;
        }

        /// <inheritdoc />
        public IDocumentQuery<T> Search<TValue>(Expression<Func<T, TValue>> propertySelector, string searchTerms, SearchOperator @operator)
        {
            Search(GetMemberQueryPath(propertySelector.Body), searchTerms, @operator);
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.Intersect()
        {
            Intersect();
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.ContainsAny(string fieldName, IEnumerable<object> values)
        {
            ContainsAny(fieldName, values);
            return this;
        }

        /// <inheritdoc />
        public IDocumentQuery<T> ContainsAny<TValue>(Expression<Func<T, TValue>> propertySelector, IEnumerable<TValue> values)
        {
            ContainsAny(GetMemberQueryPath(propertySelector.Body), values.Cast<object>());
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.ContainsAll(string fieldName, IEnumerable<object> values)
        {
            ContainsAll(fieldName, values);
            return this;
        }

        /// <inheritdoc />
        public IDocumentQuery<T> ContainsAll<TValue>(Expression<Func<T, TValue>> propertySelector, IEnumerable<TValue> values)
        {
            ContainsAll(GetMemberQueryPath(propertySelector.Body), values.Cast<object>());
            return this;
        }

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
        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.Include(string path)
        {
            Include(path);
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.Include(Expression<Func<T, object>> path)
        {
            Include(path);
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.Not
        {
            get
            {
                NegateNext();
                return this;
            }
        }

        /// <inheritdoc />
        public QueryResult GetQueryResult()
        {
            InitSync();

            return QueryOperation.CurrentQueryResults.CreateSnapshot();
        }
        */

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

        /// <inheritdoc />
        IRawDocumentQuery<T> IQueryBase<T, IRawDocumentQuery<T>>.Skip(int count)
        {
            Skip(count);
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.WhereLucene(string fieldName, string whereClause)
        {
            WhereLucene(fieldName, whereClause);
            return this;
        }

*/

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

    /* TODO
        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.WhereEquals<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value, bool exact)
        {
            WhereEquals(GetMemberQueryPath(propertySelector.Body), value, exact);
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.WhereEquals(WhereParams whereParams)
        {
            WhereEquals(whereParams);
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.WhereNotEquals(string fieldName, object value, bool exact)
        {
            WhereNotEquals(fieldName, value, exact);
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.WhereNotEquals<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value, bool exact)
        {
            WhereNotEquals(GetMemberQueryPath(propertySelector.Body), value, exact);
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.WhereNotEquals(WhereParams whereParams)
        {
            WhereNotEquals(whereParams);
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.WhereIn(string fieldName, IEnumerable<object> values, bool exact)
        {
            WhereIn(fieldName, values, exact);
            return this;
        }

        /// <inheritdoc />
        public IDocumentQuery<T> WhereIn<TValue>(Expression<Func<T, TValue>> propertySelector, IEnumerable<TValue> values, bool exact = false)
        {
            WhereIn(GetMemberQueryPath(propertySelector.Body), values.Cast<object>(), exact);
            return this;
        }

*/

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

    //TODO: public IDocumentQuery<T> WhereEndsWith<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value)

    /* TODO

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.WhereBetween(string fieldName, object start, object end, bool exact)
        {
            WhereBetween(fieldName, start, end, exact);
            return this;
        }

        /// <inheritdoc />
        public IDocumentQuery<T> WhereBetween<TValue>(Expression<Func<T, TValue>> propertySelector, TValue start, TValue end, bool exact = false)
        {
            WhereBetween(GetMemberQueryPath(propertySelector.Body), start, end, exact);
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.WhereGreaterThan(string fieldName, object value, bool exact)
        {
            WhereGreaterThan(fieldName, value, exact);
            return this;
        }

        /// <inheritdoc />
        public IDocumentQuery<T> WhereGreaterThan<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value, bool exact = false)
        {
            WhereGreaterThan(GetMemberQueryPath(propertySelector.Body), value, exact);
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.WhereGreaterThanOrEqual(string fieldName, object value, bool exact)
        {
            WhereGreaterThanOrEqual(fieldName, value, exact);
            return this;
        }

        /// <inheritdoc />
        public IDocumentQuery<T> WhereGreaterThanOrEqual<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value, bool exact = false)
        {
            WhereGreaterThanOrEqual(GetMemberQueryPath(propertySelector.Body), value, exact);
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.WhereLessThan(string fieldName, object value, bool exact)
        {
            WhereLessThan(fieldName, value, exact);
            return this;
        }

        /// <inheritdoc />
        public IDocumentQuery<T> WhereLessThan<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value, bool exact = false)
        {
            WhereLessThan(GetMemberQueryPath(propertySelector.Body), value, exact);
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.WhereLessThanOrEqual(string fieldName, object value, bool exact)
        {
            WhereLessThanOrEqual(fieldName, value, exact);
            return this;
        }

        /// <inheritdoc />
        public IDocumentQuery<T> WhereLessThanOrEqual<TValue>(Expression<Func<T, TValue>> propertySelector, TValue value, bool exact = false)
        {
            WhereLessThanOrEqual(GetMemberQueryPath(propertySelector.Body), value, exact);
            return this;
        }

        /// <inheritdoc />
        public IDocumentQuery<T> WhereExists<TValue>(Expression<Func<T, TValue>> propertySelector)
        {
            WhereExists(GetMemberQueryPath(propertySelector.Body));
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.WhereExists(string fieldName)
        {
            WhereExists(fieldName);
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.AndAlso()
        {
            AndAlso();
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.OrElse()
        {
            OrElse();
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.Boost(decimal boost)
        {
            Boost(boost);
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.Fuzzy(decimal fuzzy)
        {
            Fuzzy(fuzzy);
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.Proximity(int proximity)
        {
            Proximity(proximity);
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.RandomOrdering()
        {
            RandomOrdering();
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.RandomOrdering(string seed)
        {
            RandomOrdering(seed);
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.CustomSortUsing(string typeName, bool descending)
        {
            CustomSortUsing(typeName, descending);
            return this;
        }

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

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.OrderBy(string field, OrderingType ordering)
        {
            OrderBy(field, ordering);
            return this;
        }

        /// <inheritdoc />
        public IDocumentQuery<T> OrderBy<TValue>(params Expression<Func<T, TValue>>[] propertySelectors)
        {
            foreach (var item in propertySelectors)
            {
                OrderBy(GetMemberQueryPathForOrderBy(item), OrderingUtil.GetOrderingOfType(item.ReturnType));
            }
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.OrderByDescending(string field, OrderingType ordering)
        {
            OrderByDescending(field, ordering);
            return this;
        }

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

        /// <inheritdoc />
        IDocumentQuery<T> IQueryBase<T, IDocumentQuery<T>>.WaitForNonStaleResults()
        {
            WaitForNonStaleResults();
            return this;
        }

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
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.Highlight(
            string fieldName,
            int fragmentLength,
            int fragmentCount,
            string fragmentsField)
        {
            Highlight(fieldName, fragmentLength, fragmentCount, fragmentsField);
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.Highlight(
            string fieldName,
            int fragmentLength,
            int fragmentCount,
            out FieldHighlightings highlightings)
        {
            Highlight(fieldName, fragmentLength, fragmentCount, out highlightings);
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.Highlight(
            string fieldName,
            string fieldKeyName,
            int fragmentLength,
            int fragmentCount,
            out FieldHighlightings highlightings)
        {
            Highlight(fieldName, fieldKeyName, fragmentLength, fragmentCount, out highlightings);
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.Highlight<TValue>(
            Expression<Func<T, TValue>> propertySelector,
            int fragmentLength,
            int fragmentCount,
            Expression<Func<T, IEnumerable>> fragmentsPropertySelector)
        {
            var fieldName = GetMemberQueryPath(propertySelector);
            var fragmentsField = GetMemberQueryPath(fragmentsPropertySelector);
            Highlight(fieldName, fragmentLength, fragmentCount, fragmentsField);
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.Highlight<TValue>(
            Expression<Func<T, TValue>> propertySelector,
            int fragmentLength,
            int fragmentCount,
            out FieldHighlightings fieldHighlightings)
        {
            Highlight(GetMemberQueryPath(propertySelector), fragmentLength, fragmentCount, out fieldHighlightings);
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.Highlight<TValue>(
            Expression<Func<T, TValue>> propertySelector,
            Expression<Func<T, TValue>> keyPropertySelector,
            int fragmentLength,
            int fragmentCount,
            out FieldHighlightings fieldHighlightings)
        {
            Highlight(GetMemberQueryPath(propertySelector), GetMemberQueryPath(keyPropertySelector), fragmentLength, fragmentCount, out fieldHighlightings);
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.SetHighlighterTags(string preTag, string postTag)
        {
            SetHighlighterTags(new[] { preTag }, new[] { postTag });
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.SetHighlighterTags(string[] preTags, string[] postTags)
        {
            SetHighlighterTags(preTags, postTags);
            return this;
        }

         /// <inheritdoc />
        public IDocumentQuery<T> Spatial(Expression<Func<T, object>> path, Func<SpatialCriteriaFactory, SpatialCriteria> clause)
        {
            return Spatial(path.ToPropertyPath(), clause);
        }

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

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.WithinRadiusOf<TValue>(Expression<Func<T, TValue>> propertySelector, double radius, double latitude, double longitude, SpatialUnits? radiusUnits, double distanceErrorPct)
        {
            WithinRadiusOf(propertySelector.ToPropertyPath(), radius, latitude, longitude, radiusUnits, distanceErrorPct);
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.WithinRadiusOf(string fieldName, double radius, double latitude, double longitude, SpatialUnits? radiusUnits, double distanceErrorPct)
        {
            WithinRadiusOf(fieldName, radius, latitude, longitude, radiusUnits, distanceErrorPct);
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.RelatesToShape<TValue>(Expression<Func<T, TValue>> propertySelector, string shapeWKT, SpatialRelation relation, double distanceErrorPct)
        {
            Spatial(propertySelector.ToPropertyPath(), shapeWKT, relation, distanceErrorPct);
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.RelatesToShape(string fieldName, string shapeWKT, SpatialRelation relation, double distanceErrorPct)
        {
            Spatial(fieldName, shapeWKT, relation, distanceErrorPct);
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.OrderByDistance<TValue>(Expression<Func<T, TValue>> propertySelector, double latitude, double longitude)
        {
            OrderByDistance(propertySelector.ToPropertyPath(), latitude, longitude);
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.OrderByDistance(string fieldName, double latitude, double longitude)
        {
            OrderByDistance(fieldName, latitude, longitude);
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.OrderByDistance<TValue>(Expression<Func<T, TValue>> propertySelector, string shapeWkt)
        {
            OrderByDistance(propertySelector.ToPropertyPath(), shapeWkt);
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.OrderByDistance(string fieldName, string shapeWkt)
        {
            OrderByDistance(fieldName, shapeWkt);
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.OrderByDistanceDescending<TValue>(Expression<Func<T, TValue>> propertySelector, double latitude, double longitude)
        {
            OrderByDistanceDescending(propertySelector.ToPropertyPath(), latitude, longitude);
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.OrderByDistanceDescending(string fieldName, double latitude, double longitude)
        {
            OrderByDistanceDescending(fieldName, latitude, longitude);
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.OrderByDistanceDescending<TValue>(Expression<Func<T, TValue>> propertySelector, string shapeWkt)
        {
            OrderByDistanceDescending(propertySelector.ToPropertyPath(), shapeWkt);
            return this;
        }

        /// <inheritdoc />
        IDocumentQuery<T> IDocumentQueryBase<T, IDocumentQuery<T>>.OrderByDistanceDescending(string fieldName, string shapeWkt)
        {
            OrderByDistanceDescending(fieldName, shapeWkt);
            return this;
        }
     */
}
