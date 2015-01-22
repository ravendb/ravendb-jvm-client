package net.ravendb.client.connection;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.ravendb.abstractions.basic.CloseableIterator;
import net.ravendb.abstractions.basic.Reference;
import net.ravendb.abstractions.commands.ICommandData;
import net.ravendb.abstractions.connection.OperationCredentials;
import net.ravendb.abstractions.data.Attachment;
import net.ravendb.abstractions.data.AttachmentInformation;
import net.ravendb.abstractions.data.BatchResult;
import net.ravendb.abstractions.data.BuildNumber;
import net.ravendb.abstractions.data.BulkInsertOptions;
import net.ravendb.abstractions.data.BulkOperationOptions;
import net.ravendb.abstractions.data.DatabaseStatistics;
import net.ravendb.abstractions.data.Etag;
import net.ravendb.abstractions.data.Facet;
import net.ravendb.abstractions.data.FacetQuery;
import net.ravendb.abstractions.data.FacetResults;
import net.ravendb.abstractions.data.GetRequest;
import net.ravendb.abstractions.data.GetResponse;
import net.ravendb.abstractions.data.HttpMethods;
import net.ravendb.abstractions.data.IndexQuery;
import net.ravendb.abstractions.data.JsonDocument;
import net.ravendb.abstractions.data.JsonDocumentMetadata;
import net.ravendb.abstractions.data.MoreLikeThisQuery;
import net.ravendb.abstractions.data.MultiLoadResult;
import net.ravendb.abstractions.data.PatchRequest;
import net.ravendb.abstractions.data.PutResult;
import net.ravendb.abstractions.data.QueryHeaderInformation;
import net.ravendb.abstractions.data.QueryResult;
import net.ravendb.abstractions.data.ScriptedPatchRequest;
import net.ravendb.abstractions.data.SuggestionQuery;
import net.ravendb.abstractions.data.SuggestionQueryResult;
import net.ravendb.abstractions.exceptions.ServerClientException;
import net.ravendb.abstractions.indexing.IndexDefinition;
import net.ravendb.abstractions.indexing.IndexMergeResults;
import net.ravendb.abstractions.indexing.TransformerDefinition;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.json.linq.RavenJToken;
import net.ravendb.client.RavenPagingInformation;
import net.ravendb.client.changes.IDatabaseChanges;
import net.ravendb.client.connection.implementation.HttpJsonRequest;
import net.ravendb.client.connection.profiling.IHoldProfilingInformation;
import net.ravendb.client.document.ILowLevelBulkInsertOperation;
import net.ravendb.client.indexes.IndexDefinitionBuilder;


public interface IDatabaseCommands extends IHoldProfilingInformation {

  /**
   * @return Gets the operations headers
   */
  public Map<String, String> getOperationsHeaders();

  /**
   * Sets the operations headers
   * @param operationsHeaders
   */
  public void setOperationsHeaders(Map<String, String> operationsHeaders);

  /**
   * Admin operations performed against system database, like create/delete database
   */
  public IGlobalAdminDatabaseCommands getGlobalAdmin();

  public OperationCredentials getPrimaryCredentials();

  /**
   * @return Admin operations for current database
   */
  public IAdminDatabaseCommands getAdmin();

  /**
   * Retrieves the document for the specified key
   * @param key The key
   */
  public JsonDocument get(String key) throws ServerClientException;

  /**
   * Gets the results for the specified ids.
   * @param ids
   * @param includes
   */
  public MultiLoadResult get(final String[] ids, final String[] includes);

  /**
   * Gets the results for the specified ids.
   * @param ids
   * @param includes
   * @param transformer
   */
  public MultiLoadResult get(final String[] ids, final String[] includes, final String transformer);

  /**
   * Gets the results for the specified ids.
   * @param ids
   * @param includes
   * @param transformer
   * @param transformerParameters
   */
  public MultiLoadResult get(final String[] ids, final String[] includes, final String transformer, final Map<String, RavenJToken> transformerParameters);

