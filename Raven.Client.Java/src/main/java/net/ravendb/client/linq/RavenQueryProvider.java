package net.ravendb.client.linq;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.ravendb.abstractions.basic.Lazy;
import net.ravendb.abstractions.closure.Action1;
import net.ravendb.abstractions.data.QueryResult;
import net.ravendb.abstractions.json.linq.RavenJToken;
import net.ravendb.client.IDocumentQuery;
import net.ravendb.client.RavenQueryHighlightings;
import net.ravendb.client.RavenQueryStatistics;
import net.ravendb.client.connection.IDatabaseCommands;
import net.ravendb.client.document.DocumentQueryCustomizationFactory;
import net.ravendb.client.document.InMemoryDocumentSessionOperations;

import com.mysema.query.types.Expression;


public class RavenQueryProvider<T> implements IRavenQueryProvider {

  private Action1<QueryResult> afterQueryExecuted;
  private DocumentQueryCustomizationFactory customizeQuery;
  private final String indexName;
  private final IDocumentQueryGenerator queryGenerator;
  private final RavenQueryStatistics ravenQueryStatistics;
  private final RavenQueryHighlightings highlightings;
  private final IDatabaseCommands databaseCommands;
  private final boolean isMapReduce;
  private final Map<String, RavenJToken> transformerParameters = new HashMap<>();
  private final Class<T> clazz;

  private Set<String> fieldsToFetch = new HashSet<>();
  private String resultTranformer;
  private List<RenamedField> fieldsToRename;

  public RavenQueryProvider(Class<T> clazz, IDocumentQueryGenerator queryGenerator, String indexName, RavenQueryStatistics ravenQueryStatistics,
      RavenQueryHighlightings highlightings, IDatabaseCommands databaseCommands, boolean isMapReduce) {
    this.clazz = clazz;

    fieldsToFetch = new HashSet<>();
    fieldsToRename = new ArrayList<>();

    this.queryGenerator = queryGenerator;
    this.indexName = indexName;
    this.ravenQueryStatistics = ravenQueryStatistics;
    this.highlightings = highlightings;
    this.databaseCommands = databaseCommands;
    this.isMapReduce = isMapReduce;

  }

  /**
   * @return the actions for customizing the generated lucene query
   */
  public DocumentQueryCustomizationFactory getCustomizedQuery() {
    return customizeQuery;
  }

  /**
   * Gets the name of the index.
   */
  @Override
  public String getIndexName() {
    return indexName;
  }

  /**
   * Get the query generator
   */
  @Override
  public IDocumentQueryGenerator getQueryGenerator() {
    return queryGenerator;
  }

  @Override
  public DocumentQueryCustomizationFactory getCustomizeQuery() {
    return customizeQuery;
  }

  @Override
  public Set<String> getFieldsToFetch() {
    return fieldsToFetch;
  }

  /**
   * Gets the results transformer to use
   */
  @Override
  public String getResultTranformer() {
    return resultTranformer;
  }

  @Override
  public Map<String, RavenJToken> getTransformerParameters() {
    return transformerParameters;
  }

  @Override
  public void addTransformerParameter(String name, RavenJToken value) {
    transformerParameters.put(name, value);
  }

  public List<RenamedField> getFieldsToRename() {
    return fieldsToRename;
  }

  @SuppressWarnings("hiding")
  @Override
  public <S> IRavenQueryProvider forClass(Class<S> clazz) {
    if (this.clazz.equals(clazz)) {
      return this;
    }
    RavenQueryProvider<S> ravenQueryProvider = new RavenQueryProvider<>(clazz, queryGenerator, indexName, ravenQueryStatistics, highlightings, databaseCommands, isMapReduce);
    ravenQueryProvider.resultTranformer = resultTranformer;
    ravenQueryProvider.customize(customizeQuery);
    for (Map.Entry<String, RavenJToken> transformerParameter: transformerParameters.entrySet()) {
      ravenQueryProvider.addTransformerParameter(transformerParameter.getKey(), transformerParameter.getValue());
    }
    return ravenQueryProvider;
  }

