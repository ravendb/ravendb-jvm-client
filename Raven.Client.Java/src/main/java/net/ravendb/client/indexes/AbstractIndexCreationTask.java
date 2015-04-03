package net.ravendb.client.indexes;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import net.ravendb.abstractions.data.Constants;
import net.ravendb.abstractions.data.Etag;
import net.ravendb.abstractions.data.HttpMethods;
import net.ravendb.abstractions.data.StringDistanceTypes;
import net.ravendb.abstractions.data.IndexStats.IndexingPriority;
import net.ravendb.abstractions.indexing.FieldIndexing;
import net.ravendb.abstractions.indexing.FieldStorage;
import net.ravendb.abstractions.indexing.FieldTermVector;
import net.ravendb.abstractions.indexing.IndexDefinition;
import net.ravendb.abstractions.indexing.IndexReplaceDocument;
import net.ravendb.abstractions.indexing.SortOptions;
import net.ravendb.abstractions.indexing.SpatialOptions;
import net.ravendb.abstractions.indexing.SpatialOptionsFactory;
import net.ravendb.abstractions.indexing.SuggestionOptions;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.client.IDocumentStore;
import net.ravendb.client.connection.IDatabaseCommands;
import net.ravendb.client.connection.ServerClient;
import net.ravendb.client.connection.implementation.HttpJsonRequest;
import net.ravendb.client.document.DocumentConvention;
import net.ravendb.client.document.IndexAndTransformerReplicationMode;
import net.ravendb.client.utils.UrlUtils;

import com.mysema.query.types.Path;

/**
 * Base class for creating indexes
 */
public class AbstractIndexCreationTask extends AbstractCommonApiForIndexesAndTransformers {

  protected DocumentConvention conventions;
  private IndexingPriority priority;

  protected String map;
  protected String reduce;

  protected Long maxIndexOutputsPerDocument;

  protected boolean disableInMemoryIndexing;

  protected Map<Path<?>, FieldStorage> stores;
  protected Map<String, FieldStorage> storesStrings;
  protected Map<Path<?>, SortOptions> indexSortOptions;
  protected Map<String, SortOptions> indexSortOptionsStrings;
  protected Map<Path<?>, String> analyzers;
  protected Map<String, String> analyzersStrings;
  protected Map<Path<?>, SuggestionOptions> indexSuggestions;
  protected Map<Path<?>, FieldTermVector> termVectors;
  protected Map<String, FieldTermVector> termVectorsStrings;
  protected Map<Path<?>, SpatialOptions> spatialIndexes;
  protected Map<String, SpatialOptions> spatialIndexesStrings;
  protected Map<Path<?>, FieldIndexing> indexes;
  protected Map<String, FieldIndexing> indexesStrings;


  public AbstractIndexCreationTask() {
    this.stores = new HashMap<>();
    this.storesStrings = new HashMap<>();
    this.indexes = new HashMap<>();
    this.indexesStrings = new HashMap<>();
    this.indexSortOptions = new HashMap<>();
    this.indexSortOptionsStrings = new HashMap<>();
    this.indexSuggestions = new HashMap<>();
    this.analyzers = new HashMap<>();
    this.analyzersStrings = new HashMap<>();
    this.termVectors = new HashMap<>();
    this.termVectorsStrings = new HashMap<>();
    this.spatialIndexes = new HashMap<>();
    this.spatialIndexesStrings = new HashMap<>();

  }

  /**
   *  index can have a priority that controls how much power of the indexing process it is allowed to consume. index priority can be forced by the user.
   *  There are four available values that you can set: Normal, Idle, Disabled, Abandoned
   * @param priority Default value: null means that the priority of the index is Normal.
   */
  public IndexingPriority getPriority() {
    return priority;
  }

  /**
   *  index can have a priority that controls how much power of the indexing process it is allowed to consume. index priority can be forced by the user.
   *  There are four available values that you can set: Normal, Idle, Disabled, Abandoned
   * @param priority Default value: null means that the priority of the index is Normal.
   */
  public void setPriority(IndexingPriority priority) {
    this.priority = priority;
  }



  /**
   * Gets the conventions that should be used when index definition is created.
   * @return
   */
  public DocumentConvention getConventions() {
    return conventions;
  }

  /**
   * Sets the conventions that should be used when index definition is created.
   */
  public void setConventions(DocumentConvention conventions) {
    this.conventions = conventions;
  }

  /**
   * Generates index name from type name replacing all _ with /
   * e.g.
   * if our type is <code>'Orders_Totals'</code> then index name would be <code>'Orders/Totals'</code>
   */
  public String getIndexName() {
    return getClass().getSimpleName().replace('_', '/');
  }


  public boolean isDisableInMemoryIndexing() {
    return disableInMemoryIndexing;
  }