  /**
   * @param ids
   * @param includes
   * @param transformer
   * @param transformerParameters
   * @param metadataOnly
   */
  public MultiLoadResult get(final String[] ids, final String[] includes, final String transformer, final Map<String, RavenJToken> transformerParameters, final boolean metadataOnly);

  /**
   * Get documents from server
   * @param start
   * @param pageSize
   */
  public List<JsonDocument> getDocuments(int start, int pageSize);

  /**
   * Get documents from server
   * @param start Paging start
   * @param pageSize Size of the page.
   * @param metadataOnly Load just the document metadata
   */
  public List<JsonDocument> getDocuments(int start, int pageSize, boolean metadataOnly);

  /**
   * Performs index query
   * @param index
   * @param query
   */
  public QueryResult query(String index, IndexQuery query);

  /**
   * Performs index query
   * @param index
   * @param query
   * @param includes
   */
  public QueryResult query(String index, IndexQuery query, String[] includes);

  /**
   * Performs index query
   * @param index
   * @param query
   * @param includes
   * @param metadataOnly
   */
  public QueryResult query(String index, IndexQuery query, String[] includes, boolean metadataOnly);

  /**
   * Performs index query
   * @param index
   * @param query
   * @param includes
   * @param metadataOnly
   * @param indexEntriesOnly
   */
  public QueryResult query(String index, IndexQuery query, String[] includes, boolean metadataOnly, boolean indexEntriesOnly);

  /**
   * Executed the specified commands as a single batch
   * @param commandDatas
   * @throws IOException
   */
  public BatchResult[] batch(final List<ICommandData> commandDatas) throws IOException;

  /**
   * Returns a list of suggestions based on the specified suggestion query
   * @param index
   * @param suggestionQuery
   */
  public SuggestionQueryResult suggest(final String index, final SuggestionQuery suggestionQuery);

  /**
   * Gets the index names from the server
   * @param start
   * @param pageSize
   */
  public Collection<String> getIndexNames(final int start, final int pageSize);

  /**
   * Returns {@link IndexDefinition}s
   * @param start
   * @param pageSize
   */
  public Collection<IndexDefinition> getIndexes(final int start, final int pageSize);

  /**
   * Gets the transformers from the server
   * @param start
   * @param pageSize
   */
  public List<TransformerDefinition> getTransformers(int start, int pageSize);

  /**
   * Resets the specified index
   * @param name
   */
  public void resetIndex(String name);

  /**
   * Gets the index definition for the specified name
   * @param name
   */
  public IndexDefinition getIndex(String name);

  /**
   *  Gets the transformer definition for the specified name
   * @param name
   */
  public TransformerDefinition getTransformer(String name);

  /**
   * Puts index with given definition
   * @param name
   * @param definition
   */
  public String putIndex(String name, IndexDefinition definition);

  /**
   * Creates a transformer with the specified name, based on an transformer definition
   * @param name
   * @param transformerDef
   */
  public String putTransformer(String name, TransformerDefinition transformerDef);

  /**
   * Puts the index.
   * @param name
   * @param definition
   * @param overwrite
   */
  public String putIndex(final String name, final IndexDefinition definition, final boolean overwrite);

  /**
   * Creates an index with the specified name, based on an index definition
   * @param name
   * @param indexDef
   */
  public String putIndex(String name, IndexDefinitionBuilder indexDef);

  /**
   * Creates an index with the specified name, based on an index definition
   * @param name
   * @param indexDef
   * @param overwrite
   */
  public String putIndex(String name, IndexDefinitionBuilder indexDef, boolean overwrite);

  /**
   * Checks if passed index definition matches version stored on server.
   * If index does not exist this method returns true.
   * @param name
   * @param indexDef
   */
  public boolean indexHasChanged(String name, IndexDefinition indexDef);

  /**
   * Delete index
   * @param name
   */
  public void deleteIndex(final String name);

