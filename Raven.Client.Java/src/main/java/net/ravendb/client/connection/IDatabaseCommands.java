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
   * Gets the operations headers
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

  /**
   * Primary credentials for access. Will be used also in replication context - for failovers
   */
  public OperationCredentials getPrimaryCredentials();

  /**
   * Admin operations for current database
   */
  public IAdminDatabaseCommands getAdmin();

  /**
   * Retrieve a single document for a specified key.
   * @param key Key of the document you want to retrieve
   */
  public JsonDocument get(String key) throws ServerClientException;

  /**
   * Retrieves documents with the specified ids, optionally specifying includes to fetch along and also optionally the transformer.
   * Returns MultiLoadResult where:
   * - Results - list of documents in exact same order as in keys parameter
   * - Includes - list of documents that were found in specified paths that were passed in includes parameter
   * @param ids Array of keys of the documents you want to retrieve
   * @param includes Array of paths in documents in which server should look for a 'referenced' document
   */
  public MultiLoadResult get(final String[] ids, final String[] includes);

  /**
   * Retrieves documents with the specified ids, optionally specifying includes to fetch along and also optionally the transformer.
   * Returns MultiLoadResult where:
   * - Results - list of documents in exact same order as in keys parameter
   * - Includes - list of documents that were found in specified paths that were passed in includes parameter
   * @param ids Array of keys of the documents you want to retrieve
   * @param includes Array of paths in documents in which server should look for a 'referenced' document
   * @param transformer Name of a transformer that should be used to transform the results
   */
  public MultiLoadResult get(final String[] ids, final String[] includes, final String transformer);

  /**
   * Retrieves documents with the specified ids, optionally specifying includes to fetch along and also optionally the transformer.
   * Returns MultiLoadResult where:
   * - Results - list of documents in exact same order as in keys parameter
   * - Includes - list of documents that were found in specified paths that were passed in includes parameter
   * @param ids Array of keys of the documents you want to retrieve
   * @param includes Array of paths in documents in which server should look for a 'referenced' document
   * @param transformer Name of a transformer that should be used to transform the results
   * @param transformerParameters Parameters that will be passed to transformer
   */
  public MultiLoadResult get(final String[] ids, final String[] includes, final String transformer, final Map<String, RavenJToken> transformerParameters);

  /**
   * Retrieves documents with the specified ids, optionally specifying includes to fetch along and also optionally the transformer.
   * Returns MultiLoadResult where:
   * - Results - list of documents in exact same order as in keys parameter
   * - Includes - list of documents that were found in specified paths that were passed in includes parameter
   * @param ids Array of keys of the documents you want to retrieve
   * @param includes Array of paths in documents in which server should look for a 'referenced' document
   * @param transformer Name of a transformer that should be used to transform the results
   * @param transformerParameters Parameters that will be passed to transformer
   * @param metadataOnly Specifies if only document metadata should be returned
   */
  public MultiLoadResult get(final String[] ids, final String[] includes, final String transformer, final Map<String, RavenJToken> transformerParameters, final boolean metadataOnly);

  /**
   * Retrieves multiple documents.
   * @param start Number of documents that should be skipped
   * @param pageSize Maximum number of documents that will be retrieved
   */
  public List<JsonDocument> getDocuments(int start, int pageSize);

  /**
   * Retrieves multiple documents.
   * @param start Number of documents that should be skipped
   * @param pageSize Maximum number of documents that will be retrieved
   * @param metadataOnly Specifies if only document metadata should be returned
   */
  public List<JsonDocument> getDocuments(int start, int pageSize, boolean metadataOnly);

  /**
   * Queries the specified index in the Raven-flavored Lucene query syntax
   * @param index Name of an index to query
   * @param query Query definition containing all information required to query a specified index
   */
  public QueryResult query(String index, IndexQuery query);

  /**
   * Queries the specified index in the Raven-flavored Lucene query syntax
   * @param index Name of an index to query
   * @param query Query definition containing all information required to query a specified index
   * @param includes An array of relative paths that specify related documents ids which should be included in a query result
   */
  public QueryResult query(String index, IndexQuery query, String[] includes);

  /**
   * Queries the specified index in the Raven-flavored Lucene query syntax
   * @param index Name of an index to query
   * @param query Query definition containing all information required to query a specified index
   * @param includes An array of relative paths that specify related documents ids which should be included in a query result
   * @param metadataOnly True if returned documents should include only metadata without a document body.
   */
  public QueryResult query(String index, IndexQuery query, String[] includes, boolean metadataOnly);

  /**
   * Queries the specified index in the Raven-flavored Lucene query syntax
   * @param index Name of an index to query
   * @param query Query definition containing all information required to query a specified index
   * @param includes An array of relative paths that specify related documents ids which should be included in a query result
   * @param metadataOnly True if returned documents should include only metadata without a document body.
   * @param indexEntriesOnly True if query results should contain only index entries.
   */
  public QueryResult query(String index, IndexQuery query, String[] includes, boolean metadataOnly, boolean indexEntriesOnly);

  /**
   * Sends multiple operations in a single request, reducing the number of remote calls and allowing several operations to share same transaction
   * @param commandDatas Commands to process
   * @throws IOException
   */
  public BatchResult[] batch(final List<ICommandData> commandDatas) throws IOException;

  /**
   * Returns a list of suggestions based on the specified suggestion query
   * @param index Name of an index to query
   * @param suggestionQuery Suggestion query definition containing all information required to query a specified index
   */
  public SuggestionQueryResult suggest(final String index, final SuggestionQuery suggestionQuery);

  /**
   * Retrieves multiple index names from a database.
   * @param start Number of index names that should be skipped
   * @param pageSize Maximum number of index names that will be retrieved
   */
  public Collection<String> getIndexNames(final int start, final int pageSize);

  /**
   * Retrieves multiple index definitions from a database
   * @param start Number of indexes that should be skipped
   * @param pageSize Maximum number of indexes that will be retrieved
   */
  public Collection<IndexDefinition> getIndexes(final int start, final int pageSize);

  /**
   * Gets the transformers from the server
   * @param start Number of transformers that should be skipped
   * @param pageSize Maximum number of transformers that will be retrieved
   */
  public List<TransformerDefinition> getTransformers(int start, int pageSize);

  /**
   * Removes all indexing data from a server for a given index so the indexation can start from scratch for that index.
   * @param name Name of an index to reset
   */
  public void resetIndex(String name);

  /**
   * Retrieves an index definition from a database.
   * @param name Name of an index
   */
  public IndexDefinition getIndex(String name);

  /**
   * Gets the transformer definition for the specified name
   * @param name Transformer name
   */
  public TransformerDefinition getTransformer(String name);

  /**
   * Creates an index with the specified name, based on an index definition
   * @param name Name of an index
   * @param definition Definition of an index
   */
  public String putIndex(String name, IndexDefinition definition);

  /**
   * Creates a transformer with the specified name, based on an transformer definition
   * @param name Name of a transformer
   * @param transformerDef Definition of a transformer
   */
  public String putTransformer(String name, TransformerDefinition transformerDef);

  /**
   * Creates an index with the specified name, based on an index definition
   * @param name Name of an index
   * @param definition Definition of an index
   * @param overwrite If set to true [overwrite]
   */
  public String putIndex(final String name, final IndexDefinition definition, final boolean overwrite);

  /**
   * Creates an index with the specified name, based on an index definition that is created by the supplied IndexDefinitionBuilder
   * @param name Name of an index
   * @param indexDef Definition of an index
   */
  public String putIndex(String name, IndexDefinitionBuilder indexDef);

  /**
   * Creates an index with the specified name, based on an index definition that is created by the supplied IndexDefinitionBuilder
   * @param name Name of an index
   * @param indexDef Definition of an index
   * @param overwrite If set to true [overwrite]
   */
  public String putIndex(String name, IndexDefinitionBuilder indexDef, boolean overwrite);

  /**
   * Lets you check if the given index definition differs from the one on a server.
   *
   * This might be useful when you want to check the prior index deployment, if index will be overwritten, and if
   * indexing data will be lost.
   * Returns:
   * - true - if an index does not exist on a server
   * - true - if an index definition does not match the one from the indexDef parameter,
   * - false - if there are no differences between an index definition on server and the one from the indexDef parameter
   * If index does not exist this method returns true.
   * @param name Name of an index to check
   * @param indexDef Index definition
   */
  public boolean indexHasChanged(String name, IndexDefinition indexDef);

  /**
   * Deletes the specified index
   * @param name Name of an index to delete
   */
  public void deleteIndex(final String name);

  /**
   * Perform a set based deletes using the specified index
   * @param indexName Name of an index to perform a query on
   * @param queryToDelete Query that will be performed
   * @param options Various operation options e.g. AllowStale or MaxOpsPerSec
   */
  public Operation deleteByIndex(final String indexName, final IndexQuery queryToDelete, final BulkOperationOptions options);

  /**
   * Perform a set based deletes using the specified index
   * @param indexName Name of an index to perform a query on
   * @param queryToDelete Query that will be performed
   */
  public Operation deleteByIndex(String indexName, IndexQuery queryToDelete);

  /**
   * Deletes the specified transformer
   * @param name Name of a transformer to delete
   */
  public void deleteTransformer(String name);

  /**
   * Puts the document in the database with the specified key.
   *
   * Returns PutResult where:
   * - Key - unique key under which document was stored,
   * - Etag - stored document etag
   * @param key Unique key under which document will be stored
   * @param guid Current document etag, used for concurrency checks (null to skip check)
   * @param document Document data
   * @param metadata Document metadata
   */
  public PutResult put(String key, Etag guid, RavenJObject document, RavenJObject metadata);

  /**
   * Sends a patch request for a specific document, ignoring the document's Etag and if the document is missing
   * @param key Id of the document to patch
   * @param patches Array of patch requests
   */
  public RavenJObject patch(String key, PatchRequest[] patches);

  /**
   * Sends a patch request for a specific document, ignoring the document's Etag
   * @param key Id of the document to patch
   * @param patches Array of patch requests
   * @param ignoreMissing True if the patch request should ignore a missing document, false to throw DocumentDoesNotExistException
   */
  public RavenJObject patch(String key, PatchRequest[] patches, boolean ignoreMissing);

  /**
   * Sends a patch request for a specific document, ignoring the document's Etag and  if the document is missing
   * @param key Id of the document to patch
   * @param patch The patch request to use (using JavaScript)
   */
  public RavenJObject patch(String key, ScriptedPatchRequest patch);

  /**
   * Sends a patch request for a specific document, ignoring the document's Etag
   * @param key Id of the document to patch
   * @param patch The patch request to use (using JavaScript)
   * @param ignoreMissing true if the patch request should ignore a missing document, false to throw DocumentDoesNotExistException
   */
  public RavenJObject patch(String key, ScriptedPatchRequest patch, boolean ignoreMissing);

  /**
   * Sends a patch request for a specific document
   * @param key Id of the document to patch
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
   * @param key Id of the document to patch
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
   * Retrieve the statistics for the database
   */
  public DatabaseStatistics getStatistics();

  /**
   * Downloads a single attachment.
   * @param key Key of the attachment you want to download
   * @deprecated Use RavenFS instead.
   */
  @Deprecated
  public Attachment getAttachment(String key);

  /**
   * Used to download attachment information for multiple attachments.
   * @param start Indicates how many attachments should be skipped
   * @param startEtag ETag from which to start
   * @param pageSize Maximum number of attachments that will be downloaded
   * @deprecated Use RavenFS instead.
   */
  @Deprecated
  public AttachmentInformation[] getAttachments(int start, Etag startEtag, int pageSize);

  /**
   * Download attachment metadata for a single attachment.
   * @param key Key of the attachment you want to download metadata for
   */
  @Deprecated
  public Attachment headAttachment(String key);

  /**
   * Removes an attachment from a database.
   * @param key Key of an attachment to delete
   * @param etag Current attachment etag, used for concurrency checks (null to skip check)
   * @deprecated Use RavenFS instead.
   */
  @Deprecated
  public void deleteAttachment(String key, Etag etag);

  /**
   * Get the all terms stored in the index for the specified field
   * You can page through the results by use fromValue parameter as the
   * starting point for the next query
   * @param index Name of an index
   * @param field Index field
   * @param fromValue Starting point for a query, used for paging
   * @param pageSize Maximum number of terms that will be returned
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
   * Perform a set based update using the specified index
   * @param indexName Name of an index to perform a query on
   * @param queryToUpdate Query that will be performed
   * @param patchRequests Array of patches that will be executed on a query results
   */
  public Operation updateByIndex(String indexName, IndexQuery queryToUpdate, PatchRequest[] patchRequests);

  /**
   * Perform a set based update using the specified index
   * @param indexName Name of an index to perform a query on
   * @param queryToUpdate Query that will be performed
   * @param patch JavaScript patch that will be executed on query results
   */
  public Operation updateByIndex(String indexName, IndexQuery queryToUpdate, ScriptedPatchRequest patch);

  /**
   * Perform a set based update using the specified index
   * @param indexName Name of an index to perform a query on
   * @param queryToUpdate Query that will be performed
   * @param patchRequests Array of patches that will be executed on a query results
   * @param options Various operation options e.g. AllowStale or MaxOpsPerSec
   */
  public Operation updateByIndex(String indexName, IndexQuery queryToUpdate, PatchRequest[] patchRequests, BulkOperationOptions options);

  /**
   * Perform a set based update using the specified index
   * @param indexName Name of an index to perform a query on
   * @param queryToUpdate Query that will be performed
   * @param patch JavaScript patch that will be executed on query results
   * @param options Various operation options e.g. AllowStale or MaxOpsPerSec
   */
  public Operation updateByIndex(String indexName, IndexQuery queryToUpdate, ScriptedPatchRequest patch, BulkOperationOptions options);

  /**
   * Using the given Index, calculate the facets as per the specified doc with the given start and pageSize
   * @param index Name of an index to query
   * @param query Query definition containing all information required to query a specified index
   * @param facetSetupDoc Document key that contains predefined FacetSetup
   */
  public FacetResults getFacets(String index, IndexQuery query, String facetSetupDoc);

  /**
   * Using the given Index, calculate the facets as per the specified doc with the given start and pageSize
   * @param index Name of an index to query
   * @param query Query definition containing all information required to query a specified index
   * @param facetSetupDoc Document key that contains predefined FacetSetup
   * @param start Number of results that should be skipped. Default: 0
   */
  public FacetResults getFacets(String index, IndexQuery query, String facetSetupDoc, int start);

  /**
   * Using the given Index, calculate the facets as per the specified doc with the given start and pageSize
   * @param index Name of an index to query
   * @param query Query definition containing all information required to query a specified index
   * @param facetSetupDoc Document key that contains predefined FacetSetup
   * @param start Number of results that should be skipped. Default: 0
   * @param pageSize Maximum number of results that will be retrieved. Default: null. If set, overrides Facet.MaxResults
   */
  public FacetResults getFacets(String index, IndexQuery query, String facetSetupDoc, int start, Integer pageSize);

  /**
   * Using the given Index, calculate the facets as per the specified doc with the given start and pageSize
   * @param index Name of an index to query
   * @param query Query definition containing all information required to query a specified index
   * @param facets List of facets required to perform a facet query
   */
  public FacetResults getFacets(final String index, final IndexQuery query, final List<Facet> facets) ;


  /**
   * Using the given Index, calculate the facets as per the specified doc with the given start and pageSize
   * @param index Name of an index to query
   * @param query Query definition containing all information required to query a specified index
   * @param facets List of facets required to perform a facet query
   * @param start Number of results that should be skipped. Default: 0
   */
  public FacetResults getFacets(final String index, final IndexQuery query, final List<Facet> facets, final int start) ;


  /**
   * Using the given Index, calculate the facets as per the specified doc with the given start and pageSize
   * @param index Name of an index to query
   * @param query Query definition containing all information required to query a specified index
   * @param facets List of facets required to perform a facet query
   * @param start Number of results that should be skipped. Default: 0
   * @param pageSize Maximum number of results that will be retrieved. Default: null. If set, overrides Facet.MaxResults
   */
  public FacetResults getFacets(final String index, final IndexQuery query, final List<Facet> facets, final int start, final Integer pageSize) ;

  /**
   * Retrieves documents for the specified key prefix.
   * @param keyPrefix Prefix for which documents should be returned e.g. "products/"
   * @param matches Pipe ('|') separated values for which document keys (after 'keyPrefix') should be matched ('?' any single character, '*' any characters)
   * @param start Number of documents that should be skipped
   * @param pageSize Maximum number of documents that will be retrieved
   */
  public List<JsonDocument> startsWith(String keyPrefix, String matches, int start, int pageSize) throws ServerClientException;


  /**
   * Retrieves documents for the specified key prefix.
   * @param keyPrefix Prefix for which documents should be returned e.g. "products/"
   * @param matches Pipe ('|') separated values for which document keys (after 'keyPrefix') should be matched ('?' any single character, '*' any characters)
   * @param start Number of documents that should be skipped
   * @param pageSize Maximum number of documents that will be retrieved
   * @param metadataOnly Specifies if only document metadata should be returned
   */
  public List<JsonDocument> startsWith(String keyPrefix, String matches, int start, int pageSize, boolean metadataOnly);

  /**
   * Retrieves documents for the specified key prefix.
   * @param keyPrefix Prefix for which documents should be returned e.g. "products/"
   * @param matches Pipe ('|') separated values for which document keys (after 'keyPrefix') should be matched ('?' any single character, '*' any characters)
   * @param start Number of documents that should be skipped
   * @param pageSize Maximum number of documents that will be retrieved
   * @param metadataOnly Specifies if only document metadata should be returned
   * @param exclude Pipe ('|') separated values for which document keys (after 'keyPrefix') should not be matched ('?' any single character, '*' any characters)
   */
  public List<JsonDocument> startsWith(String keyPrefix, String matches, int start, int pageSize, boolean metadataOnly, String exclude);

  /**
   * Retrieves documents for the specified key prefix.
   * @param keyPrefix Prefix for which documents should be returned e.g. "products/"
   * @param matches Pipe ('|') separated values for which document keys (after 'keyPrefix') should be matched ('?' any single character, '*' any characters)
   * @param start Number of documents that should be skipped
   * @param pageSize Maximum number of documents that will be retrieved
   * @param metadataOnly Specifies if only document metadata should be returned
   * @param exclude Pipe ('|') separated values for which document keys (after 'keyPrefix') should not be matched ('?' any single character, '*' any characters)
   * @param pagingInformation Used to perform rapid pagination on a server side
   */
  public List<JsonDocument> startsWith(String keyPrefix, String matches, int start, int pageSize, boolean metadataOnly, String exclude, RavenPagingInformation pagingInformation);

  /**
   * Retrieves documents for the specified key prefix.
   * @param keyPrefix Prefix for which documents should be returned e.g. "products/"
   * @param matches Pipe ('|') separated values for which document keys (after 'keyPrefix') should be matched ('?' any single character, '*' any characters)
   * @param start Number of documents that should be skipped
   * @param pageSize Maximum number of documents that will be retrieved
   * @param metadataOnly Specifies if only document metadata should be returned
   * @param exclude Pipe ('|') separated values for which document keys (after 'keyPrefix') should not be matched ('?' any single character, '*' any characters)
   * @param pagingInformation Used to perform rapid pagination on a server side
   * @param transformer Name of a transformer that should be used to transform the results
   * @param transformerParameters Parameters that will be passed to transformer
   */
  public List<JsonDocument> startsWith(String keyPrefix, String matches, int start, int pageSize, boolean metadataOnly,
    String exclude, RavenPagingInformation pagingInformation, String transformer, Map<String, RavenJToken> transformerParameters);

  /**
   * Retrieves documents for the specified key prefix.
   * @param keyPrefix Prefix for which documents should be returned e.g. "products/"
   * @param matches Pipe ('|') separated values for which document keys (after 'keyPrefix') should be matched ('?' any single character, '*' any characters)
   * @param start Number of documents that should be skipped
   * @param pageSize Maximum number of documents that will be retrieved
   * @param metadataOnly Specifies if only document metadata should be returned
   * @param exclude Pipe ('|') separated values for which document keys (after 'keyPrefix') should not be matched ('?' any single character, '*' any characters)
   * @param pagingInformation Used to perform rapid pagination on a server side
   * @param transformer Name of a transformer that should be used to transform the results
   * @param transformerParameters Parameters that will be passed to transformer
   * @param skipAfter Skip document fetching until given key is found and return documents after that key (default: null)
   */
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
   * @param key The key of a document to be deleted
   * @param etag current document etag, used for concurrency checks (null to skip check)
   */
  public void delete(String key, Etag etag);

  /**
   * Puts a byte array as attachment with the specified key
   * @param key Unique key under which attachment will be stored
   * @param etag Current attachment etag, used for concurrency checks (null to skip check)
   * @param data Attachment data
   * @param metadata Attachment metadata
   * @deprecated Use RavenFS instead.
   */
  @Deprecated
  public void putAttachment(String key, Etag etag, InputStream data, RavenJObject metadata);

  /**
   * Updates attachments metadata only.
   * @param key Key under which attachment is stored
   * @param etag Current attachment etag, used for concurrency checks (null to skip check)
   * @param metadata Attachment metadata
   * @deprecated Use RavenFS instead.
   */
  @Deprecated
  public void updateAttachmentMetadata(String key, Etag etag, RavenJObject metadata);

  /**
   * Downloads attachment metadata for a multiple attachments.
   * @param idPrefix Prefix for which attachments should be returned
   * @param start Number of attachments that should be skipped
   * @param pageSize Maximum number of attachments that will be returned
   * @deprecated Use RavenFS instead.
   */
  @Deprecated
  public List<Attachment> getAttachmentHeadersStartingWith(String idPrefix, int start, int pageSize);

  /**
   *  Queries the specified index in the Raven flavored Lucene query syntax. Will return *all* results, regardless
   *  of the number of items that might be returned.
   * @param index Name of an index to query
   * @param query Query definition containing all information required to query a specified index
   * @param queryHeaderInfo Information about performed query
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
   * @param fromEtag ETag of a document from which stream should start (mutually exclusive with 'startsWith')
   */
  public CloseableIterator<RavenJObject> streamDocs(Etag fromEtag);

  /**
   * Streams the documents by etag OR starts with the prefix and match the matches
   * Will return *all* results, regardless of the number of items that might be returned.
   * @param fromEtag ETag of a document from which stream should start (mutually exclusive with 'startsWith')
   * @param startsWith Prefix for which documents should be streamed (mutually exclusive with 'fromEtag')
   */
  public CloseableIterator<RavenJObject> streamDocs(Etag fromEtag, String startsWith);

  /**
   * Streams the documents by etag OR starts with the prefix and match the matches
   * Will return *all* results, regardless of the number of items that might be returned.
   * @param fromEtag ETag of a document from which stream should start (mutually exclusive with 'startsWith')
   * @param startsWith Prefix for which documents should be streamed (mutually exclusive with 'fromEtag')
   * @param matches Pipe ('|') separated values for which document keys (after 'keyPrefix') should be matched ('?' any single character, '*' any characters)
   */
  public CloseableIterator<RavenJObject> streamDocs(Etag fromEtag, String startsWith, String matches);

  /**
   * Streams the documents by etag OR starts with the prefix and match the matches
   * Will return *all* results, regardless of the number of items that might be returned.
   * @param fromEtag ETag of a document from which stream should start (mutually exclusive with 'startsWith')
   * @param startsWith Prefix for which documents should be streamed (mutually exclusive with 'fromEtag')
   * @param matches Pipe ('|') separated values for which document keys (after 'keyPrefix') should be matched ('?' any single character, '*' any characters)
   * @param start Number of documents that should be skipped
   */
  public CloseableIterator<RavenJObject> streamDocs(Etag fromEtag, String startsWith, String matches, int start);

  /**
   * Streams the documents by etag OR starts with the prefix and match the matches
   * Will return *all* results, regardless of the number of items that might be returned.
   * @param fromEtag ETag of a document from which stream should start (mutually exclusive with 'startsWith')
   * @param startsWith Prefix for which documents should be streamed (mutually exclusive with 'fromEtag')
   * @param matches Pipe ('|') separated values for which document keys (after 'keyPrefix') should be matched ('?' any single character, '*' any characters)
   * @param start Number of documents that should be skipped
   * @param pageSize Maximum number of documents that will be retrieved
   */
  public CloseableIterator<RavenJObject> streamDocs(Etag fromEtag, String startsWith, String matches, int start, int pageSize);

  /**
   * Streams the documents by etag OR starts with the prefix and match the matches
   * Will return *all* results, regardless of the number of items that might be returned.
   * @param fromEtag ETag of a document from which stream should start (mutually exclusive with 'startsWith')
   * @param startsWith Prefix for which documents should be streamed (mutually exclusive with 'fromEtag')
   * @param matches Pipe ('|') separated values for which document keys (after 'keyPrefix') should be matched ('?' any single character, '*' any characters)
   * @param start Number of documents that should be skipped
   * @param pageSize Maximum number of documents that will be retrieved
   * @param exclude Pipe ('|') separated values for which document keys (after 'keyPrefix') should not be matched ('?' any single character, '*' any characters)
   */
  public CloseableIterator<RavenJObject> streamDocs(Etag fromEtag, String startsWith, String matches, int start, int pageSize, String exclude);

  /**
   * Streams the documents by etag OR starts with the prefix and match the matches
   * Will return *all* results, regardless of the number of items that might be returned.
   * @param fromEtag ETag of a document from which stream should start (mutually exclusive with 'startsWith')
   * @param startsWith Prefix for which documents should be streamed (mutually exclusive with 'fromEtag')
   * @param matches Pipe ('|') separated values for which document keys (after 'keyPrefix') should be matched ('?' any single character, '*' any characters)
   * @param start Number of documents that should be skipped
   * @param pageSize Maximum number of documents that will be retrieved
   * @param exclude Pipe ('|') separated values for which document keys (after 'keyPrefix') should not be matched ('?' any single character, '*' any characters)
   * @param pagingInformation Used to perform rapid pagination on a server side
   */
  public CloseableIterator<RavenJObject> streamDocs(Etag fromEtag, String startsWith, String matches, int start, int pageSize, String exclude, RavenPagingInformation pagingInformation);

  /**
   * Streams the documents by etag OR starts with the prefix and match the matches
   * Will return *all* results, regardless of the number of items that might be returned.
   * @param fromEtag ETag of a document from which stream should start (mutually exclusive with 'startsWith')
   * @param startsWith Prefix for which documents should be streamed (mutually exclusive with 'fromEtag')
   * @param matches Pipe ('|') separated values for which document keys (after 'keyPrefix') should be matched ('?' any single character, '*' any characters)
   * @param start Number of documents that should be skipped
   * @param pageSize Maximum number of documents that will be retrieved
   * @param exclude Pipe ('|') separated values for which document keys (after 'keyPrefix') should not be matched ('?' any single character, '*' any characters)
   * @param pagingInformation Used to perform rapid pagination on a server side
   * @param skipAfter Skip document fetching until given key is found and return documents after that key (default: null)
   */
  public CloseableIterator<RavenJObject> streamDocs(Etag fromEtag, String startsWith, String matches, int start, int pageSize, String exclude, RavenPagingInformation pagingInformation, String skipAfter);

  /**
   * Return a list of documents that based on the MoreLikeThisQuery.
   * @param query More like this query definition that will be executed
   */
  public MultiLoadResult moreLikeThis(MoreLikeThisQuery query);

  /**
   * Sends a multiple faceted queries in a single request and calculates the facet results for each of them
   * @param facetedQueries List of the faceted queries that will be executed on the server-side
   */
  public FacetResults[] getMultiFacets(FacetQuery[] facetedQueries);


  /**
   * Retrieves the document metadata for the specified document key.
   *
   * Returns:
   * The document metadata for the specified document, or null if the document does not exist
   * @param key Key of a document to get metadata for
   * @return The document metadata for the specified document, or null if the document does not exist
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

  /**
   * Get the low level bulk insert operation
   * @param options
   * @param changes
   */
  public ILowLevelBulkInsertOperation getBulkInsertOperation(BulkInsertOptions options, IDatabaseChanges changes);

  /**
   * Retrieves all suggestions for an index merging
   */
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

  /**
   * Gets the build number
   * @throws IOException
   */
  public BuildNumber getBuildNumber() throws IOException;

}