  public void setDisableInMemoryIndexing(boolean disableInMemoryIndexing) {
    this.disableInMemoryIndexing = disableInMemoryIndexing;
  }

  public void sideBySideExecute(IDocumentStore store) {
    store.sideBySideExecuteIndex(this);
  }

  public void sideBySideExecute(IDocumentStore store, Etag minimumEtagBeforeReplace, Date replaceTimeUtc) {
    store.sideBySideExecuteIndex(this, minimumEtagBeforeReplace, replaceTimeUtc);
  }

  /**
   * Executes the index creation against the specified document store.
   * @param store
   */
  public void execute(IDocumentStore store) {
    store.executeIndex(this);
  }

  /**
   * Executes the index creation using in side-by-side mode.
   * @param databaseCommands
   * @param conventions
   * @param minimumEtagBeforeReplace
   * @param replaceTimeUtc
   */
  public void sideBySideExecute(IDatabaseCommands databaseCommands, DocumentConvention documentConvention,
    Etag minimumEtagBeforeReplace, Date replaceTimeUtc) {
    conventions = documentConvention;
    IndexDefinition indexDefinition = createIndexDefinition();
    IndexDefinition serverDef = databaseCommands.getIndex(getIndexName());
    if (serverDef != null) {
      if (currentOrLegacyIndexDefinitionEquals(documentConvention, serverDef, indexDefinition)) {
        return;
      }

      String replaceIndexName = "ReplacementOf/" + getIndexName();
      databaseCommands.putIndex(replaceIndexName, indexDefinition);

      IndexReplaceDocument indexReplaceDocument = new IndexReplaceDocument();
      indexReplaceDocument.setIndexToReplace(serverDef.getName());
      indexReplaceDocument.setMinimumEtagBeforeReplace(minimumEtagBeforeReplace);
      indexReplaceDocument.setReplaceTimeUtc(replaceTimeUtc);

      databaseCommands.put(Constants.INDEX_REPLACE_PREFIX + replaceIndexName, null, RavenJObject.fromObject(indexReplaceDocument), new RavenJObject());
    } else {
      // since index doesn't exist yet - create it in normal mode
      databaseCommands.putIndex(getIndexName(), indexDefinition);
    }
  }

  @SuppressWarnings({"static-method", "unused"})
  private boolean currentOrLegacyIndexDefinitionEquals(DocumentConvention documentConvention, IndexDefinition serverDef, IndexDefinition indexDefinition) {
    if (serverDef.equals(indexDefinition)) {
      return true;
    }
    return false;
    // in java we don't support pretty printing
  }

  /**
   * Executes the index creation against the specified document database using the specified conventions
   * @param databaseCommands
   * @param documentConvention
   */
  public void execute(final IDatabaseCommands databaseCommands, final DocumentConvention documentConvention) {
    conventions = documentConvention;
    final IndexDefinition indexDefinition = createIndexDefinition();
    // This code take advantage on the fact that RavenDB will turn an index PUT
    // to a noop of the index already exists and the stored definition matches
    // the new definition.
    databaseCommands.putIndex(getIndexName(), indexDefinition, true);

    if (priority != null) {
      databaseCommands.setIndexPriority(getIndexName(), priority);
    }

    if (conventions.getIndexAndTransformerReplicationMode().contains(IndexAndTransformerReplicationMode.INDEXES)) {
      replicateIndexesIfNeeded(databaseCommands);
    }
  }

  private void replicateIndexesIfNeeded(IDatabaseCommands databaseCommands) {
    ServerClient serverClient = (ServerClient) databaseCommands;

    String repliaceIndexUrl = String.format("/replication/replicate-indexes?indexName=%s", UrlUtils.escapeDataString(getIndexName()));
    try (HttpJsonRequest replicateIndexRequest = serverClient.createRequest(HttpMethods.POST, repliaceIndexUrl)) {
      replicateIndexRequest.executeRequest();
    } catch (Exception e) {
      // ignore errors
    }
  }

  public IndexDefinition createIndexDefinition() {
    if (conventions == null) {
      conventions = new DocumentConvention();
    }

    IndexDefinitionBuilder builder = new IndexDefinitionBuilder();
    builder.setIndexes(indexes);
    builder.setIndexesStrings(indexesStrings);
    builder.setSortOptions(indexSortOptions);
    builder.setSortOptionsStrings(indexSortOptionsStrings);
    builder.setAnalyzers(analyzers);
    builder.setAnalyzersStrings(analyzersStrings);
    builder.setMap(map);
    builder.setReduce(reduce);
    builder.setStores(stores);
    builder.setStoresStrings(storesStrings);
    builder.setSuggestions(indexSuggestions);
    builder.setTermVectors(termVectors);
    builder.setTermVectorsStrings(termVectorsStrings);
    builder.setSpatialIndexes(spatialIndexes);
    builder.setSpatialIndexesStrings(spatialIndexesStrings);
    builder.setMaxIndexOutputsPerDocument(maxIndexOutputsPerDocument);
    builder.setDisableInMemoryIndexing(disableInMemoryIndexing);
    return builder.toIndexDefinition(conventions);

  }