  /**
   * Perform a set based deletes using the specified index.
   * @param indexName
   * @param queryToDelete
   * @param options
   */
  public Operation deleteByIndex(final String indexName, final IndexQuery queryToDelete, final BulkOperationOptions options);

  /**
   * Perform a set based deletes using the specified index, not allowing the operation
   *  if the index is stale
   * @param indexName
   * @param queryToDelete
   */
  public Operation deleteByIndex(String indexName, IndexQuery queryToDelete);

  /**
   * Deletes the specified transformer
   * @param name
   */
  public void deleteTransformer(String name);

  /**
   * Puts the document in the database with the specified key
   * @param key The key.
   * @param guid The etag.
   * @param document The document.
   * @param metadata The metadata.
   * @return PutResult
   */
  public PutResult put(String key, Etag guid, RavenJObject document, RavenJObject metadata);

  /**
   * Sends a patch request for a specific document, ignoring document's Etag and if it is missing
   * @param key Key of the document to patch
   * @param patches Array of patch requests
   */
  public RavenJObject patch(String key, PatchRequest[] patches);

  /**
   * Sends a patch request for a specific document, ignoring the document's Etag
   * @param key Key of the document to patch
   * @param patches Array of patch requests
   * @param ignoreMissing true if the patch request should ignore a missing document, false to throw DocumentDoesNotExistException
   */
  public RavenJObject patch(String key, PatchRequest[] patches, boolean ignoreMissing);

  /**
   * Sends a patch request for a specific document, ignoring the document's Etag and  if the document is missing
   * @param key Key of the document to patch
   * @param patch The patch request to use (using JavaScript)
   */
  public RavenJObject patch(String key, ScriptedPatchRequest patch);

  /**
   * Sends a patch request for a specific document, ignoring the document's Etag
   * @param key Key of the document to patch
   * @param patch The patch request to use (using JavaScript)
   * @param ignoreMissing true if the patch request should ignore a missing document, false to throw DocumentDoesNotExistException
   */
  public RavenJObject patch(String key, ScriptedPatchRequest patch, boolean ignoreMissing);

  /**
   * Sends a patch request for a specific document
   * @param key Key of the document to patch
   * @param patches Array of patch requests
   * @param etag Require specific Etag [null to ignore]
   */
  public RavenJObject patch(String key, PatchRequest[] patches, Etag etag);

  /**
   * Sends a patch request for a specific document which may or may not currently exist
   * @param key Id of the document to patch
   * @param patchesToExisting Array of patch requests to apply to an existing document
   * @param patchesToDefault Array of patch requests to apply to a default document when the document is missing
   * @param defaultMetadata The metadata for the default document when the document is missing
   */
  public RavenJObject patch(String key, PatchRequest[] patchesToExisting, PatchRequest[] patchesToDefault, RavenJObject defaultMetadata);

  /**
   * Sends a patch request for a specific document
   * @param key Key of the document to patch
   * @param patch The patch request to use (using JavaScript)
   * @param etag Require specific Etag [null to ignore]
   */
  public RavenJObject patch(String key, ScriptedPatchRequest patch, Etag etag);

  /**
   * Sends a patch request for a specific document which may or may not currently exist
   * @param key Id of the document to patch
   * @param patchExisting The patch request to use (using JavaScript) to an existing document
   * @param patchDefault The patch request to use (using JavaScript)  to a default document when the document is missing
   * @param defaultMetadata The metadata for the default document when the document is missing
   */
  public RavenJObject patch(String key, ScriptedPatchRequest patchExisting, ScriptedPatchRequest patchDefault, RavenJObject defaultMetadata);

  /**
   * Create a new instance of {@link IDatabaseCommands} that will interacts with the specified database
   * @param database
   */
  public IDatabaseCommands forDatabase(String database);

  /**
   * Creates a new instance of {@link IDatabaseCommands} that will interacts with the default database.
   */
  public IDatabaseCommands forSystemDatabase();

  /**
   * Returns server statistics
   */
  public DatabaseStatistics getStatistics();