  @Override
  public Object execute(Expression<?> expression) {
    return getQueryProviderProcessor(clazz).execute(expression);
  }

  /**
   *  Callback to get the results of the query
   */
  @Override
  public void afterQueryExecuted(Action1<QueryResult> afterQueryExecutedCallback) {
    this.afterQueryExecuted = afterQueryExecutedCallback;
  }

  /**
   *  Customizes the query using the specified action
   */
  @Override
  public void customize(DocumentQueryCustomizationFactory factory) {
    if (factory == null) {
      return;
    }
    if (customizeQuery == null) {
      customizeQuery = factory;
    }
    // merge 2 factories
    customizeQuery = DocumentQueryCustomizationFactory.join(customizeQuery, factory);
  }

  @Override
  public void transformWith(String transformerName) {
    this.resultTranformer = transformerName;
  }

  @SuppressWarnings("hiding")
  protected <S> RavenQueryProviderProcessor<S> getQueryProviderProcessor(Class<S> clazz) {
    return new RavenQueryProviderProcessor<>(clazz, queryGenerator, customizeQuery, afterQueryExecuted, indexName,
        fieldsToFetch, fieldsToRename, isMapReduce, resultTranformer, transformerParameters);
  }

  /**
   * Convert the expression to a Lucene query
   */
  @SuppressWarnings({"hiding", "unchecked"})
  @Override
  public <S> IDocumentQuery<S> toDocumentQuery(Class<S> clazz, Expression<?> expression) {
    RavenQueryProviderProcessor<T> processor = getQueryProviderProcessor(this.clazz);
    IDocumentQuery<S> result = (IDocumentQuery<S>) processor.getDocumentQueryFor(expression);
    result.setResultTransformer(resultTranformer);
    return result;
  }

  @SuppressWarnings("hiding")
  @Override
  public <S> Lazy<List<S>> lazily(Class<S> clazz, Expression< ? > expression, Action1<List<S>> onEval) {
    final RavenQueryProviderProcessor<S> processor = getQueryProviderProcessor(clazz);
    IDocumentQuery<S> query = processor.getDocumentQueryFor(expression);
    if (afterQueryExecuted != null) {
      query.afterQueryExecuted(afterQueryExecuted);
    }

    List<String> renamedFields = new ArrayList<>();
    outer:
    for (String field :fieldsToFetch) {
      for (RenamedField renamedField : fieldsToRename) {
        if (renamedField.getOriginalField().equals(field)) {
          renamedFields.add(renamedField.getNewField() != null ? renamedField.getNewField() : field);
          continue outer;
        }
      }
      renamedFields.add(field);
    }

    if (!renamedFields.isEmpty()) {
      query.afterQueryExecuted(new Action1<QueryResult>() {
        @Override
        public void apply(QueryResult queryResult) {
          processor.renameResults(queryResult);
        }
      });
    }
    if (!fieldsToFetch.isEmpty()) {
      query = query.selectFields(clazz, fieldsToFetch.toArray(new String[0]), renamedFields.toArray(new String[0]));
    }
    return query.lazily(onEval);
  }

  /**
   * Register the query as a lazy-count query in the session and return a lazy
   * Register the query as a lazy-count query in the session and return a lazy
   */
  @SuppressWarnings("hiding")
  @Override
  public <T> Lazy<Integer> countLazily(Class<T> clazz, Expression<?> expression) {
      RavenQueryProviderProcessor<T> processor = getQueryProviderProcessor(clazz);
      IDocumentQuery<T> query = processor.getDocumentQueryFor(expression);
      return query.countLazily();
  }

  @SuppressWarnings("unchecked")
  @Override
  public <S> IRavenQueryable<S> createQuery(Expression< ? > expression) {
    RavenQueryInspector<S> inspector = new RavenQueryInspector<>();
    inspector.init((Class<S>) clazz, this,
        ravenQueryStatistics, highlightings, indexName, expression, (InMemoryDocumentSessionOperations) queryGenerator, databaseCommands, isMapReduce);
    return inspector;

  }

}