  /**
   * Gets a value indicating whether this instance is map reduce index definition
   * @return <code>true</code> if this instance is map reduce; otherwise, <code>false</code>.
   */
  public boolean isMapReduce() {
    return reduce != null;
  }


  /**
   *  Register a field to be indexed
   * @param field
   * @param indexing
   */
  protected void index(Path<?> field, FieldIndexing indexing) {
    indexes.put(field, indexing);
  }

  /**
   * Register a field to be indexed
   * @param field
   * @param indexing
   */
  protected void index(String field, FieldIndexing indexing) {
    indexesStrings.put(field, indexing);
  }

  /**
   * Register a field to be spatially indexed
   *
   * Note: using {@link SpatialOptionsFactory} might be very helpful!
   * @param field
   * @param indexing
   */
  protected void spatial(Path<?> field, SpatialOptions indexing) {
    spatialIndexes.put(field, indexing);
  }

  /**
   * Register a field to be spatially indexed
   *
   * Note: using {@link SpatialOptionsFactory} might be very helpful!
   * @param field
   * @param indexing
   */
  protected void spatial(String field, SpatialOptions indexing) {
    spatialIndexesStrings.put(field, indexing);
  }


  /**
   * Register a field to be stored
   * @param field
   * @param storage
   */
  protected void store(Path<?> field, FieldStorage storage) {
    stores.put(field, storage);
  }

  protected void storeAllFields(FieldStorage storage) {
    storesStrings.put(Constants.ALL_FIELDS, storage);
  }

  /**
   * Register a field to be stored
   * @param field
   * @param storage
   */
  protected void store(String field, FieldStorage storage) {
    storesStrings.put(field, storage);
  }

  /**
   * Register a field to be analyzed
   * @param field
   * @param analyzer
   */
  protected void analyze(Path<?> field, String analyzer) {
    analyzers.put(field, analyzer);
  }

  /**
   * Index specific setting that limits the number of map outputs that an index is allowed to create for a one source document. If a map operation applied to
   * the one document produces more outputs than this number then an index definition will be considered as a suspicious, the indexing of this document
   * will be skipped and the appropriate error message will be added to the indexing errors.
   * Default value: null means that the global value from Raven configuration will be taken to detect if number of outputs was exceeded.
   */
  public Long getMaxIndexOutputsPerDocument() {
    return maxIndexOutputsPerDocument;
  }

  /**
   * Index specific setting that limits the number of map outputs that an index is allowed to create for a one source document. If a map operation applied to
   * the one document produces more outputs than this number then an index definition will be considered as a suspicious, the indexing of this document
   * will be skipped and the appropriate error message will be added to the indexing errors.
   * Default value: null means that the global value from Raven configuration will be taken to detect if number of outputs was exceeded.
   */
  public void setMaxIndexOutputsPerDocument(Long maxIndexOutputsPerDocument) {
    this.maxIndexOutputsPerDocument = maxIndexOutputsPerDocument;
  }

  /**
   * Register a field to be analyzed
   * @param field
   * @param analyzer
   */
  protected void analyze(String field, String analyzer) {
    analyzersStrings.put(field, analyzer);
  }

  /**
   * Register a field to have term vectors
   * @param field
   * @param termVector
   */
  protected void termVector(Path<?> field, FieldTermVector termVector) {
    termVectors.put(field, termVector);
  }

  /**
   * Register a field to have term vectors
   * @param field
   * @param termVector
   */
  protected void termVector(String field, FieldTermVector termVector) {
    termVectorsStrings.put(field, termVector);
  }

  /**
   * Register a field to be sorted
   * @param field
   * @param sort
   */
  protected void sort(Path<?> field, SortOptions sort) {
    indexSortOptions.put(field, sort);
  }

  /**
   * Register a field to be sorted
   * @param field
   * @param sort
   */
  protected void sort(String field, SortOptions sort) {
    indexSortOptionsStrings.put(field, sort);
  }

  /**
   * Register a field to be sorted
   * @param field
   * @param suggestion
   */
  protected void suggestion(Path<?> field, SuggestionOptions suggestion) {
    indexSuggestions.put(field, suggestion);
  }

  /**
   * Register a field to be sorted
   * @param field
   */
  protected void suggestion(Path<?> field) {
    SuggestionOptions options = new SuggestionOptions();
    options.setAccuracy(0.5f);
    options.setDistance(StringDistanceTypes.LEVENSHTEIN);
    suggestion(field, options);
  }


}