  /**
   * Gets the attachment by the specified key
   * @param key
   */
  @Deprecated
  public Attachment getAttachment(String key);

  @Deprecated
  public AttachmentInformation[] getAttachments(int start, Etag startEtag, int pageSize);

  /**
   * Retrieves the attachment metadata with the specified key, not the actual attachment
   * @param key
   */
  @Deprecated
  public Attachment headAttachment(String key);

  /**
   * Deletes the attachment with the specified key
   * @param key The key.
   * @param etag The etag.
   */
  @Deprecated
  public void deleteAttachment(String key, Etag etag);

  /**
   * Get the all terms stored in the index for the specified field
   * You can page through the results by use fromValue parameter as the
   * starting point for the next query
   * @param index
   * @param field
   * @param fromValue
   * @param pageSize
   */
  public List<String> getTerms(String index, String field, String fromValue, int pageSize);

  /**
   * Disable all caching within the given scope
   */
  public AutoCloseable disableAllCaching();

  /**
   * Perform a single POST request containing multiple nested GET requests
   * @param requests
   */
  public GetResponse[] multiGet(GetRequest[] requests);

  /**
   * Perform a set based update using the specified index, not allowing the operation
   * if the index is stale
   * @param indexName
   * @param queryToUpdate
   * @param patchRequests
   */
  public Operation updateByIndex(String indexName, IndexQuery queryToUpdate, PatchRequest[] patchRequests);

  /**
   * Perform a set based update using the specified index, not allowing the operation
   * if the index is stale
   * @param indexName
   * @param queryToUpdate
   * @param patch
   */
  public Operation updateByIndex(String indexName, IndexQuery queryToUpdate, ScriptedPatchRequest patch);

  /**
   * Perform a set based update using the specified index, not allowing the operation
   * if the index is stale
   * @param indexName
   * @param queryToUpdate
   * @param patchRequests
   * @param options
   */
  public Operation updateByIndex(String indexName, IndexQuery queryToUpdate, PatchRequest[] patchRequests, BulkOperationOptions options);

  /**
   * Perform a set based update using the specified index, not allowing the operation
   * if the index is stale
   * @param indexName
   * @param queryToUpdate
   * @param patch
   * @param options
   */
  public Operation updateByIndex(String indexName, IndexQuery queryToUpdate, ScriptedPatchRequest patch, BulkOperationOptions options);

  /**
   *  Using the given Index, calculate the facets as per the specified doc with the given start and pageSize
   * @param index
   * @param query
   * @param facetSetupDoc
   */
  public FacetResults getFacets(String index, IndexQuery query, String facetSetupDoc);

  /**
   *  Using the given Index, calculate the facets as per the specified doc with the given start and pageSize
   * @param index
   * @param query
   * @param facetSetupDoc
   * @param start
   */
  public FacetResults getFacets(String index, IndexQuery query, String facetSetupDoc, int start);

  /**
   *  Using the given Index, calculate the facets as per the specified doc with the given start and pageSize
   * @param index
   * @param query
   * @param facetSetupDoc
   * @param start
   * @param pageSize
   */
  public FacetResults getFacets(String index, IndexQuery query, String facetSetupDoc, int start, Integer pageSize);

  /**
   * Using the given Index, calculate the facets as per the specified doc with the given start and pageSize
   * @param index
   * @param query
   * @param facets
   */
  public FacetResults getFacets(final String index, final IndexQuery query, final List<Facet> facets) ;


  /**
   * Using the given Index, calculate the facets as per the specified doc with the given start and pageSize
   * @param index
   * @param query
   * @param facets
   * @param start
   */
  public FacetResults getFacets(final String index, final IndexQuery query, final List<Facet> facets, final int start) ;


  /**
   * Using the given Index, calculate the facets as per the specified doc with the given start and pageSize
   * @param index
   * @param query
   * @param facets
   * @param start
   * @param pageSize
   */
  public FacetResults getFacets(final String index, final IndexQuery query, final List<Facet> facets, final int start, final Integer pageSize) ;

  /**
   * Retrieves documents for the specified key prefix
   * @param keyPrefix
   * @param matches
   * @param start
   * @param pageSize
   */
  public List<JsonDocument> startsWith(String keyPrefix, String matches, int start, int pageSize) throws ServerClientException;


  /**
   * Retrieves documents for the specified key prefix
   * @param keyPrefix
   * @param matches
   * @param start
   * @param pageSize
   * @param metadataOnly
   */
  public List<JsonDocument> startsWith(String keyPrefix, String matches, int start, int pageSize, boolean metadataOnly);

  /**
   * Retrieves documents for the specified key prefix
   * @param keyPrefix
   * @param matches
   * @param start
   * @param pageSize
   * @param metadataOnly
   */
  public List<JsonDocument> startsWith(String keyPrefix, String matches, int start, int pageSize, boolean metadataOnly, String exclude);

  /**
   * Retrieves documents for the specified key prefix
   * @param keyPrefix
   * @param matches
   * @param start
   * @param pageSize
   * @param metadataOnly
   */
  public List<JsonDocument> startsWith(String keyPrefix, String matches, int start, int pageSize, boolean metadataOnly, String exclude, RavenPagingInformation pagingInformation);

  public List<JsonDocument> startsWith(String keyPrefix, String matches, int start, int pageSize, boolean metadataOnly,
    String exclude, RavenPagingInformation pagingInformation, String transformer, Map<String, RavenJToken> transformerParameters);

  public List<JsonDocument> startsWith(String keyPrefix, String matches, int start, int pageSize, boolean metadataOnly,
    String exclude, RavenPagingInformation pagingInformation, String transformer, Map<String, RavenJToken> transformerParameters, String skipAfter);

  /**
   * Seeds the next identity value on the server
   * @param name
   * @param value
   */
  public long seedIdentityFor(final String name, final long value) ;

  /**
   * Deletes the document with the specified key
   * @param key The key.
   * @param etag The etag.
   */
  public void delete(String key, Etag etag);

  /**
   * Puts a byte array as attachment with the specified key
   * @param key The key.
   * @param etag The etag.
   * @param data The data.
   * @param metadata The metadata.
   */
  @Deprecated
  public void putAttachment(String key, Etag etag, InputStream data, RavenJObject metadata);

  /**
   *  Updates just the attachment with the specified key's metadata
   * @param key The key.
   * @param etag The etag.
   * @param metadata The metadata.
   */
  @Deprecated
  public void updateAttachmentMetadata(String key, Etag etag, RavenJObject metadata);

  /**
   * Gets the attachments starting with the specified prefix
   * @param idPrefix
   * @param start
   * @param pageSize
   */
  @Deprecated
  public List<Attachment> getAttachmentHeadersStartingWith(String idPrefix, int start, int pageSize);

  /**
   *  Queries the specified index in the Raven flavored Lucene query syntax. Will return *all* results, regardless
   *  of the number of items that might be returned.
   * @param index
   * @param query
   * @param queryHeaderInfo
   */
  public CloseableIterator<RavenJObject> streamQuery(String index, IndexQuery query, Reference<QueryHeaderInformation> queryHeaderInfo) ;

  /**
   * Streams the documents by etag OR starts with the prefix and match the matches
   * Will return *all* results, regardless of the number of items that might be returned.
   */
  public CloseableIterator<RavenJObject> streamDocs();

  /**
   * Streams the documents by etag OR starts with the prefix and match the matches
   * Will return *all* results, regardless of the number of items that might be returned.
   * @param fromEtag
   */
  public CloseableIterator<RavenJObject> streamDocs(Etag fromEtag);

  /**
   * Streams the documents by etag OR starts with the prefix and match the matches
   * Will return *all* results, regardless of the number of items that might be returned.
   * @param fromEtag
   * @param startsWith
   */
  public CloseableIterator<RavenJObject> streamDocs(Etag fromEtag, String startsWith);

  /**
   * Streams the documents by etag OR starts with the prefix and match the matches
   * Will return *all* results, regardless of the number of items that might be returned.
   * @param fromEtag
   * @param startsWith
   * @param matches
   */
  public CloseableIterator<RavenJObject> streamDocs(Etag fromEtag, String startsWith, String matches);

  /**
   * Streams the documents by etag OR starts with the prefix and match the matches
   * Will return *all* results, regardless of the number of items that might be returned.
   * @param fromEtag
   * @param startsWith
   * @param matches
   * @param start
   */
  public CloseableIterator<RavenJObject> streamDocs(Etag fromEtag, String startsWith, String matches, int start);

  /**
   * Streams the documents by etag OR starts with the prefix and match the matches
   * Will return *all* results, regardless of the number of items that might be returned.
   * @param fromEtag
   * @param startsWith
   * @param matches
   * @param start
   * @param pageSize
   */
  public CloseableIterator<RavenJObject> streamDocs(Etag fromEtag, String startsWith, String matches, int start, int pageSize);

  /**
   * Streams the documents by etag OR starts with the prefix and match the matches
   * Will return *all* results, regardless of the number of items that might be returned.
   * @param fromEtag
   * @param startsWith
   * @param matches
   * @param start
   * @param pageSize
   */
  public CloseableIterator<RavenJObject> streamDocs(Etag fromEtag, String startsWith, String matches, int start, int pageSize, String exclude);

  /**
   * Streams the documents by etag OR starts with the prefix and match the matches
   * Will return *all* results, regardless of the number of items that might be returned.
   * @param fromEtag
   * @param startsWith
   * @param matches
   * @param start
   * @param pageSize
   * @param pagingInformation
   */
  public CloseableIterator<RavenJObject> streamDocs(Etag fromEtag, String startsWith, String matches, int start, int pageSize, String exclude, RavenPagingInformation pagingInformation);

  /**
   * Streams the documents by etag OR starts with the prefix and match the matches
   * Will return *all* results, regardless of the number of items that might be returned.
   * @param fromEtag
   * @param startsWith
   * @param matches
   * @param start
   * @param pageSize
   * @param pagingInformation
   * @param skipAfter
   */
  public CloseableIterator<RavenJObject> streamDocs(Etag fromEtag, String startsWith, String matches, int start, int pageSize, String exclude, RavenPagingInformation pagingInformation, String skipAfter);

  /**
   * Return a list of documents that based on the MoreLikeThisQuery.
   * @param query
   */
  public MultiLoadResult moreLikeThis(MoreLikeThisQuery query);

  /**
   * Sends a multiple faceted queries in a single request and calculates the facet results for each of them
   * @param facetedQueries
   */
  public FacetResults[] getMultiFacets(FacetQuery[] facetedQueries);


  /**
   * Checks if the document exists for the specified key
   * @param key The key.
   */
  public JsonDocumentMetadata head(String key);

  /**
   * Generate the next identity value from the server
   * @param name
   */
  public Long nextIdentityFor(String name);

  /**
   * Get the full URL for the given document key
   * @param documentKey
   */
  public String urlFor(String documentKey);

  /**
   * Force the database commands to read directly from the master, unless there has been a failover.
   */
  public AutoCloseable forceReadFromMaster();

  public ILowLevelBulkInsertOperation getBulkInsertOperation(BulkInsertOptions options, IDatabaseChanges changes);

  public IndexMergeResults getIndexMergeSuggestions() throws IOException;

  /**
   * Tries to resolve conflict using registered listeners
   * @param key
   * @param etag
   * @param conflictedIds
   * @param opUrl
   */
  public Boolean tryResolveConflictByUsingRegisteredListeners(OperationMetadata operationMetadata, String key, Etag etag, List<String> conflictedIds,
    OperationMetadata opUrl);

  /**
   * Internal use
   */
  public HttpJsonRequest createRequest(HttpMethods method, String requestUrl);

  public BuildNumber getBuildNumber() throws IOException;

}
