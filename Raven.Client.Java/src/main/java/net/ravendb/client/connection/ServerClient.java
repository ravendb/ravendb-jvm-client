package net.ravendb.client.connection;

import static net.ravendb.client.connection.RavenUrlExtensions.indexes;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import net.ravendb.abstractions.basic.CleanCloseable;
import net.ravendb.abstractions.basic.EventHandler;
import net.ravendb.abstractions.basic.Reference;
import net.ravendb.abstractions.basic.SharpEnum;
import net.ravendb.abstractions.closure.Action2;
import net.ravendb.abstractions.closure.Action3;
import net.ravendb.abstractions.closure.Function0;
import net.ravendb.abstractions.closure.Function1;
import net.ravendb.abstractions.closure.Function3;
import net.ravendb.abstractions.commands.ICommandData;
import net.ravendb.abstractions.commands.PatchCommandData;
import net.ravendb.abstractions.commands.ScriptedPatchCommandData;
import net.ravendb.abstractions.connection.ErrorResponseException;
import net.ravendb.abstractions.connection.OperationCredentials;
import net.ravendb.abstractions.data.Attachment;
import net.ravendb.abstractions.data.AttachmentInformation;
import net.ravendb.abstractions.data.BatchResult;
import net.ravendb.abstractions.data.BuildNumber;
import net.ravendb.abstractions.data.BulkInsertOptions;
import net.ravendb.abstractions.data.BulkOperationOptions;
import net.ravendb.abstractions.data.Constants;
import net.ravendb.abstractions.data.DatabaseStatistics;
import net.ravendb.abstractions.data.Etag;
import net.ravendb.abstractions.data.Facet;
import net.ravendb.abstractions.data.FacetQuery;
import net.ravendb.abstractions.data.FacetResults;
import net.ravendb.abstractions.data.GetRequest;
import net.ravendb.abstractions.data.GetResponse;
import net.ravendb.abstractions.data.HttpMethods;
import net.ravendb.abstractions.data.IndexQuery;
import net.ravendb.abstractions.data.IndexStats.IndexingPriority;
import net.ravendb.abstractions.data.JsonDocument;
import net.ravendb.abstractions.data.JsonDocumentMetadata;
import net.ravendb.abstractions.data.LicensingStatus;
import net.ravendb.abstractions.data.LogItem;
import net.ravendb.abstractions.data.MoreLikeThisQuery;
import net.ravendb.abstractions.data.MultiLoadResult;
import net.ravendb.abstractions.data.PatchRequest;
import net.ravendb.abstractions.data.PatchResult;
import net.ravendb.abstractions.data.PutResult;
import net.ravendb.abstractions.data.QueryHeaderInformation;
import net.ravendb.abstractions.data.QueryResult;
import net.ravendb.abstractions.data.ScriptedPatchRequest;
import net.ravendb.abstractions.data.SuggestionQuery;
import net.ravendb.abstractions.data.SuggestionQueryResult;
import net.ravendb.abstractions.exceptions.BadRequestException;
import net.ravendb.abstractions.exceptions.ConcurrencyException;
import net.ravendb.abstractions.exceptions.DocumentDoesNotExistsException;
import net.ravendb.abstractions.exceptions.IndexCompilationException;
import net.ravendb.abstractions.exceptions.TransformCompilationException;
import net.ravendb.abstractions.extensions.ExceptionExtensions;
import net.ravendb.abstractions.extensions.MetadataExtensions;
import net.ravendb.abstractions.indexing.IndexDefinition;
import net.ravendb.abstractions.indexing.IndexLockMode;
import net.ravendb.abstractions.indexing.IndexMergeResults;
import net.ravendb.abstractions.indexing.NumberUtil;
import net.ravendb.abstractions.indexing.TransformerDefinition;
import net.ravendb.abstractions.json.linq.JTokenType;
import net.ravendb.abstractions.json.linq.RavenJArray;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.json.linq.RavenJToken;
import net.ravendb.abstractions.json.linq.RavenJValue;
import net.ravendb.abstractions.replication.ReplicationDocument;
import net.ravendb.abstractions.replication.ReplicationStatistics;
import net.ravendb.abstractions.util.BomUtils;
import net.ravendb.abstractions.util.NetDateFormat;
import net.ravendb.client.RavenPagingInformation;
import net.ravendb.client.changes.IDatabaseChanges;
import net.ravendb.client.connection.ReplicationInformer.FailoverStatusChangedEventArgs;
import net.ravendb.client.connection.implementation.HttpJsonRequest;
import net.ravendb.client.connection.implementation.HttpJsonRequestFactory;
import net.ravendb.client.connection.profiling.ProfilingInformation;
import net.ravendb.client.document.DocumentConvention;
import net.ravendb.client.document.ILowLevelBulkInsertOperation;
import net.ravendb.client.document.JsonSerializer;
import net.ravendb.client.document.RemoteBulkInsertOperation;
import net.ravendb.client.exceptions.ConflictException;
import net.ravendb.client.exceptions.ServerRequestError;
import net.ravendb.client.extensions.HttpJsonRequestExtension;
import net.ravendb.client.extensions.MultiDatabase;
import net.ravendb.client.indexes.IndexDefinitionBuilder;
import net.ravendb.client.listeners.IDocumentConflictListener;
import net.ravendb.client.utils.UrlUtils;
import net.ravendb.imports.json.JsonConvert;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.codehaus.jackson.JsonParser;


@SuppressWarnings("deprecation")
public class ServerClient implements IDatabaseCommands {

  private final ProfilingInformation profilingInformation;
  private final IDocumentConflictListener[] conflictListeners;
  protected String url;
  private String rootUrl;
  private OperationCredentials credentialsThatShouldBeUsedOnlyInOperationsWithoutReplication;
  final DocumentConvention convention;
  protected Map<String, String> operationsHeaders = new HashMap<>();
  protected final HttpJsonRequestFactory jsonRequestFactory;
  private final UUID sessionId;
  private final Function1<String, IDocumentStoreReplicationInformer> replicationInformerGetter;
  @SuppressWarnings("unused")
  private final String databaseName;
  private final IDocumentStoreReplicationInformer replicationInformer;
  protected int requestCount;
  protected int readStripingBase;

  private boolean resolvingConflict;
  private boolean resolvingConflictRetries;

  private boolean expect100Continue = false;

  /**
   * @return the url
   */
  public String getUrl() {
    return url;
  }

  /**
   * @return the replicationInformer
   */
  public IDocumentStoreReplicationInformer getReplicationInformer() {
    return replicationInformer;
  }

  @Override
  public OperationCredentials getPrimaryCredentials() {
    return credentialsThatShouldBeUsedOnlyInOperationsWithoutReplication;
  }

  public ServerClient(String url, DocumentConvention convention, OperationCredentials credentials,
    HttpJsonRequestFactory httpJsonRequestFactory, UUID sessionId,
    Function1<String, IDocumentStoreReplicationInformer> replicationInformerGetter,  String databaseName,
    IDocumentConflictListener[] conflictListeners, boolean incrementReadStripe) {
    this.profilingInformation = ProfilingInformation.createProfilingInformation(sessionId);
    this.url = url;
    if (this.url.endsWith("/")) {
      this.url = this.url.substring(0, this.url.length() - 1);
    }
    rootUrl = this.url;
    int databasesIndex = rootUrl.indexOf("/databases/");
    if (databasesIndex > 0) {
      rootUrl = rootUrl.substring(0, databasesIndex);
    }
    this.jsonRequestFactory = httpJsonRequestFactory;
    this.sessionId = sessionId;
    this.convention = convention;
    this.credentialsThatShouldBeUsedOnlyInOperationsWithoutReplication = credentials;
    this.databaseName = databaseName;
    this.conflictListeners = conflictListeners;
    this.replicationInformerGetter = replicationInformerGetter;
    this.replicationInformer = replicationInformerGetter.apply(databaseName);
    this.readStripingBase = replicationInformer.getReadStripingBase(incrementReadStripe);
    replicationInformer.updateReplicationInformationIfNeeded(this);
  }

  @Override
  public Collection<String> getIndexNames(final int start, final int pageSize) {
    return executeWithReplication(HttpMethods.GET, new Function1<OperationMetadata, Collection<String>>() {
      @Override
      public Collection<String> apply(OperationMetadata operationMetadata) {
        return directGetIndexNames(start, pageSize, operationMetadata);
      }
    });
  }

  protected Collection<String> directGetIndexNames(int start, int pageSize, OperationMetadata operationMetadata) {
    try (HttpJsonRequest request = jsonRequestFactory.createHttpJsonRequest(
      new CreateHttpJsonRequestParams(this, RavenUrlExtensions.indexNames(operationMetadata.getUrl(), start, pageSize),
        HttpMethods.GET, null,  operationMetadata.getCredentials(), convention))) {
      RavenJArray json = (RavenJArray)request
        .addReplicationStatusHeaders(url, operationMetadata.getUrl(), replicationInformer, convention.getFailoverBehavior(), new HandleReplicationStatusChangesCallback())
        .readResponseJson();

      return json.values(String.class);
    }
  }

  @Override
  public Collection<IndexDefinition> getIndexes(final int start, final int pageSize) {
    return executeWithReplication(HttpMethods.GET, new Function1<OperationMetadata, Collection<IndexDefinition>>() {
      @Override
      public Collection<IndexDefinition> apply(OperationMetadata operationMetadata) {
        return directGetIndexes(start, pageSize, operationMetadata);
      }
    });
  }

  protected Collection<IndexDefinition> directGetIndexes(int start, int pageSize, OperationMetadata operationMetadata) {
    String operationUrl = operationMetadata.getUrl() + "/indexes/?start=" + start + "&pageSize=" + pageSize;
    try (HttpJsonRequest request = jsonRequestFactory.createHttpJsonRequest(new CreateHttpJsonRequestParams(this, operationUrl, HttpMethods.GET, new RavenJObject(), operationMetadata.getCredentials(), convention))) {
      request.addReplicationStatusHeaders(url, operationMetadata.getUrl(), replicationInformer, convention.getFailoverBehavior(), new HandleReplicationStatusChangesCallback());

      RavenJArray json = (RavenJArray) request.readResponseJson();
      return JsonConvert.deserializeObject(json, IndexDefinition.class, "definition");
    }
  }

  @Override
  public List<TransformerDefinition> getTransformers(final int start, final int pageSize) {
    return executeWithReplication(HttpMethods.GET, new Function1<OperationMetadata, List<TransformerDefinition>>() {
      @Override
      public List<TransformerDefinition> apply(OperationMetadata operationMetadata) {
        return directGetTransformers(operationMetadata, start, pageSize);
      }
    });
  }

  protected List<TransformerDefinition> directGetTransformers(OperationMetadata operationMetadata, int start, int pageSize) {
    String operationUrl = operationMetadata.getUrl() + "/transformers?start=" + start + "&pageSize=" + pageSize;
    try (HttpJsonRequest request = jsonRequestFactory.createHttpJsonRequest(new CreateHttpJsonRequestParams(this, operationUrl, HttpMethods.GET, new RavenJObject(), operationMetadata.getCredentials(), convention))) {
      request.addReplicationStatusHeaders(url, operationMetadata.getUrl(), replicationInformer, convention.getFailoverBehavior(), new HandleReplicationStatusChangesCallback());

      RavenJToken result = request.readResponseJson();
      RavenJArray json = ((RavenJArray)result);
      return JsonConvert.deserializeObject(json, TransformerDefinition.class, "definition");
    }
  }

  @Override
  public void resetIndex(final String name) {
    executeWithReplication(HttpMethods.RESET, new Function1<OperationMetadata, Void>() {
      @Override
      public Void apply(OperationMetadata operationMetadata) {
        directResetIndex(name, operationMetadata);
        return null;
      }
    });
  }

  protected void directResetIndex(String name, OperationMetadata operationMetadata) {
    try (HttpJsonRequest httpJsonRequest = jsonRequestFactory.createHttpJsonRequest(
      new CreateHttpJsonRequestParams(this, operationMetadata.getUrl() + "/indexes/" + name, HttpMethods.RESET, new RavenJObject(), operationMetadata.getCredentials(), convention))) {
      httpJsonRequest.addOperationHeaders(operationsHeaders);
      httpJsonRequest.addReplicationStatusHeaders(url, operationMetadata.getUrl(), replicationInformer, convention.getFailoverBehavior(), new HandleReplicationStatusChangesCallback());

      httpJsonRequest.readResponseJson();
    }
  }

  @Override
  public void setIndexLock(final String name, final IndexLockMode lockMode) {
    executeWithReplication(HttpMethods.POST, new Function1<OperationMetadata, Void>() {
      @Override
      public Void apply(OperationMetadata operationMetadata) {
        directSetIndexLock(name, lockMode, operationMetadata);
        return null;
      }
    });
  }
  protected void directSetIndexLock(String name, IndexLockMode lockMode, OperationMetadata operationMetadata) {
    try (HttpJsonRequest httpJsonRequest = jsonRequestFactory.createHttpJsonRequest(
      new CreateHttpJsonRequestParams(this, operationMetadata.getUrl() + "/indexes/" + name + "?op=lockModeChange&mode=" + SharpEnum.value(lockMode), HttpMethods.POST, new RavenJObject(), operationMetadata.getCredentials(), convention))) {
      httpJsonRequest.addOperationHeaders(operationsHeaders);
      httpJsonRequest.addReplicationStatusHeaders(url, operationMetadata.getUrl(), replicationInformer, convention.getFailoverBehavior(), new HandleReplicationStatusChangesCallback());
      httpJsonRequest.readResponseJson();
    }
  }

  @Override
  public void setIndexPriority(final String name, final IndexingPriority priority){
    executeWithReplication(HttpMethods.POST, new Function1<OperationMetadata, Void>() {
      @Override
      public Void apply(OperationMetadata operationMetadata) {
        directSetIndexPriority(name, priority, operationMetadata);
        return null;
      }
    });
  }

  protected void directSetIndexPriority(String name, IndexingPriority priority, OperationMetadata operationMetadata) {
    try (HttpJsonRequest httpJsonRequest = jsonRequestFactory.createHttpJsonRequest(
      new CreateHttpJsonRequestParams(this, operationMetadata.getUrl() + "/indexes/set-priority/" + name + "?priority=" + SharpEnum.value(priority), HttpMethods.POST, new RavenJObject(), operationMetadata.getCredentials(), convention))) {
      httpJsonRequest.addOperationHeaders(operationsHeaders);
      httpJsonRequest.addReplicationStatusHeaders(url, operationMetadata.getUrl(), replicationInformer, convention.getFailoverBehavior(), new HandleReplicationStatusChangesCallback());
      httpJsonRequest.readResponseJson();
    }
  }

  @Override
  public String putIndex(String name, IndexDefinitionBuilder indexDef) {
    return putIndex(name, indexDef.toIndexDefinition(convention));
  }

  @Override
  public String putIndex(String name, IndexDefinitionBuilder indexDef, boolean overwrite) {
    return putIndex(name, indexDef.toIndexDefinition(convention), overwrite);
  }

  @SuppressWarnings("boxing")
  @Override
  public boolean indexHasChanged(final String name, final IndexDefinition definition) {
    return executeWithReplication(HttpMethods.POST, new Function1<OperationMetadata, Boolean>() {
      @Override
      public Boolean apply(OperationMetadata input) {
        return directIndexHasChanged(name, definition, input);
      }
    });
  }

  protected Boolean directIndexHasChanged(String name, IndexDefinition definition, OperationMetadata operationMetadata) {
    String requestUri = RavenUrlExtensions.indexes(operationMetadata.getUrl(), name) + "?op=hasChanged";
    try (HttpJsonRequest webRequest = jsonRequestFactory.createHttpJsonRequest(
      new CreateHttpJsonRequestParams(this, requestUri, HttpMethods.POST, new RavenJObject(), operationMetadata.getCredentials(), convention))) {
      webRequest.addOperationHeaders(operationsHeaders);
      webRequest.addReplicationStatusHeaders(url, operationMetadata.getUrl(), replicationInformer, convention.getFailoverBehavior(), new HandleReplicationStatusChangesCallback());

      webRequest.write(JsonConvert.serializeObject(definition)); //we don't use default converters
      RavenJToken responseJson = webRequest.readResponseJson();
      return responseJson.value(Boolean.class, "Changed");
    }
  }

  @Override
  public String putIndex(final String name, final IndexDefinition definition) {
    return putIndex(name, definition, false);
  }

  @Override
  public String putIndex(final String name, final IndexDefinition definition, final boolean overwrite) {
    ensureIsNotNullOrEmpty(name, "name");
    return executeWithReplication(HttpMethods.PUT, new Function1<OperationMetadata, String>() {
      @Override
      public String apply(OperationMetadata operationMetadata) {
        return directPutIndex(name, definition, overwrite, operationMetadata);
      }
    });
  }

  @Override
  public String putTransformer(final String name, final TransformerDefinition indexDef) {
    ensureIsNotNullOrEmpty(name, "name");
    return executeWithReplication(HttpMethods.PUT, new Function1<OperationMetadata, String>() {
      @Override
      public String apply(OperationMetadata operationMetadata) {
        return directPutTransformer(name, operationMetadata, indexDef);
      }
    });
  }

  public String directPutIndex(String name, IndexDefinition definition, boolean overwrite, OperationMetadata operationMetadata) {
    String requestUri = operationMetadata.getUrl() + "/indexes/" + UrlUtils.escapeUriString(name) + "?definition=yes";

    try (HttpJsonRequest webRequest = jsonRequestFactory.createHttpJsonRequest(
      new CreateHttpJsonRequestParams(this, requestUri, HttpMethods.HEAD, new RavenJObject(), operationMetadata.getCredentials(), convention)
      .addOperationHeaders(operationsHeaders))
      .addReplicationStatusHeaders(url, operationMetadata.getUrl(), replicationInformer, convention.getFailoverBehavior(), new HandleReplicationStatusChangesCallback())) {

      try {
        // If the index doesn't exist this will throw a NotFound exception and continue with a PUT request
        webRequest.executeRequest();
        if (!overwrite) {
          throw new IllegalStateException("Cannot put index: " + name + ", index already exists");
        }
      } catch (ErrorResponseException e) {
        if (e.getStatusCode() != HttpStatus.SC_NOT_FOUND) {
          throw e;
        }
      }
    }
    try (HttpJsonRequest request = jsonRequestFactory.createHttpJsonRequest(
      new CreateHttpJsonRequestParams(this, requestUri, HttpMethods.PUT, new RavenJObject(), operationMetadata.getCredentials(), convention)
      .addOperationHeaders(operationsHeaders))) {

      ErrorResponseException responseException;
      try {
        request.write(JsonConvert.serializeObject(definition)); //we don't use default converters
        RavenJToken responseJson = request.readResponseJson();
        return responseJson.value(String.class, "Index");
      } catch (ErrorResponseException e) {
        if (e.getStatusCode() != HttpStatus.SC_BAD_REQUEST) {
          throw e;
        }
        responseException = e;
      }

      IndexErrorObjectProto error = ExceptionExtensions.tryReadErrorResponseObject(IndexErrorObjectProto.class, responseException);
      if (error == null) {
        throw responseException;
      }
      IndexCompilationException compilationException = new IndexCompilationException(error.getMessage());
      compilationException.setIndexDefinitionProperty(error.getIndexDefinitionProperty());
      compilationException.setProblematicText(error.getProblematicText());
      throw compilationException;
    }
  }

  public static class IndexErrorObjectProto {
    private String error;
    private String message;
    private String indexDefinitionProperty;
    private String problematicText;

    public String getError() {
      return error;
    }
    public void setError(String error) {
      this.error = error;
    }
    public String getMessage() {
      return message;
    }
    public void setMessage(String message) {
      this.message = message;
    }
    public String getIndexDefinitionProperty() {
      return indexDefinitionProperty;
    }
    public void setIndexDefinitionProperty(String indexDefinitionProperty) {
      this.indexDefinitionProperty = indexDefinitionProperty;
    }
    public String getProblematicText() {
      return problematicText;
    }
    public void setProblematicText(String problematicText) {
      this.problematicText = problematicText;
    }
  }

  public String directPutTransformer(String name, OperationMetadata operationMetadata, TransformerDefinition definition) {
    String requestUri = operationMetadata.getUrl() + "/transformers/" + name;

    try(HttpJsonRequest request = jsonRequestFactory.createHttpJsonRequest(
      new CreateHttpJsonRequestParams(this, requestUri, HttpMethods.PUT, new RavenJObject(), operationMetadata.getCredentials(), convention)
      .addOperationHeaders(operationsHeaders))) {

      ErrorResponseException responseException;
      try {
        request.write(JsonConvert.serializeObject(definition));
        RavenJObject responseJson = (RavenJObject) request.readResponseJson();
        return responseJson.value(String.class, "Transformer");
      } catch (BadRequestException e) {
        throw new TransformCompilationException(e);
      } catch (ErrorResponseException e) {
        if (e.getStatusCode() != HttpStatus.SC_BAD_REQUEST) {
          throw e;
        }
        responseException = e;
      }

      ErrorObjectProtoTransformer error = ExceptionExtensions.tryReadErrorResponseObject(ErrorObjectProtoTransformer.class, responseException);
      if (error == null) {
        throw responseException;
      }
      throw new TransformCompilationException(error.getMessage());
    }
  }

  public static class ErrorObjectProtoTransformer {
    private String error;
    private String message;

    public String getError() {
      return error;
    }
    public void setError(String error) {
      this.error = error;
    }
    public String getMessage() {
      return message;
    }
    public void setMessage(String message) {
      this.message = message;
    }
  }

  @Override
  public void deleteIndex(final String name) {
    ensureIsNotNullOrEmpty(name, "name");
    executeWithReplication(HttpMethods.DELETE, new Function1<OperationMetadata, Void>() {
      @Override
      public Void apply(OperationMetadata operationMetadata) {
        directDeleteIndex(name, operationMetadata);
        return null;
      }
    });
  }

  protected void directDeleteIndex(String name, OperationMetadata operationMetadata) {
    try (HttpJsonRequest request = jsonRequestFactory.createHttpJsonRequest(
      new CreateHttpJsonRequestParams(
        this, indexes(operationMetadata.getUrl(), name), HttpMethods.DELETE, null, operationMetadata.getCredentials(), convention))) {
      request.addOperationHeaders(operationsHeaders);
      request.addReplicationStatusHeaders(url, operationMetadata.getUrl(), replicationInformer, convention.getFailoverBehavior(), new HandleReplicationStatusChangesCallback());
      request.executeRequest();
    }
  }

  @Override
  public Operation deleteByIndex(String indexName, IndexQuery queryToDelete) {
    return deleteByIndex(indexName, queryToDelete, null);
  }

  @Override
  public Operation deleteByIndex(final String indexName, final IndexQuery queryToDelete, final BulkOperationOptions options) {
    return executeWithReplication(HttpMethods.DELETE, new Function1<OperationMetadata, Operation>() {
      @Override
      public Operation apply(OperationMetadata operationMetadata) {
        return directDeleteByIndex(operationMetadata, indexName, queryToDelete, options);
      }
    });
  }

  @SuppressWarnings("boxing")
  protected Operation directDeleteByIndex(OperationMetadata operationMetadata, String indexName, IndexQuery queryToDelete, BulkOperationOptions options) {
    BulkOperationOptions notNullOptions = (options != null) ? options : new BulkOperationOptions();
    String path = queryToDelete.getIndexQueryUrl(operationMetadata.getUrl(), indexName, "bulk_docs") + "&allowStale=" + notNullOptions.isAllowStale()
      + "&details=" + notNullOptions.isRetrieveDetails();
    if (notNullOptions.getMaxOpsPerSec() != null) {
      path += "&maxOpsPerSec=" + notNullOptions.getMaxOpsPerSec();
    }
    if (notNullOptions.getStaleTimeout() != null) {
      path += "&staleTimeout=" + notNullOptions.getStaleTimeout();
    }
    try (HttpJsonRequest request = jsonRequestFactory.createHttpJsonRequest(
      new CreateHttpJsonRequestParams(this, path, HttpMethods.DELETE, new RavenJObject(), operationMetadata.getCredentials(), convention)
      .addOperationHeaders(operationsHeaders))
      .addReplicationStatusHeaders(url, operationMetadata.getUrl(), replicationInformer, convention.getFailoverBehavior(), new HandleReplicationStatusChangesCallback())) {
      RavenJToken jsonResponse;
      try {
        jsonResponse = request.readResponseJson();
      } catch (ErrorResponseException e) {
        if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
          throw new IllegalStateException("There is no index named: " + indexName);
        }
        throw e;
      }

      if (jsonResponse == null || jsonResponse.getType() != JTokenType.OBJECT) {
        return null;
      }
      RavenJToken opId = ((RavenJObject)jsonResponse).get("OperationId");
      if (opId == null || opId.getType() != JTokenType.INTEGER) {
        return null;
      }
      return new Operation(this, opId.value(Long.TYPE));
    }
  }

  @Override
  public void deleteTransformer(final String name) {
    ensureIsNotNullOrEmpty(name, "name");
    executeWithReplication(HttpMethods.DELETE, new Function1<OperationMetadata, Void>() {
      @Override
      public Void apply(OperationMetadata operationMetadata) {
        directDeleteTransformer(name, operationMetadata);
        return null;
      }
    });
  }

  protected void directDeleteTransformer(final String name, OperationMetadata operationMetadata) {
    try (HttpJsonRequest request = jsonRequestFactory.createHttpJsonRequest(
      new CreateHttpJsonRequestParams(this, operationMetadata.getUrl() + "/transformers/" + name, HttpMethods.DELETE, new RavenJObject(), operationMetadata.getCredentials(), convention)
      .addOperationHeaders(operationsHeaders))) {
      request.addReplicationStatusHeaders(url, operationMetadata.getUrl(), replicationInformer, convention.getFailoverBehavior(), new HandleReplicationStatusChangesCallback());
      request.executeRequest();
    }
  }

  @Override
  public RavenJObject patch(String key, PatchRequest[] patches) {
    return patch(key, patches, null);
  }

  @Override
  public RavenJObject patch(String key, PatchRequest[] patches, boolean ignoreMissing) {
    PatchCommandData command = new PatchCommandData();
    command.setKey(key);
    command.setPatches(patches);

    BatchResult[] batchResults = batch(Arrays.<ICommandData> asList(command));
    if (!ignoreMissing && batchResults[0].getPatchResult() != null && batchResults[0].getPatchResult() == PatchResult.DOCUMENT_DOES_NOT_EXISTS) {
      throw new DocumentDoesNotExistsException("Document with key " + key + " does not exist.");
    }
    return batchResults[0].getAdditionalData();
  }

  @Override
  public RavenJObject patch(String key, ScriptedPatchRequest patch) {
    return patch(key, patch, null);
  }

  @Override
  public RavenJObject patch(String key, ScriptedPatchRequest patch, boolean ignoreMissing) {
    ScriptedPatchCommandData command = new ScriptedPatchCommandData();
    command.setKey(key);
    command.setPatch(patch);

    BatchResult[] batchResults = batch(Arrays.<ICommandData> asList(command));
    if (!ignoreMissing && batchResults[0].getPatchResult() != null && batchResults[0].getPatchResult() == PatchResult.DOCUMENT_DOES_NOT_EXISTS) {
      throw new DocumentDoesNotExistsException("Document with key " + key + " does not exist.");
    }
    return batchResults[0].getAdditionalData();

  }

  @Override
  public RavenJObject patch(String key, PatchRequest[] patches, Etag etag) {
    PatchCommandData command = new PatchCommandData();
    command.setKey(key);
    command.setPatches(patches);
    command.setEtag(etag);

    BatchResult[] batchResults = batch(Arrays.<ICommandData> asList(command));
    return batchResults[0].getAdditionalData();
  }

  @Override
  public RavenJObject patch(String key, PatchRequest[] patchesToExisting, PatchRequest[] patchesToDefault, RavenJObject defaultMetadata) {
    PatchCommandData command = new PatchCommandData();
    command.setKey(key);
    command.setPatches(patchesToExisting);
    command.setPatchesIfMissing(patchesToDefault);
    command.setMetadata(defaultMetadata);

    BatchResult[] batchResults = batch(Arrays.<ICommandData> asList(command));
    return batchResults[0].getAdditionalData();
  }

  @Override
  public RavenJObject patch(String key, ScriptedPatchRequest patch, Etag etag) {
    ScriptedPatchCommandData command = new ScriptedPatchCommandData();
    command.setKey(key);
    command.setPatch(patch);
    command.setEtag(etag);

    BatchResult[] batchResults = batch(Arrays.<ICommandData> asList(command));
    return batchResults[0].getAdditionalData();
  }

  @Override
  public RavenJObject patch(String key, ScriptedPatchRequest patchExisting, ScriptedPatchRequest patchDefault, RavenJObject defaultMetadata) {
    ScriptedPatchCommandData command = new ScriptedPatchCommandData();
    command.setKey(key);
    command.setPatch(patchExisting);
    command.setPatchIfMissing(patchDefault);
    command.setMetadata(defaultMetadata);

    BatchResult[] batchResults = batch(Arrays.<ICommandData>asList(command));
    return batchResults[0].getAdditionalData();
  }

  @Override
  public PutResult put(final String key, final Etag etag, final RavenJObject document, final RavenJObject metadata) {
    return executeWithReplication(HttpMethods.PUT, new Function1<OperationMetadata, PutResult>() {
      @Override
      public PutResult apply(OperationMetadata operationMetadata) {
        return directPut(metadata, key, etag, document, operationMetadata);
      }
    });
  }

  protected PutResult directPut(RavenJObject metadata, String key, Etag etag, RavenJObject document, OperationMetadata operationMetadata) {
    if (metadata == null) {
      metadata = new RavenJObject();
    }
    HttpMethods method = StringUtils.isNotEmpty(key) ? HttpMethods.PUT : HttpMethods.POST;
    if (etag != null) {
      metadata.set(Constants.METADATA_ETAG_FIELD, new RavenJValue(etag.toString()));
    }
    if (key != null) {
      key = UrlUtils.escapeUriString(key);
    }

    String requestUrl = operationMetadata.getUrl() + "/docs/" + ((key != null) ? key : "");

    try (HttpJsonRequest jsonRequest = jsonRequestFactory.createHttpJsonRequest(
      new CreateHttpJsonRequestParams(this, requestUrl, method, metadata, operationMetadata.getCredentials(), convention)
      .addOperationHeaders(operationsHeaders))
      .addReplicationStatusHeaders(url, operationMetadata.getUrl(), replicationInformer, convention.getFailoverBehavior(), new HandleReplicationStatusChangesCallback())) {

      ErrorResponseException responseException;
      try {
        jsonRequest.write(document.toString());
        RavenJObject responseJson = (RavenJObject) jsonRequest.readResponseJson();

        if (responseJson == null) {
          throw new IllegalStateException("Got null response from the server after doing a put on " + key + ", something is very wrong. Probably a garbled response.");
        }

        return new PutResult(responseJson.value(String.class, "Key"), responseJson.value(Etag.class, "ETag"));
      } catch (ErrorResponseException e) {
        if (e.getStatusCode() != HttpStatus.SC_CONFLICT) {
          throw e;
        }
        responseException = e;
      }
      throw fetchConcurrencyException(responseException);
    }
  }

  @Override
  public IDatabaseCommands forDatabase(String database) {
    if (Constants.SYSTEM_DATABASE.equals(database)) {
      return forSystemDatabase();
    }

    String databaseUrl = MultiDatabase.getRootDatabaseUrl(url);
    databaseUrl = databaseUrl + "/databases/" + database;
    if (databaseUrl.equals(url)) {
      return this;
    }
    ServerClient client = new ServerClient(databaseUrl, convention, credentialsThatShouldBeUsedOnlyInOperationsWithoutReplication,
      jsonRequestFactory, sessionId, replicationInformerGetter, database, conflictListeners, false);
    client.setOperationsHeaders(operationsHeaders);
    return client;
  }

  @Override
  public IDatabaseCommands forSystemDatabase() {
    String databaseUrl = MultiDatabase.getRootDatabaseUrl(url);
    if (databaseUrl.equals(url)) {
      return this;
    }
    ServerClient client = new ServerClient(databaseUrl, convention, credentialsThatShouldBeUsedOnlyInOperationsWithoutReplication,
      jsonRequestFactory, sessionId, replicationInformerGetter, null, conflictListeners, false);
    client.setOperationsHeaders(operationsHeaders);
    return client;
  }

  @Override
  public Map<String, String> getOperationsHeaders() {
    return operationsHeaders;
  }

  @Override
  public void setOperationsHeaders(Map<String, String> operationsHeaders) {
    this.operationsHeaders = operationsHeaders;
  }

  @Override
  public IGlobalAdminDatabaseCommands getGlobalAdmin() {
    return new AdminServerClient(this);
  }

  @Override
  public IAdminDatabaseCommands getAdmin() {
    return new AdminServerClient(this);
  }

  @Override
  public JsonDocument get(final String key) {
    ensureIsNotNullOrEmpty(key, "key");
    return executeWithReplication(HttpMethods.GET, new Function1<OperationMetadata, JsonDocument>() {
      @Override
      public JsonDocument apply(OperationMetadata operationMetadata) {
        return directGet(operationMetadata, key);
      }
    });
  }

  @Override
  public TransformerDefinition getTransformer(final String name) {
    ensureIsNotNullOrEmpty(name, "name");
    return executeWithReplication(HttpMethods.GET, new Function1<OperationMetadata, TransformerDefinition>() {
      @Override
      public TransformerDefinition apply(OperationMetadata operationMetadata) {
        return directGetTransformer(name, operationMetadata);
      }
    });
  }

  protected TransformerDefinition directGetTransformer(final String transformerName, final OperationMetadata operationMetadata) {
    try {
      try (HttpJsonRequest httpJsonRequest = jsonRequestFactory.createHttpJsonRequest(
        new CreateHttpJsonRequestParams(this, operationMetadata.getUrl() + "/transformers/" + transformerName, HttpMethods.GET, new RavenJObject(), operationMetadata.getCredentials(), convention)
        .addOperationHeaders(operationsHeaders))
        .addReplicationStatusHeaders(url, operationMetadata.getUrl(), replicationInformer, convention.getFailoverBehavior(), new HandleReplicationStatusChangesCallback())) {
        RavenJToken transformerDef = httpJsonRequest.readResponseJson();
        RavenJObject value = transformerDef.value(RavenJObject.class, "Transformer");
        return convention.createSerializer().deserialize(value.toString(), TransformerDefinition.class);
      }
    } catch (ErrorResponseException we) {
      if (we.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
        return null;
      }
      throw we;
    }
  }

  @Override
  public IndexDefinition getIndex(final String name) {
    ensureIsNotNullOrEmpty(name, "name");
    return executeWithReplication(HttpMethods.GET, new Function1<OperationMetadata, IndexDefinition>() {
      @Override
      public IndexDefinition apply(OperationMetadata operationMetadata) {
        return directGetIndex(name, operationMetadata);
      }
    });
  }

  protected IndexDefinition directGetIndex(String indexName, OperationMetadata operationMetadata) {
    try {
      try (HttpJsonRequest httpJsonRequest = jsonRequestFactory.createHttpJsonRequest(
        new CreateHttpJsonRequestParams(this, operationMetadata.getUrl() + "/indexes/" + indexName + "?definition=yes", HttpMethods.GET, new RavenJObject(), operationMetadata.getCredentials(), convention)
        .addOperationHeaders(operationsHeaders))
        .addReplicationStatusHeaders(url, operationMetadata.getUrl(), replicationInformer, convention.getFailoverBehavior(), new HandleReplicationStatusChangesCallback())) {
        RavenJToken indexDef = httpJsonRequest.readResponseJson();
        RavenJObject value = indexDef.value(RavenJObject.class, "Index");
        return convention.createSerializer().deserialize(value, IndexDefinition.class);
      }
    } catch (ErrorResponseException we) {
      if (we.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
        return null;
      }
      throw we;
    }
  }

  public JsonDocument directGet(OperationMetadata operationMetadata, String key) {
    if (key.length() > 127) {
      MultiLoadResult multiLoadResult = directGet(new String[] { key}, operationMetadata, new String[0], null, new HashMap<String, RavenJToken>(), false);
      List<RavenJObject> results = multiLoadResult.getResults();
      if (results.get(0) == null) {
        return null;
      }
      return SerializationHelper.ravenJObjectToJsonDocument(results.get(0));
    }

    RavenJObject metadata = new RavenJObject();
    String actualUrl = operationMetadata.getUrl() + "/docs?id=" + UrlUtils.escapeDataString(key);
    CreateHttpJsonRequestParams createHttpJsonRequestParams = new CreateHttpJsonRequestParams(this, actualUrl, HttpMethods.GET, metadata, operationMetadata.getCredentials(), convention);

    try (HttpJsonRequest request = jsonRequestFactory.createHttpJsonRequest(createHttpJsonRequestParams)
      .addOperationHeaders(operationsHeaders)
      .addReplicationStatusHeaders(url, operationMetadata.getUrl(), replicationInformer, convention.getFailoverBehavior(), new HandleReplicationStatusChangesCallback())) {

      try {
        RavenJToken responseJson = request.readResponseJson();
        String docKey = request.getResponseHeaders().get(Constants.DOCUMENT_ID_FIELD_NAME);
        if (docKey == null) {
          docKey = key;
        }
        docKey = UrlUtils.unescapeDataString(docKey);
        request.getResponseHeaders().remove(Constants.DOCUMENT_ID_FIELD_NAME);
        return SerializationHelper.deserializeJsonDocument(docKey, responseJson, request.getResponseHeaders(), request.getResponseStatusCode());
      } catch (ErrorResponseException e) {
        if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
          return null;
        } else if (e.getStatusCode() == HttpStatus.SC_CONFLICT) {
          return resolveConflict(e.getResponseString(), e.getEtag(), operationMetadata, key);
        }
        throw e;
      }
    }
  }

  private JsonDocument resolveConflict(String httpResponse, Etag etag, OperationMetadata operationMetadata, String key) {
    RavenJObject conflictsDoc = RavenJObject.parse(httpResponse);
    ConflictException result = tryResolveConflictOrCreateConcurrencyException(operationMetadata, key, conflictsDoc, etag);
    if (result != null) {
      throw result;
    }
    return directGet(operationMetadata, key);
  }

  @Override
  public MultiLoadResult get(final String[] ids, final String[] includes) {
    return get(ids, includes, null, null, false);
  }

  @Override
  public MultiLoadResult get(final String[] ids, final String[] includes, final String transformer) {
    return get(ids, includes, transformer, null, false);
  }

  @Override
  public MultiLoadResult get(final String[] ids, final String[] includes, final String transformer, final Map<String, RavenJToken> transformerParameters) {
    return get(ids, includes, transformer, transformerParameters, false);
  }

  @Override
  public MultiLoadResult get(final String[] ids, final String[] includes, final String transformer, final Map<String, RavenJToken> transformerParameters, final boolean metadataOnly) {
    return executeWithReplication(HttpMethods.GET, new Function1<OperationMetadata, MultiLoadResult>() {

      @Override
      public MultiLoadResult apply(OperationMetadata operationMetadata) {
        return directGet(ids, operationMetadata, includes, transformer, transformerParameters != null ? transformerParameters : new HashMap<String, RavenJToken>(), metadataOnly);
      }
    });
  }

  protected MultiLoadResult directGet(final String[] ids, final OperationMetadata operationMetadata, final String[] includes, final String transformer, final Map<String, RavenJToken> transformerParameters, final boolean metadataOnly) {
    String path = operationMetadata.getUrl() + "/queries/?";

    if (metadataOnly)
      path += "metadata-only=true&";
    if (includes != null && includes.length > 0) {
      List<String> tokens = new ArrayList<>();
      for (String include: includes) {
        tokens.add("include=" + include);
      }
      path += "&" + StringUtils.join(tokens, "&");
    }
    if (StringUtils.isNotEmpty(transformer)) {
      path += "&transformer=" + transformer;
    }

    if (transformerParameters != null) {
      for (Entry<String, RavenJToken> transformerParameter: transformerParameters.entrySet()) {
        path += String.format("&tp-%s=%s", transformerParameter.getKey(), transformerParameter.getValue());
      }
    }


    RavenJObject metadata = new RavenJObject();
    Set<String> uniqueIds = new LinkedHashSet<>(Arrays.asList(ids));
    // if it is too big, we drop to POST (note that means that we can't use the HTTP cache any longer)
    // we are fine with that, requests to load that many items are probably going to be rare
    HttpJsonRequest request = null;

    try {
      int uniqueIdsSum = 0;
      for (String id: ids) {
        uniqueIdsSum += id.length();
      }

      if (uniqueIdsSum < 1024) {
        for (String uniqueId: uniqueIds) {
          path += "&id=" + UrlUtils.escapeDataString(uniqueId);
        }
        request = jsonRequestFactory.createHttpJsonRequest(
          new CreateHttpJsonRequestParams(this, path, HttpMethods.GET, metadata, operationMetadata.getCredentials(), convention)
          .addOperationHeaders(operationsHeaders))
          .addReplicationStatusHeaders(url, operationMetadata.getUrl(), replicationInformer, convention.getFailoverBehavior(), new HandleReplicationStatusChangesCallback());

      } else {
        request = jsonRequestFactory.createHttpJsonRequest(
          new CreateHttpJsonRequestParams(this, path, HttpMethods.POST, metadata, operationMetadata.getCredentials(), convention)
          .addOperationHeaders(operationsHeaders))
          .addReplicationStatusHeaders(url, operationMetadata.getUrl(), replicationInformer, convention.getFailoverBehavior(), new HandleReplicationStatusChangesCallback());
        request.write(RavenJToken.fromObject(uniqueIds).toString());
      }

      RavenJToken result = request.readResponseJson();
      return completeMultiGet(operationMetadata, ids, includes,transformer, transformerParameters, result);
    } finally {
      if (request != null) {
        request.close();
      }
    }
  }

  private MultiLoadResult completeMultiGet(final OperationMetadata operationMetadata, final String[] keys,
    final String[] includes, final String transformer, final Map<String, RavenJToken> transformerParameters, RavenJToken result) {
    ErrorResponseException responseException;
    try {

      HashSet<String> uniqueKeys = new HashSet<>(Arrays.asList(keys));

      List<RavenJObject> results = new ArrayList<>();
      for (RavenJToken token: result.value(RavenJArray.class, "Results")) {
        if (token instanceof RavenJObject) {
          results.add((RavenJObject) token);
        }
      }

      Map<String, RavenJObject> documents = new HashMap<>();
      for (RavenJObject doc : results) {
        if (doc.containsKey("@metadata") && doc.get("@metadata").value(String.class, "@id") != null) {
          documents.put(doc.get("@metadata").value(String.class, "@id"), doc);
        }
      }

      if (results.size() >= uniqueKeys.size()) {
        for (int i = 0; i < uniqueKeys.size(); i++) {
          String key = keys[i];
          if (documents.containsKey(key)) {
            continue;
          }
          documents.put(key, results.get(i));
        }
      }

      MultiLoadResult multiLoadResult = new MultiLoadResult();

      List<RavenJObject> includesList = new ArrayList<>();
      for (RavenJToken token: result.value(RavenJArray.class, "Includes")) {
        includesList.add((RavenJObject) token);
      }
      multiLoadResult.setIncludes(includesList);

      List<RavenJObject> resultsList = new ArrayList<>();
      for (String key: keys) {
        if (documents.containsKey(key)) {
          resultsList.add(documents.get(key));
        } else {
          resultsList.add(null);
        }
      }
      multiLoadResult.setResults(resultsList);

      List<RavenJObject> docResults = new ArrayList<>();
      docResults.addAll(resultsList);
      docResults.addAll(includesList);

      return retryOperationBecauseOfConflict(operationMetadata, docResults, multiLoadResult, new Function0<MultiLoadResult>() {
        @Override
        public MultiLoadResult apply() {
          return directGet(keys, operationMetadata, includes, transformer, transformerParameters, false);
        }
      }, null);
    } catch (ErrorResponseException e) {
      if (e.getStatusCode() != HttpStatus.SC_CONFLICT) {
        throw e;
      }
      responseException = e;
    }
    throw fetchConcurrencyException(responseException);
  }

  @Override
  public List<JsonDocument> getDocuments(int start, int pageSize) {
    return getDocuments(start, pageSize, false);
  }

  @Override
  public List<JsonDocument> getDocuments(final int start, final int pageSize, final boolean metadataOnly) {
    return executeWithReplication(HttpMethods.GET, new Function1<OperationMetadata, List<JsonDocument>>() {
      @Override
      public List<JsonDocument> apply(OperationMetadata operationMetadata) {
        String requestUri = operationMetadata.getUrl() + "/docs?start=" + start + "&pageSize=" + pageSize;
        if (metadataOnly) {
          requestUri += "&metadata-only=true";
        }
        try (HttpJsonRequest request = jsonRequestFactory
          .createHttpJsonRequest(new CreateHttpJsonRequestParams(ServerClient.this, requestUri, HttpMethods.GET,
            new RavenJObject(), operationMetadata.getCredentials(), convention)
          .addOperationHeaders(operationsHeaders))) {
          RavenJToken responseJson = request.readResponseJson();
          return SerializationHelper.ravenJObjectsToJsonDocuments(responseJson);
        }
      }
    });
  }

  @Override
  public List<JsonDocument> getDocuments(final Etag fromEtag, final int pageSize) {
    return getDocuments(fromEtag, pageSize, false);
  }

  @Override
  public List<JsonDocument> getDocuments(final Etag fromEtag, final int pageSize, final boolean metadataOnly) {
    return executeWithReplication(HttpMethods.GET, new Function1<OperationMetadata, List<JsonDocument>>() {
      @Override
      public List<JsonDocument> apply(OperationMetadata operationMetadata) {
        String requestUri = operationMetadata.getUrl() + "/docs?etag=" + fromEtag + "&pageSize=" + pageSize;
        if (metadataOnly) {
          requestUri += "&metadata-only=true";
        }
        try (HttpJsonRequest request = jsonRequestFactory
          .createHttpJsonRequest(new CreateHttpJsonRequestParams(ServerClient.this, requestUri, HttpMethods.GET,
            new RavenJObject(), operationMetadata.getCredentials(), convention)
          .addOperationHeaders(operationsHeaders))) {
          RavenJToken responseJson = request.readResponseJson();
          return SerializationHelper.ravenJObjectsToJsonDocuments(responseJson);
        }
      }
    });
  }

  @Override
  public Operation updateByIndex(String indexName, IndexQuery queryToUpdate, PatchRequest[] patchRequests) {
    return updateByIndex(indexName, queryToUpdate, patchRequests, null);
  }

  @Override
  public Operation updateByIndex(String indexName, IndexQuery queryToUpdate, ScriptedPatchRequest patch) {
    return updateByIndex(indexName, queryToUpdate, patch, null);
  }

  @Override
  public Operation updateByIndex(String indexName, IndexQuery queryToUpdate, PatchRequest[] patchRequests, BulkOperationOptions options) {
    RavenJArray array = new RavenJArray();
    for (PatchRequest request: patchRequests) {
      array.add(request.toJson());
    }

    String requestData = array.toString();
    BulkOperationOptions notNullOptions = (options != null) ? options : new BulkOperationOptions();
    return updateByIndexImpl(indexName, queryToUpdate, notNullOptions, requestData, HttpMethods.PATCH);
  }

  @Override
  public Operation updateByIndex(String indexName, IndexQuery queryToUpdate, ScriptedPatchRequest patch, BulkOperationOptions options) {
    String requestData = RavenJObject.fromObject(patch).toString();
    BulkOperationOptions notNullOptions = (options != null) ? options : new BulkOperationOptions();
    return updateByIndexImpl(indexName, queryToUpdate, notNullOptions, requestData, HttpMethods.EVAL);
  }

  @Override
  public MultiLoadResult moreLikeThis(MoreLikeThisQuery query) {
    final String requestUrl = query.getRequestUri();
    ensureIsNotNullOrEmpty(requestUrl, "url");
    RavenJToken result = executeWithReplication(HttpMethods.GET, new Function1<OperationMetadata, RavenJToken>() {
      @Override
      public RavenJToken apply(OperationMetadata operationMetadata) {
        RavenJObject metadata = new RavenJObject();
        try (HttpJsonRequest request = jsonRequestFactory
          .createHttpJsonRequest(new CreateHttpJsonRequestParams(ServerClient.this, operationMetadata.getUrl() + requestUrl, HttpMethods.GET,
            metadata, operationMetadata.getCredentials(), convention)
          .addOperationHeaders(operationsHeaders))) {
          return request.readResponseJson();
        }
      }
    });

    MultiLoadResult multiLoadResult = new MultiLoadResult();
    multiLoadResult.setIncludes(new ArrayList<>(result.value(RavenJArray.class, "Includes").values(RavenJObject.class)));
    multiLoadResult.setResults(new ArrayList<>(result.value(RavenJArray.class, "Results").values(RavenJObject.class)));
    return multiLoadResult;
  }

  @Override
  public Long nextIdentityFor(final String name) {
    return executeWithReplication(HttpMethods.POST, new Function1<OperationMetadata, Long>() {
      @Override
      public Long apply(OperationMetadata operationMetadata) {
        return directNextIdentityFor(name, operationMetadata);
      }
    });
  }

  protected Long directNextIdentityFor(String name, OperationMetadata operationMetadata) {
    try (HttpJsonRequest jsonRequest = jsonRequestFactory.createHttpJsonRequest(
      new CreateHttpJsonRequestParams(this, operationMetadata.getUrl() + "/identity/next?name=" + UrlUtils.escapeDataString(name),
        HttpMethods.POST, new RavenJObject(), operationMetadata.getCredentials(), convention)
      .addOperationHeaders(operationsHeaders))) {
      RavenJToken ravenJToken = jsonRequest.readResponseJson();
      return ravenJToken.value(Long.class, "Value");
    }
  }

  @SuppressWarnings("boxing")
  @Override
  public long seedIdentityFor(final String name, final long value) {
    return executeWithReplication(HttpMethods.POST, new Function1<OperationMetadata, Long>() {
      @Override
      public Long apply(OperationMetadata operationMetadata) {
        return directSeedIdentityFor(operationMetadata, name, value);
      }
    });
  }

  @SuppressWarnings("boxing")
  long directSeedIdentityFor(OperationMetadata operationMetadata, String name, long value) {
    try (HttpJsonRequest request = jsonRequestFactory.createHttpJsonRequest(
      new CreateHttpJsonRequestParams(this, operationMetadata.getUrl() + "/identity/seed?name=" + UrlUtils.escapeDataString(name)+ "&value=" + value, HttpMethods.POST, new RavenJObject(), operationMetadata.getCredentials(), convention)
      .addOperationHeaders(operationsHeaders))) {
      RavenJToken readResponseJson = request.readResponseJson();
      return readResponseJson.value(Long.TYPE, "Value");
    }
  }

  private Operation updateByIndexImpl(final String indexName, final IndexQuery queryToUpdate, final BulkOperationOptions options, final String requestData, final HttpMethods method) {
    return executeWithReplication(method, new Function1<OperationMetadata, Operation>() {
      @Override
      public Operation apply(OperationMetadata operationMetadata) {
        return directUpdateByIndexImpl(operationMetadata, indexName, queryToUpdate, options, requestData, method);
      }
    });
  }

  @SuppressWarnings("boxing")
  protected Operation directUpdateByIndexImpl(OperationMetadata operationMetadata, String indexName, IndexQuery queryToUpdate, BulkOperationOptions options, String requestData, HttpMethods method) {
    BulkOperationOptions notNullOptions = (options != null) ? options : new BulkOperationOptions();
    String path = queryToUpdate.getIndexQueryUrl(operationMetadata.getUrl(), indexName, "bulk_docs")
      + "&allowStale=" + notNullOptions.isAllowStale() + "&maxOpsPerSec=" + notNullOptions.getMaxOpsPerSec()
      + "&details=" + notNullOptions.isRetrieveDetails();
    if (notNullOptions.getStaleTimeout() != null) {
      path += "&staleTimeout=" + notNullOptions.getStaleTimeout();
    }
    try (HttpJsonRequest request = jsonRequestFactory.createHttpJsonRequest(
      new CreateHttpJsonRequestParams(this, path, method, new RavenJObject(), operationMetadata.getCredentials(), convention)
      .addOperationHeaders(operationsHeaders))
      .addReplicationStatusHeaders(url, operationMetadata.getUrl(), replicationInformer, convention.getFailoverBehavior(), new HandleReplicationStatusChangesCallback())) {

      RavenJToken jsonResponse;
      try {
        request.write(requestData);
        jsonResponse = request.readResponseJson();
      } catch (ErrorResponseException e) {
        if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
          throw new IllegalStateException("There is no index named: " + indexName);
        }
        throw e;
      }
      return new Operation(this, jsonResponse.value(Long.TYPE, "OperationId"));
    }
  }

  @Override
  public FacetResults getFacets(final String index, final IndexQuery query, final String facetSetupDoc) {
    return getFacets(index, query, facetSetupDoc, 0, null);
  }

  @Override
  public FacetResults getFacets(final String index, final IndexQuery query, final String facetSetupDoc, final int start) {
    return getFacets(index, query, facetSetupDoc, start, null);
  }

  @Override
  public FacetResults getFacets(final String index, final IndexQuery query, final String facetSetupDoc, final int start, final Integer pageSize) {
    return executeWithReplication(HttpMethods.GET, new Function1<OperationMetadata, FacetResults>() {
      @Override
      public FacetResults apply(OperationMetadata operationMetadata) {
        return directGetFacets(operationMetadata, index, query, facetSetupDoc, start, pageSize);
      }
    });
  }

  @SuppressWarnings("boxing")
  protected FacetResults directGetFacets(OperationMetadata operationMetadata, String index, IndexQuery query, String facetSetupDoc, int start, Integer pageSize) {
    String requestUri = operationMetadata.getUrl() + String.format("/facets/%s?facetDoc=%s&%s&facetStart=%d&facetPageSize=%s",
      UrlUtils.escapeUriString(index),
      UrlUtils.escapeDataString(facetSetupDoc),
      query.getMinimalQueryString(),
      start,
      pageSize != null ? pageSize : "");

    try(final HttpJsonRequest request = jsonRequestFactory.createHttpJsonRequest(
      new CreateHttpJsonRequestParams(this, requestUri, HttpMethods.GET, new RavenJObject(), operationMetadata.getCredentials(), convention)
      .addOperationHeaders(operationsHeaders))
      .addReplicationStatusHeaders(url, operationMetadata.getUrl(), replicationInformer, convention.getFailoverBehavior(), new HandleReplicationStatusChangesCallback())) {

      CachedRequestOp cachedRequestDetails = jsonRequestFactory.configureCaching(requestUri, new Action2<String, String>() {
        @Override
        public void apply(String key, String val) {
          request.addOperationHeader(key, val);
        }
      });
      request.setCachedRequestDetails(cachedRequestDetails.getCachedRequest());
      request.setSkipServerCheck(cachedRequestDetails.isSkipServerCheck());

      RavenJObject json = (RavenJObject)request.readResponseJson();
      return convention.createSerializer().deserialize(json.toString(), FacetResults.class);
    }
  }

  @SuppressWarnings("boxing")
  @Override
  public FacetResults[] getMultiFacets(final FacetQuery[] facetedQueries) {
    JsonSerializer jsonSerializer = convention.createSerializer();

    GetRequest[] multiGetRequestItems = new GetRequest[facetedQueries.length];
    for (int i = 0; i < facetedQueries.length; i++) {
      FacetQuery x = facetedQueries[i];

      String addition = null;
      if (x.getFacetSetupDoc() != null) {
        addition = "facetDoc=" + x.getFacetSetupDoc();
        GetRequest request = new GetRequest();
        request.setUrl("/facets/" + x.getIndexName());
        request.setQuery(String.format("%s&facetStart=%d&facetPageSize=%d&%d",
          x.getQuery().getQueryString(),
          x.getQuery().getStart(),
          x.getQuery().getPageSize(),
          addition));
        multiGetRequestItems[i] = request;
      } else {
        String serializedFacets = jsonSerializer.serializeAsString(x.getFacets());
        if (serializedFacets.length() < (32 * 1024 - 1)) {
          addition = "facets=" + UrlUtils.escapeDataString(serializedFacets);
          GetRequest request = new GetRequest();
          request.setUrl("/facets/" + x.getIndexName());
          request.setQuery(String.format("%s&facetStart=%d&facetPageSize=%d&%d",
            x.getQuery().getQueryString(),
            x.getQuery().getStart(),
            x.getQuery().getPageSize(),
            addition));
          multiGetRequestItems[i] = request;
        } else {
          GetRequest request = new GetRequest();
          request.setUrl("/facets/" + x.getIndexName());
          request.setMethod(HttpMethods.POST);
          request.setContent(serializedFacets);
          multiGetRequestItems[i] = request;
        }
      }
    }

    GetResponse[] results = multiGet(multiGetRequestItems);

    FacetResults[] facetResults = new FacetResults[results.length];
    for (int facetResultCounter = 0; facetResultCounter < facetResults.length; facetResultCounter++) {
      GetResponse curFacetDoc = results[facetResultCounter];
      if (curFacetDoc.isRequestHasErrors()) {
        throw new IllegalStateException("Got an error from server, status code: " + curFacetDoc.getStatus() + "\n" + curFacetDoc.getResult());
      }
      facetResults[facetResultCounter] = jsonSerializer.deserialize(curFacetDoc.getResult(), FacetResults.class);
    }
    return facetResults;
  }

  @Override
  public FacetResults getFacets(final String index, final IndexQuery query, final List<Facet> facets) {
    return getFacets(index, query, facets, 0, null);
  }

  @Override
  public FacetResults getFacets(final String index, final IndexQuery query, final List<Facet> facets, final int start) {
    return getFacets(index, query, facets, start, null);
  }

  @SuppressWarnings({"unused", "boxing"})
  @Override
  public FacetResults getFacets(final String index, final IndexQuery query, final List<Facet> facets, final int start, final Integer pageSize) {

    RavenJArray ravenJArray = (RavenJArray) RavenJToken.fromObject(facets);
    for (RavenJToken facet : ravenJArray) {
      RavenJObject obj = (RavenJObject) facet;
      if (Objects.equals(obj.value(String.class, "Name"), obj.value(String.class, "DisplayName"))) {
        obj.remove("DisplayName");
      }
      RavenJArray jArray = obj.value(RavenJArray.class, "Ranges");
      if (jArray != null && jArray.size() == 0) {
        obj.remove("Ranges");
      }
      for (String props : new HashSet<String>(obj.getKeys())) {
        if (obj.get(props).getType() == JTokenType.NULL) {
          obj.remove(props);
        }
      }
      if ("None".equals(obj.value(String.class, "Aggregation"))) {
        obj.remove("Aggregation");
      }
      if ("Default".equals(obj.value(String.class, "Mode"))) {
        obj.remove("Mode");
      }
      if ("ValueAsc".equals(obj.value(String.class, "TermSortMode"))) {
        obj.remove("TermSortMode");
      }
      if (!obj.value(Boolean.class, "IncludeRemainingTerms")) {
        obj.remove("IncludeRemainingTerms");
      }
    }

    final String facetsJson = ravenJArray.toString();
    final HttpMethods method = facetsJson.length() > 1024 ? HttpMethods.POST : HttpMethods.GET;
    if (HttpMethods.POST.equals(method)) {
      FacetQuery facetQuery = new FacetQuery();
      facetQuery.setFacets(facets);
      facetQuery.setIndexName(index);
      facetQuery.setQuery(query);
      facetQuery.setPageSize(pageSize);
      facetQuery.setPageStart(start);
      return getMultiFacets(new FacetQuery[] { facetQuery })[0];
    }
    return executeWithReplication(method, new Function1<OperationMetadata, FacetResults>() {
      @Override
      public FacetResults apply(OperationMetadata operationMetadata) {
        return directGetFacets(operationMetadata, index, query, facetsJson, start, pageSize, method);
      }
    });
  }

  @SuppressWarnings("boxing")
  protected FacetResults directGetFacets(OperationMetadata operationMetadata, String index, IndexQuery query, String facetsJson, int start, Integer pageSize, HttpMethods method) {
    String requestUri = operationMetadata.getUrl() + String.format("/facets/%s?%s&facetStart=%d&facetPageSize=%s",
      UrlUtils.escapeUriString(index),
      query.getQueryString(),
      start,
      (pageSize !=null ) ? pageSize.toString() : "");

    if(method == HttpMethods.GET) {
      requestUri += "&facets=" + UrlUtils.escapeDataString(facetsJson);
    }

    try (HttpJsonRequest request = jsonRequestFactory.createHttpJsonRequest(
      new CreateHttpJsonRequestParams(this, requestUri, method, new RavenJObject(), operationMetadata.getCredentials(), convention)
      .addOperationHeaders(operationsHeaders))
      .addReplicationStatusHeaders(url, operationMetadata.getUrl(), replicationInformer, convention.getFailoverBehavior(), new HandleReplicationStatusChangesCallback())) {
      if (method != HttpMethods.GET)
        request.write(facetsJson);

      RavenJObject json = (RavenJObject)request.readResponseJson();
      return convention.createSerializer().deserialize(json, FacetResults.class);
    }
  }

  @Override
  public LogItem[] getLogs(final boolean errorsOnly) {
    return executeWithReplication(HttpMethods.GET, new Function1<OperationMetadata, LogItem[]>() {
      @SuppressWarnings("synthetic-access")
      @Override
      public LogItem[] apply(OperationMetadata operationMetadata) {
        String requestUri = url + "/logs";
        if (errorsOnly) {
          requestUri += "?type=error";
        }
        try (HttpJsonRequest request = jsonRequestFactory.createHttpJsonRequest(new CreateHttpJsonRequestParams(ServerClient.this, requestUri, HttpMethods.GET, new RavenJObject(), operationMetadata.getCredentials(), convention))) {
          request.addOperationHeaders(operationsHeaders);
          request.addReplicationStatusHeaders(url, operationMetadata.getUrl(), replicationInformer, convention.getFailoverBehavior(), new HandleReplicationStatusChangesCallback());

          RavenJToken result = request.readResponseJson();
          return convention.createSerializer().deserialize(result, LogItem[].class);
        }
      }
    });
  }

  @Override
  public ReplicationStatistics getReplicationInfo() {
    try (HttpJsonRequest request = jsonRequestFactory.createHttpJsonRequest(new CreateHttpJsonRequestParams(ServerClient.this, url + "/replication/info",HttpMethods.GET, new RavenJObject(), credentialsThatShouldBeUsedOnlyInOperationsWithoutReplication, convention))) {
      request.addOperationHeaders(operationsHeaders);
      RavenJToken result = request.readResponseJson();
      return convention.createSerializer().deserialize(result, ReplicationStatistics.class);
    }
  }

  @Override
  public LicensingStatus getLicenseStatus() {
    try (HttpJsonRequest request = jsonRequestFactory.createHttpJsonRequest(new CreateHttpJsonRequestParams(ServerClient.this,
      MultiDatabase.getRootDatabaseUrl(url) + "/license/status", HttpMethods.GET, new RavenJObject(), credentialsThatShouldBeUsedOnlyInOperationsWithoutReplication, convention))) {
      request.addOperationHeaders(operationsHeaders);
      RavenJToken result = request.readResponseJson();
      return convention.createSerializer().deserialize(result, LicensingStatus.class);
    }
  }

  @Override
  public BuildNumber getBuildNumber() {
    try (HttpJsonRequest request = jsonRequestFactory.createHttpJsonRequest(new CreateHttpJsonRequestParams(this, url + "/build/version",
      HttpMethods.GET, new RavenJObject(), credentialsThatShouldBeUsedOnlyInOperationsWithoutReplication, convention))) {
      request.addOperationHeaders(operationsHeaders);

      RavenJToken result = request.readResponseJson();
      return convention.createSerializer().deserialize(result, BuildNumber.class);
    }
  }

  @Override
  public IndexMergeResults getIndexMergeSuggestions() {
    try (HttpJsonRequest request = jsonRequestFactory.createHttpJsonRequest(new CreateHttpJsonRequestParams(this, url + "/debug/suggest-index-merge",
      HttpMethods.GET, new RavenJObject(), credentialsThatShouldBeUsedOnlyInOperationsWithoutReplication, convention))) {
      request.addOperationHeaders(operationsHeaders);
      RavenJToken result = request.readResponseJson();
      return convention.createSerializer().deserialize(result, IndexMergeResults.class);
    }
  }

  @Override
  public List<JsonDocument> startsWith(final String keyPrefix, final String matches, final int start, final int pageSize) {
    return startsWith(keyPrefix, matches, start, pageSize, false);
  }

  @Override
  public List<JsonDocument> startsWith(final String keyPrefix, final String matches, final int start, final int pageSize, final boolean metadataOnly) {
    return startsWith(keyPrefix, matches, start, pageSize, metadataOnly, null, null, null, null, null);
  }

  @Override
  public List<JsonDocument> startsWith(final String keyPrefix, final String matches, final int start, final int pageSize, final boolean metadataOnly, final String exclude) {
    return startsWith(keyPrefix, matches, start, pageSize, metadataOnly, exclude, null, null, null, null);
  }

  @Override
  public List<JsonDocument> startsWith(final String keyPrefix, final String matches, final int start, final int pageSize, final boolean metadataOnly, final String exclude, final RavenPagingInformation pagingInformation) {
    return startsWith(keyPrefix, matches, start, pageSize, metadataOnly, exclude, pagingInformation, null, null, null);
  }

  @Override
  public List<JsonDocument> startsWith(final String keyPrefix, final String matches, final int start, final int pageSize, final boolean metadataOnly, final String exclude, final RavenPagingInformation pagingInformation, final String transformer, final Map<String, RavenJToken> transformerParameters) {
    return startsWith(keyPrefix, matches, start, pageSize, metadataOnly, exclude, pagingInformation, transformer, transformerParameters, null);
  }

  @Override
  public List<JsonDocument> startsWith(final String keyPrefix, final String matches, final int start, final int pageSize, final boolean metadataOnly, final String exclude, final RavenPagingInformation pagingInformation, final String transformer, final Map<String, RavenJToken> transformerParameters, final String skipAfter) {
    ensureIsNotNullOrEmpty(keyPrefix, "keyPrefix");
    return executeWithReplication(HttpMethods.GET, new Function1<OperationMetadata, List<JsonDocument>>() {
      @Override
      public List<JsonDocument> apply(OperationMetadata operationMetadata) {
        return directStartsWith(operationMetadata, keyPrefix, matches, start, pageSize, metadataOnly, exclude, pagingInformation, transformer, transformerParameters, skipAfter);
      }
    });
  }

  @SuppressWarnings({"null", "boxing"})
  protected List<JsonDocument> directStartsWith(final OperationMetadata operationMetadata, final String keyPrefix,
    final String matches, final int start, final int pageSize, final boolean metadataOnly, final String exclude,
    final RavenPagingInformation pagingInformation, final String transformer,
    final Map<String, RavenJToken> transformerParameters, final String skipAfter) {
    RavenJObject metadata = new RavenJObject();

    int actualStart = start;
    boolean nextPage = pagingInformation != null && pagingInformation.isForPreviousPage(start, pageSize);
    if (nextPage) {
      actualStart = pagingInformation.getNextPageStart();
    }


    String actualUrl = operationMetadata.getUrl() + String.format("/docs?startsWith=%s&matches=%s&exclude=%s&start=%d&pageSize=%d", UrlUtils.escapeDataString(keyPrefix),
      UrlUtils.escapeDataString(StringUtils.trimToEmpty(matches)),
      UrlUtils.escapeDataString(StringUtils.trimToEmpty(exclude)),actualStart, pageSize);
    if (metadataOnly) {
      actualUrl += "&metadata-only=true";
    }

    if (StringUtils.isNotEmpty(skipAfter)) {
      actualUrl += "&skipAfter=" + UrlUtils.escapeDataString(skipAfter);
    }

    if (StringUtils.isNotEmpty(transformer)) {
      actualUrl += "&transformer=" + transformer;
      if (transformerParameters != null) {
        for (Map.Entry<String, RavenJToken> entry : transformerParameters.entrySet()) {
          actualUrl += String.format("&tp-%s=%s", entry.getKey(), entry.getValue());
        }
      }
    }

    if (nextPage) {
      actualUrl += "&next-page=true";
    }

    try (HttpJsonRequest request = jsonRequestFactory.createHttpJsonRequest(
      new CreateHttpJsonRequestParams(this, actualUrl, HttpMethods.GET, metadata, operationMetadata.getCredentials(), convention).
      addOperationHeaders(operationsHeaders)).
      addReplicationStatusHeaders(url, operationMetadata.getUrl(), replicationInformer, convention.getFailoverBehavior(), new HandleReplicationStatusChangesCallback())) {
      RavenJArray result = (RavenJArray)request.readResponseJson();

      if (pagingInformation != null) {
        try {
          int nextPageStart = Integer.parseInt(request.getResponseHeaders().get(Constants.NEXT_PAGE_START));
          pagingInformation.fill(start, pageSize, nextPageStart);
        } catch (NumberFormatException e) {
          //ignore
        }
      }

      List<RavenJObject> docResults = new ArrayList<>();
      for (RavenJToken token : result) {
        if (token instanceof RavenJObject) {
          docResults.add((RavenJObject)token.cloneToken());
        }
      }

      final int actualStartFinal = actualStart;

      List<JsonDocument> startsWithResults = SerializationHelper.ravenJObjectsToJsonDocuments(docResults);
      return retryOperationBecauseOfConflict(operationMetadata, docResults, startsWithResults, new Function0<List<JsonDocument>>() {
        @Override
        public List<JsonDocument> apply() {
          return startsWith(keyPrefix, matches, actualStartFinal, pageSize, metadataOnly, exclude, pagingInformation, transformer, transformerParameters, skipAfter);
        }
      }, new Function1<String, ConflictException>() {
        @Override
        public ConflictException apply(String conflictedResultId) {
          ConflictException conflictException = new ConflictException("Conflict detected on "
            + conflictedResultId.substring(0, conflictedResultId.indexOf("/conflicts/")) +
            ", conflict must be resolved before the document will be accessible", true);
          conflictException.setConflictedVersionIds(new String[] { conflictedResultId });
          return conflictException;
        }
      });
    }
  }

  @Override
  public GetResponse[] multiGet(final GetRequest[] requests) {
    return multiGetInternal(requests, null);
  }

  private GetResponse[] multiGetInternal(final GetRequest[] requests, final Reference<OperationMetadata> operationMetadataRef) {
    return executeWithReplication(HttpMethods.GET, new Function1<OperationMetadata, GetResponse[]>() {
      @Override
      public GetResponse[] apply(OperationMetadata operationMetadata) {
        return directMultiGetInternal(operationMetadata, requests, operationMetadataRef);
      }
    });
  }

  protected GetResponse[] directMultiGetInternal(final OperationMetadata operationMetadata, GetRequest[] requests, Reference<OperationMetadata> operationMetadataRef) {
    if (operationMetadataRef != null) {
      operationMetadataRef.value = operationMetadata;
    }
    MultiGetOperation multiGetOperation = new MultiGetOperation(this, convention, operationMetadata.getUrl(), requests);
    // logical GET even though the actual request is a POST
    try (HttpJsonRequest httpJsonRequest = jsonRequestFactory.createHttpJsonRequest(new CreateHttpJsonRequestParams(this, multiGetOperation.getRequestUri(),
      HttpMethods.POST, new RavenJObject(), operationMetadata.getCredentials(), convention))) {
      GetRequest[] requestsForServer =
        multiGetOperation.preparingForCachingRequest(jsonRequestFactory);

      String postedData = JsonConvert.serializeObject(requestsForServer);

      if (multiGetOperation.canFullyCache(jsonRequestFactory, httpJsonRequest, postedData)) {
        return multiGetOperation.handleCachingResponse(new GetResponse[requests.length],
          jsonRequestFactory);
      }

      httpJsonRequest.write(postedData);
      RavenJArray results = (RavenJArray)httpJsonRequest.readResponseJson();

      GetResponse[] responses = convention.createSerializer().deserialize(results, GetResponse[].class);

      multiGetOperation.tryResolveConflictOrCreateConcurrencyException(responses, new Function3<String, RavenJObject, Etag, ConflictException>() {
        @SuppressWarnings("synthetic-access")
        @Override
        public ConflictException apply(String key, RavenJObject conflictsDoc, Etag etag) {
          return tryResolveConflictOrCreateConcurrencyException(operationMetadata, key, conflictsDoc, etag);
        }
      });

      return multiGetOperation.handleCachingResponse(responses, jsonRequestFactory);
    }
  }

  @Override
  public QueryResult query(String index, IndexQuery query) {
    return query(index, query, null, false, false);
  }

  @Override
  public QueryResult query(String index, IndexQuery query, String[] includes) {
    return query(index, query, includes, false, false);
  }

  @Override
  public QueryResult query(String index, IndexQuery query, String[] includes, boolean metadataOnly) {
    return query(index, query, includes, metadataOnly, false);
  }

  @Override
  public QueryResult query(final String index, final IndexQuery query, final String[] includes, final boolean metadataOnly, final boolean indexEntriesOnly) {
    ensureIsNotNullOrEmpty(index, "index");
    final HttpMethods method = query.getQuery() == null || query.getQuery().length() <= convention.getMaxLengthOfQueryUsingGetUrl()
      ? HttpMethods.GET : HttpMethods.POST;

    if (HttpMethods.POST.equals(method)) {
      return executeWithReplication(method, new Function1<OperationMetadata, QueryResult>() {
        @Override
        public QueryResult apply(OperationMetadata operationMetadata) {
          return directQueryAsPost(index, query, operationMetadata, includes, metadataOnly, indexEntriesOnly);
        }
      });
    }
    return executeWithReplication(method, new Function1<OperationMetadata, QueryResult>() {
      @Override
      public QueryResult apply(OperationMetadata operationMetadata) {
        return directQueryAsGet(index, query, operationMetadata, includes, metadataOnly, indexEntriesOnly);
      }
    });
  }

  protected QueryResult directQueryAsGet(final String index, final IndexQuery query, final OperationMetadata operationMetadata, final String[] includes, final boolean metadataOnly, final boolean includeEntries) {
    String path = query.getIndexQueryUrl(operationMetadata.getUrl(), index, "indexes", true, true);
    if (metadataOnly)
      path += "&metadata-only=true";
    if (includeEntries)
      path += "&debug=entries";
    if (includes != null && includes.length > 0) {
      for (String include: includes) {
        path += "&include=" + include;
      }
    }

    try (HttpJsonRequest request = jsonRequestFactory.createHttpJsonRequest(
      new CreateHttpJsonRequestParams(this, path, HttpMethods.GET, new RavenJObject(), operationMetadata.getCredentials(), convention)
      .setAvoidCachingRequest(query.isDisableCaching())
      .addOperationHeaders(operationsHeaders))
      .addReplicationStatusHeaders(url, operationMetadata.getUrl(), replicationInformer,
        convention.getFailoverBehavior(),
        new HandleReplicationStatusChangesCallback())) {
      RavenJObject json = (RavenJObject)request.readResponseJson();

      ErrorResponseException responseException;
      try {
        if (json == null) throw new IllegalStateException("Got empty response from the server for the following request: " + request.getUrl());
        QueryResult queryResult = SerializationHelper.toQueryResult(json, HttpExtensions.getEtagHeader(request), request.getResponseHeaders().get("Temp-Request-Time"), request.getSize());
        List<RavenJObject> docsResults = new ArrayList<>();
        docsResults.addAll(queryResult.getResults());
        docsResults.addAll(queryResult.getIncludes());
        return retryOperationBecauseOfConflict(operationMetadata, docsResults, queryResult, new Function0<QueryResult>() {
          @Override
          public QueryResult apply() {
            return directQueryAsGet(index, query, operationMetadata, includes, metadataOnly, includeEntries);
          }
        }, null);

      } catch (ErrorResponseException e) {
        if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
          String text = e.getResponseString();
          if (text.contains("maxQueryString")) throw new IllegalStateException(text, e);
          throw new IllegalStateException("There is no index named: " + index, e);
        }
        responseException = e;

      }
      if (handleException(responseException)) return null;
      throw responseException;
    }
  }

  @SuppressWarnings("unused")
  protected QueryResult directQueryAsPost(final String index, final IndexQuery query, final OperationMetadata operationMetadata,
    final String[] includes, final boolean metadataOnly, final boolean indexEntriesOnly) {
    StringBuilder stringBuilder = new StringBuilder();
    query.appendQueryString(stringBuilder);

    if (includes != null && includes.length > 0) {
      for (String include: includes) {
        stringBuilder.append("&include=").append(include);
      }
    }

    if (metadataOnly) {
      stringBuilder.append("&metadata-only=true");
    }

    if (indexEntriesOnly) {
      stringBuilder.append("&debug=entries");
    }

    GetRequest getRequest = new GetRequest();
    getRequest.setQuery(stringBuilder.toString());
    getRequest.setUrl("/indexes/" + index);

    try {
      Reference<OperationMetadata> operationMetadataRef = new Reference<OperationMetadata>();

      GetResponse[] x = multiGetInternal(new GetRequest[] { getRequest }, operationMetadataRef);
      GetResponse getResponse = x[0];
      RavenJObject json = (RavenJObject) getResponse.getResult();
      QueryResult queryResult = SerializationHelper.toQueryResult(json, HttpExtensions.getEtagHeader(getResponse), getResponse.getHeaders().get("Temp-Request-Time"), -1);

      List<RavenJObject> docResults = new ArrayList<RavenJObject>();
      docResults.addAll(queryResult.getResults());
      docResults.addAll(queryResult.getIncludes());
      return retryOperationBecauseOfConflict(operationMetadataRef.value, docResults, queryResult, new Function0<QueryResult>() {
        @Override
        public QueryResult apply() {
          return query(index, query, includes, metadataOnly, indexEntriesOnly);
        }
      }, new Function1<String, ConflictException>() {
        @Override
        public ConflictException apply(String conflictedResultId) {
         ConflictException ex =  new ConflictException("Conflict detected on "
            + conflictedResultId.substring(0, conflictedResultId.indexOf("/conflicts/")) +
             ", conflict must be resolved before the document will be accessible", true);
           ex.setConflictedVersionIds(new String[] { conflictedResultId});
         return ex;
        }

      });
    } catch (ErrorResponseException errorResponseException) {
      if (errorResponseException.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
        String text = errorResponseException.getResponseString();
        if (text.contains("maxQueryString")) throw new IllegalStateException(text, errorResponseException);
        throw new IllegalStateException("There is no index named: " + index, errorResponseException);
      }
      if (handleException(errorResponseException)) return null;

      throw errorResponseException;
    }
  }

  private boolean handleException(ErrorResponseException e) {
    if (e.getStatusCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
      String content = e.getResponseString();
      RavenJObject json = RavenJObject.fromObject(content);
      ServerRequestError error = convention.createSerializer().deserialize(json, ServerRequestError.class);
      throw new ErrorResponseException(e, error.getError());

    }
    return false;
  }

  @Override
  public SuggestionQueryResult suggest(final String index, final SuggestionQuery suggestionQuery) {
    if (suggestionQuery == null) {
      throw new IllegalArgumentException("suggestionQuery");
    }
    return executeWithReplication(HttpMethods.GET, new Function1<OperationMetadata, SuggestionQueryResult>() {
      @Override
      public SuggestionQueryResult apply(OperationMetadata operationMetadata) {
        return directSuggest(index, suggestionQuery, operationMetadata);
      }
    });
  }

  @SuppressWarnings("boxing")
  protected SuggestionQueryResult directSuggest(String index, SuggestionQuery suggestionQuery, OperationMetadata operationMetadata) {
    String requestUri = operationMetadata.getUrl() + String.format("/suggest/%s?term=%s&field=%s&max=%d&popularity=%s",
      UrlUtils.escapeUriString(index),
      UrlUtils.escapeDataString(suggestionQuery.getTerm()),
      UrlUtils.escapeDataString(suggestionQuery.getField()),
      suggestionQuery.getMaxSuggestions(),
      suggestionQuery.isPopularity());

    if (suggestionQuery.getDistance() != null) {
      requestUri += "&distance=" + UrlUtils.escapeDataString(SharpEnum.value(suggestionQuery.getDistance()));
    }
    if (suggestionQuery.getAccuracy() != null) {
      requestUri += "&accuracy=" + NumberUtil.trimZeros(String.format(Constants.getDefaultLocale(), "%.4f", suggestionQuery.getAccuracy()));
    }

    try (HttpJsonRequest request = jsonRequestFactory.createHttpJsonRequest(
      new CreateHttpJsonRequestParams(this, requestUri, HttpMethods.GET, new RavenJObject(), operationMetadata.getCredentials(), convention)
      .addOperationHeaders(operationsHeaders))
      .addReplicationStatusHeaders(url, operationMetadata.getUrl(), replicationInformer, convention.getFailoverBehavior(), new HandleReplicationStatusChangesCallback())) {
      RavenJObject json = (RavenJObject)request.readResponseJson();

      List<String> suggestions = new ArrayList<>();

      SuggestionQueryResult result = new SuggestionQueryResult();
      RavenJArray array = (RavenJArray) json.get("Suggestions");
      for (RavenJToken token: array) {
        suggestions.add(token.value(String.class));
      }
      result.setSuggestions(suggestions.toArray(new String[0]));
      return result;
    }
  }

  @Override
  public BatchResult[] batch(final List<ICommandData> commandDatas) {
    return executeWithReplication(HttpMethods.POST, new Function1<OperationMetadata, BatchResult[]>() {
      @Override
      public BatchResult[] apply(OperationMetadata operationMetadata) {
        return directBatch(commandDatas, operationMetadata);
      }
    });
  }

  protected BatchResult[] directBatch(List<ICommandData> commandDatas, OperationMetadata operationMetadata) {
    RavenJObject metadata = new RavenJObject();
    try (HttpJsonRequest req = jsonRequestFactory.createHttpJsonRequest(
      new CreateHttpJsonRequestParams(this, operationMetadata.getUrl() + "/bulk_docs", HttpMethods.POST, metadata, operationMetadata.getCredentials(), convention)
      .addOperationHeaders(operationsHeaders))
      .addReplicationStatusHeaders(url, operationMetadata.getUrl(), replicationInformer, convention.getFailoverBehavior(), new HandleReplicationStatusChangesCallback())) {

      RavenJArray jArray = new RavenJArray();
      for (ICommandData command: commandDatas) {
        jArray.add(command.toJson());
      }

      ErrorResponseException responseException;
      try {
        req.write(jArray.toString());
        RavenJArray response = (RavenJArray)req.readResponseJson();

        if (response == null) {
          throw new IllegalStateException("Got null response from the server after doing a batch, something is very wrong. Probably a garbled response. Posted: " + jArray);
        }
        return JsonConvert.deserializeObject(BatchResult[].class, response.toString());
      } catch (ErrorResponseException e) {
        if (e.getStatusCode() != HttpStatus.SC_CONFLICT) {
          throw e;
        }
        responseException = e;
      }
      throw fetchConcurrencyException(responseException);
    }
  }

  protected ConcurrencyException fetchConcurrencyException(ErrorResponseException e) {
    String text = e.getResponseString();
    RavenJObject ravenJToken = RavenJObject.parse(text);
    return new ConcurrencyException(
      ravenJToken.value(Etag.class, "ExpectedETag"),
      ravenJToken.value(Etag.class, "ActualETag"),
      ravenJToken.value(String.class, "Error"),
      e);
  }

  private static void ensureIsNotNullOrEmpty(String key, String argName) {
    if (key == null || "".equals(key)) {
      throw new IllegalArgumentException("Key cannot be null or empty " + argName);
    }
  }

  @Override
  @Deprecated
  public AttachmentInformation[] getAttachments(final int start, final Etag startEtag, final int pageSize) {
    return executeWithReplication(HttpMethods.GET, new Function1<OperationMetadata, AttachmentInformation[]>() {
      @Override
      public AttachmentInformation[] apply(OperationMetadata operationMetadata) {
        return directGetAttachments(start, startEtag, pageSize, operationMetadata);
      }
    });
  }

  @Deprecated
  protected AttachmentInformation[] directGetAttachments(int start, Etag startEtag, int pageSize,
    OperationMetadata operationMetadata) {
    try (HttpJsonRequest webRequest = jsonRequestFactory.createHttpJsonRequest(
      new CreateHttpJsonRequestParams(this, operationMetadata.getUrl() + "/static/?pageSize=" + pageSize + "&etag=" + startEtag + "&start=" + start, HttpMethods.GET, new RavenJObject(), operationMetadata.getCredentials(), convention))
      .addReplicationStatusHeaders(url, operationMetadata.getUrl(), replicationInformer, convention.getFailoverBehavior(), new HandleReplicationStatusChangesCallback())) {
      RavenJArray json = (RavenJArray)webRequest.readResponseJson();
      return convention.createSerializer().deserialize(json, AttachmentInformation[].class);
    }
  }

  @Override
  @Deprecated
  public void putAttachment(final String key, final Etag etag, final InputStream data, final RavenJObject metadata) {
    executeWithReplication(HttpMethods.PUT, new Function1<OperationMetadata, Void>() {
      @Override
      public Void apply(OperationMetadata operationMetadata) {
        directPutAttachment(key, metadata, etag, data, operationMetadata);
        return null;
      }
    });
  }

  @Deprecated
  protected void directPutAttachment(String key, RavenJObject metadata, Etag etag, InputStream data, OperationMetadata operationMetadata) {
    if (metadata == null) {
      metadata = new RavenJObject();
    }
    if (etag != null) {
      metadata.set(Constants.METADATA_ETAG_FIELD, new RavenJValue(etag.toString()));
    }

    try (HttpJsonRequest jsonRequest = jsonRequestFactory.createHttpJsonRequest(
      new CreateHttpJsonRequestParams(this, operationMetadata.getUrl() + "/static/" + key, HttpMethods.PUT, metadata, operationMetadata.getCredentials(), convention))
      .addReplicationStatusHeaders(url, operationMetadata.getUrl(), replicationInformer, convention.getFailoverBehavior(), new HandleReplicationStatusChangesCallback())) {
      jsonRequest.write(data);
    }
  }

  @Override
  @Deprecated
  public Attachment getAttachment(final String key) {
    return executeWithReplication(HttpMethods.GET, new Function1<OperationMetadata, Attachment>() {
      @Override
      public Attachment apply(OperationMetadata operationMetadata) {
        return directGetAttachment(HttpMethods.GET, key, operationMetadata);
      }
    });
  }

  @Override
  @Deprecated
  public Attachment headAttachment(final String key) {
    return executeWithReplication(HttpMethods.HEAD, new Function1<OperationMetadata, Attachment>() {
      @Override
      public Attachment apply(OperationMetadata operationMetadata) {
        return directGetAttachment(HttpMethods.HEAD, key, operationMetadata);
      }
    });
  }

  @Deprecated
  protected Attachment directGetAttachment(HttpMethods method, String key, OperationMetadata operationMetadata) {
    RavenJObject metadata = new RavenJObject();
    CreateHttpJsonRequestParams createHttpJsonRequestParams = new CreateHttpJsonRequestParams(this, operationMetadata.getUrl() + "/static/" + key, method, metadata, operationMetadata.getCredentials(), convention);

    try (HttpJsonRequest request = jsonRequestFactory.createHttpJsonRequest(createHttpJsonRequestParams)
      .addReplicationStatusHeaders(url, operationMetadata.getUrl(), replicationInformer, convention.getFailoverBehavior(), new HandleReplicationStatusChangesCallback())) {
      ErrorResponseException responseException;

      try {
        byte[] result = request.readResponseBytes();

        handleReplicationStatusChanges(request.getResponseHeaders(), url, operationMetadata.getUrl());
        return new Attachment(HttpMethods.GET.equals(method), result, result.length, MetadataExtensions.filterHeadersAttachment(request.getResponseHeaders()),
          HttpExtensions.getEtagHeader(request), null);
      } catch (ErrorResponseException e) {
        if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) return null;
        if (e.getStatusCode() != HttpStatus.SC_CONFLICT) throw e;
        responseException = e;
      } catch (IOException e) {
        throw new IllegalStateException(e.getMessage(), e);
      }

      String stream = null;
      try (CloseableHttpResponse response = responseException.getResponse()) {
        stream = BomUtils.removeUTF8BOM(IOUtils.toString(response.getEntity().getContent(), "UTF-8")).trim();

        List<String> conflictedIds;
        if (HttpMethods.GET.equals(method)) {
          RavenJObject conflictsDoc = RavenJObject.parse(stream);
          conflictedIds = conflictsDoc.value(RavenJArray.class, "Conflicts").values(String.class);
        } else {
          conflictedIds = Arrays.asList("Cannot get conflict ids in HEAD requesT");
        }

        ConflictException ex = new ConflictException("Conflict detected on " + key + ", conflict must be resolved before the attachment will be accessible", true);
        ex.setConflictedVersionIds(conflictedIds.toArray(new String[0]));
        ex.setEtag(responseException.getEtag());
        throw ex;

      } catch (IOException e) {
        throw new IllegalStateException(e.getMessage(), e);
      }
    }
  }

  @Override
  @Deprecated
  public void deleteAttachment(final String key, final Etag etag) {
    executeWithReplication(HttpMethods.DELETE, new Function1<OperationMetadata, Void>() {
      @Override
      public Void apply(OperationMetadata operationMetadata) {
        directDeleteAttachment(key, etag, operationMetadata);
        return null;
      }
    });
  }

  @Deprecated
  protected void directDeleteAttachment(String key, Etag etag, OperationMetadata operationMetadata) {
    RavenJObject metadata = new RavenJObject();
    if (etag != null) {
      metadata.add(Constants.METADATA_ETAG_FIELD, RavenJToken.fromObject(etag.toString()));
    }
    try (HttpJsonRequest jsonRequest = jsonRequestFactory.createHttpJsonRequest(
      new CreateHttpJsonRequestParams(this, operationMetadata.getUrl() + "/static/" + key, HttpMethods.DELETE, metadata, operationMetadata.getCredentials(), convention))
      .addReplicationStatusHeaders(url, operationMetadata.getUrl(), replicationInformer, convention.getFailoverBehavior(), new HandleReplicationStatusChangesCallback())) {
      jsonRequest.executeRequest();
    }
  }

  @Override
  public CleanCloseable disableAllCaching() {
    return jsonRequestFactory.disableAllCaching();
  }

  @Override
  public List<String> getTerms(final String index, final String field, final String fromValue, final int pageSize) {
    return executeWithReplication(HttpMethods.GET, new Function1<OperationMetadata, List<String>>() {
      @Override
      public List<String> apply(OperationMetadata operationMetadata) {
        return directGetTerms(operationMetadata, index, field, fromValue, pageSize);
      }
    });
  }

  @SuppressWarnings("boxing")
  protected List<String> directGetTerms(OperationMetadata operationMetadata, String index, String field, String fromValue, int pageSize) {
    String requestUri = operationMetadata.getUrl() + String.format("/terms/%s?field=%s&pageSize=%d&fromValue=%s",
      UrlUtils.escapeUriString(index),
      UrlUtils.escapeDataString(field),
      pageSize,
      UrlUtils.escapeDataString(fromValue != null ? fromValue : ""));

    try (HttpJsonRequest request = jsonRequestFactory.createHttpJsonRequest(
      new CreateHttpJsonRequestParams(this, requestUri, HttpMethods.GET, new RavenJObject(), operationMetadata.getCredentials(), convention)
      .addOperationHeaders(operationsHeaders))
      .addReplicationStatusHeaders(url, operationMetadata.getUrl(), replicationInformer, convention.getFailoverBehavior(), new HandleReplicationStatusChangesCallback())) {
      return request.readResponseJson().values(String.class);
    }
  }

  @Override
  public ProfilingInformation getProfilingInformation() {
    return profilingInformation;
  }

  public void addFailoverStatusChanged(EventHandler<FailoverStatusChangedEventArgs> event) {
    replicationInformer.addFailoverStatusChanged(event);
  }

  public void removeFailoverStatusChanged(EventHandler<FailoverStatusChangedEventArgs> event) {
    replicationInformer.removeFailoverStatusChanged(event);
  }

  @Override
  public CleanCloseable forceReadFromMaster() {
    final int old = readStripingBase;
    readStripingBase = -1;
    return new CleanCloseable() {
      @Override
      public void close() {
        readStripingBase = old;
      }
    };
  }

  @Override
  public JsonDocumentMetadata head(final String key) {
    ensureIsNotNullOrEmpty(key, "key");
    return executeWithReplication(HttpMethods.HEAD, new Function1<OperationMetadata, JsonDocumentMetadata>() {
      @Override
      public JsonDocumentMetadata apply(OperationMetadata operationMetadata) {
        return directHead(operationMetadata, key);
      }
    });
  }

  @Override
  public RavenJObjectIterator streamQuery(final String index, final IndexQuery query, final Reference<QueryHeaderInformation> queryHeaderInfo) {
    return executeWithReplication(HttpMethods.GET, new Function1<OperationMetadata, RavenJObjectIterator>() {
      @Override
      public RavenJObjectIterator apply(OperationMetadata operationMetadata) {
        return directStreamQuery(operationMetadata, index, query, queryHeaderInfo);
      }
    });
  }

  @SuppressWarnings("boxing")
  public RavenJObjectIterator directStreamQuery(OperationMetadata operationMetadata, String index, IndexQuery query, Reference<QueryHeaderInformation> queryHeaderInfo) {
    ensureIsNotNullOrEmpty(index, "index");

    String path;
    HttpMethods method;

    if (query.getQuery() != null && query.getQuery().length() > convention.getMaxLengthOfQueryUsingGetUrl()) {
      path = query.getIndexQueryUrl(operationMetadata.getUrl(), index, "streams/query", false, false);
      method = HttpMethods.POST;
    } else {
      method = HttpMethods.GET;
      path = query.getIndexQueryUrl(operationMetadata.getUrl(), index, "streams/query", false);
    }

    HttpJsonRequest request = jsonRequestFactory.createHttpJsonRequest(
      new CreateHttpJsonRequestParams(this, path, method, new RavenJObject(), credentialsThatShouldBeUsedOnlyInOperationsWithoutReplication, convention)
      .addOperationHeaders(operationsHeaders))
      .addReplicationStatusHeaders(getUrl(), url, replicationInformer,
        convention.getFailoverBehavior(),
        new HandleReplicationStatusChangesCallback());
    request.removeAuthorizationHeader();

    String token = getSingleAuthToken(operationMetadata);
    try {
      token = validateThatWeCanUseAuthenticateTokens(operationMetadata, token);
    } catch (Exception e) {
      request.close();
      throw new IllegalStateException("Could not authenticate token for query streaming, if you are using ravendb in IIS make sure you have Anonymous Authentication enabled in the IIS configuration", e);
    }

    request.addOperationHeader("Single-Use-Auth-Token", token);

    CloseableHttpResponse response;

    try {
      if (HttpMethods.POST.equals(method)) {
        response = request.executeRawResponse(query.getQuery());
      } else {
        response = request.executeRawResponse();
      }
      HttpJsonRequestExtension.assertNotFailingResponse(response);
    } catch (Exception e) {
      request.close();

      if (index.startsWith("dynamic/") && request.getResponseStatusCode() == HttpStatus.SC_NOT_FOUND) {
        throw new IllegalStateException("StreamQuery does not support querying dynamic indexes. It is designed to be used with large data-sets and is unlikely to return all data-set after 15 sec of indexing, like query() does", e);
      }
      throw new IllegalStateException(e.getMessage(), e);
    }

    QueryHeaderInformation queryHeaderInformation = new QueryHeaderInformation();
    Map<String, String> headers = HttpJsonRequest.extractHeaders(response.getAllHeaders());
    queryHeaderInformation.setIndex(headers.get("Raven-Index"));
    NetDateFormat sdf = new NetDateFormat();
    try {
      queryHeaderInformation.setIndexTimestamp(sdf.parse(headers.get("Raven-Index-Timestamp")));
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }

    queryHeaderInformation.setIndexEtag(Etag.parse(headers.get("Raven-Index-Etag")));
    queryHeaderInformation.setResultEtag(Etag.parse(headers.get("Raven-Result-Etag")));
    queryHeaderInformation.setStale(Boolean.valueOf(headers.get("Raven-Is-Stale")));
    queryHeaderInformation.setTotalResults(Integer.valueOf(headers.get("Raven-Total-Results")));

    queryHeaderInfo.value = queryHeaderInformation;

    return yieldStreamResults(response);
  }

  public static RavenJObjectIterator yieldStreamResults(final CloseableHttpResponse webResponse) {
    return yieldStreamResults(webResponse, 0, 0, null, null);
  }

  public static RavenJObjectIterator yieldStreamResults(final CloseableHttpResponse webResponse, final int start,
    final int pageSize, RavenPagingInformation pagingInformation, Function1<JsonParser, Boolean> customizedEndResult) {
    return new RavenJObjectIterator(webResponse, start, pageSize, pagingInformation, customizedEndResult);
  }

  @Override
  public RavenJObjectIterator streamDocs() {
    return streamDocs(null, null, null, 0, Integer.MAX_VALUE);
  }

  @Override
  public RavenJObjectIterator streamDocs(Etag fromEtag) {
    return streamDocs(fromEtag, null, null, 0, Integer.MAX_VALUE, null);
  }

  @Override
  public RavenJObjectIterator streamDocs(Etag fromEtag, String startsWith) {
    return streamDocs(fromEtag, startsWith, null, 0, Integer.MAX_VALUE, null);
  }

  @Override
  public RavenJObjectIterator streamDocs(Etag fromEtag, String startsWith, String matches) {
    return streamDocs(fromEtag, startsWith, matches, 0, Integer.MAX_VALUE, null);
  }

  @Override
  public RavenJObjectIterator streamDocs(Etag fromEtag, String startsWith, String matches, int start) {
    return streamDocs(fromEtag, startsWith, matches, start, Integer.MAX_VALUE, null);
  }
  @Override
  public RavenJObjectIterator streamDocs(Etag fromEtag, String startsWith, String matches, int start, int pageSize) {
    return streamDocs(fromEtag, startsWith, matches, start, pageSize, null);
  }

  @Override
  public RavenJObjectIterator streamDocs(Etag fromEtag, String startsWith, String matches, int start, int pageSize, String exclude) {
    return streamDocs(fromEtag, startsWith, matches, start, pageSize, exclude, null);
  }

  @Override
  public RavenJObjectIterator streamDocs(Etag fromEtag, String startsWith, String matches, int start, int pageSize, String exclude, RavenPagingInformation pagingInformation) {
    return streamDocs(fromEtag, startsWith, matches, start, pageSize, exclude, pagingInformation, null);
  }


  @Override
  public RavenJObjectIterator streamDocs(final Etag fromEtag, final String startsWith, final String matches, final int start, final int pageSize, final String exclude, final RavenPagingInformation pagingInformation, final String skipAfter) {
    return executeWithReplication(HttpMethods.GET, new Function1<OperationMetadata, RavenJObjectIterator>() {
      @Override
      public RavenJObjectIterator apply(OperationMetadata operationMetadata) {
        return directStreamDocs(operationMetadata, fromEtag, startsWith, matches, start, pageSize, exclude, pagingInformation, skipAfter);
      }
    });
  }

  @SuppressWarnings("null")
  public RavenJObjectIterator directStreamDocs(OperationMetadata operationMetadata, final Etag fromEtag, final String startsWith, final String matches, final int start, final int pageSize, final String exclude, final RavenPagingInformation pagingInformation, final String skipAfter) {

    if (fromEtag != null && startsWith != null)
      throw new IllegalArgumentException("Either fromEtag or startsWith must be null, you can't specify both");

    StringBuilder sb = new StringBuilder(url).append("/streams/docs?");

    if (fromEtag != null) {
      sb.append("etag=")
      .append(fromEtag)
      .append("&");
    } else {
      if (startsWith != null) {
        sb.append("startsWith=").append(UrlUtils.escapeDataString(startsWith)).append("&");
      }
      if(matches != null) {
        sb.append("matches=").append(UrlUtils.escapeDataString(matches)).append("&");
      }
      if (exclude != null) {
        sb.append("exclude=").append(UrlUtils.escapeDataString(exclude)).append("&");
      }
      if (skipAfter != null) {
        sb.append("skipAfter=").append(UrlUtils.escapeDataString(skipAfter)).append("&");
      }
    }

    int actualStart = start;

    boolean nextPage = pagingInformation != null && pagingInformation.isForPreviousPage(start, pageSize);

    if (nextPage) {
      actualStart = pagingInformation.getNextPageStart();
    }

    if (actualStart != 0) {
      sb.append("start=").append(actualStart).append("&");
    }
    if (pageSize != Integer.MAX_VALUE) {
      sb.append("pageSize=").append(pageSize).append("&");
    }

    if (nextPage) {
      sb.append("next-page=true").append("&");
    }

    HttpJsonRequest request = jsonRequestFactory.createHttpJsonRequest(
      new CreateHttpJsonRequestParams(this, sb.toString(), HttpMethods.GET, new RavenJObject(), credentialsThatShouldBeUsedOnlyInOperationsWithoutReplication, convention)
      .addOperationHeaders(operationsHeaders))
      .addReplicationStatusHeaders(url, url, replicationInformer, convention.getFailoverBehavior(), new HandleReplicationStatusChangesCallback());

    request.removeAuthorizationHeader();
    String token = getSingleAuthToken(operationMetadata);

    try {
      token = validateThatWeCanUseAuthenticateTokens(operationMetadata, token);
    } catch (Exception e) {
      request.close();
      throw new IllegalStateException("Could not authenticate token for docs streaming, if you are using ravendb in IIS make sure you have Anonymous Authentication enabled in the IIS configuration", e);
    }

    request.addOperationHeader("Single-Use-Auth-Token", token);

    CloseableHttpResponse response = null;

    try {
      response = request.executeRawResponse();
      HttpJsonRequestExtension.assertNotFailingResponse(response);
    } catch (Exception e) {
      request.close();
      throw new IllegalStateException(e.getMessage(), e);
    }
    return yieldStreamResults(response, start, pageSize, pagingInformation, null);

  }

  @Override
  public void delete(final String key, final Etag etag) {
    ensureIsNotNullOrEmpty(key, "key");
    executeWithReplication(HttpMethods.DELETE, new Function1<OperationMetadata, Void>() {
      @Override
      public Void apply(OperationMetadata operationMetadata) {
        directDelete(operationMetadata, key, etag);
        return null;
      }
    });
  }

  protected void directDelete(OperationMetadata operationMetadata, String key, Etag etag) {
    ensureIsNotNullOrEmpty(key, "key");
    try (HttpJsonRequest jsonRequest = jsonRequestFactory.createHttpJsonRequest(
      new CreateHttpJsonRequestParams(this, operationMetadata.getUrl() + "/docs/" + UrlUtils.escapeDataString(key), HttpMethods.DELETE, new RavenJObject(), operationMetadata.getCredentials(), convention).addOperationHeaders(operationsHeaders))
      .addReplicationStatusHeaders(url, operationMetadata.getUrl(), replicationInformer, convention.getFailoverBehavior(), new HandleReplicationStatusChangesCallback())) {
      if (etag != null) {
        jsonRequest.addOperationHeader("If-None-Match", etag.toString());
      }
      try {
        jsonRequest.executeRequest();
      } catch (ErrorResponseException e) {
        if (e.getStatusCode() != HttpStatus.SC_CONFLICT) {
          throw e;
        }
        throw fetchConcurrencyException(e);
      }
    }
  }

  @Override
  public String urlFor(String documentKey) {
    return url + "/docs/" + documentKey;
  }

  @Override
  public ILowLevelBulkInsertOperation getBulkInsertOperation(BulkInsertOptions options, IDatabaseChanges changes) {
    return new RemoteBulkInsertOperation(options, this, changes);
  }

  protected JsonDocumentMetadata directHead(OperationMetadata operationMetadata, String key) {
    try (HttpJsonRequest jsonRequest = jsonRequestFactory.createHttpJsonRequest(
      new CreateHttpJsonRequestParams(ServerClient.this, operationMetadata.getUrl() + "/docs/" + key, HttpMethods.HEAD, new RavenJObject(), operationMetadata.getCredentials(), convention)
      .addOperationHeaders(operationsHeaders))
      .addReplicationStatusHeaders(url, operationMetadata.getUrl(), replicationInformer, convention.getFailoverBehavior(), new HandleReplicationStatusChangesCallback())) {

      try {
        jsonRequest.executeRequest();
        return SerializationHelper.deserializeJsonDocumentMetadata(key, jsonRequest.getResponseHeaders(), jsonRequest.getResponseStatusCode());
      } catch (ErrorResponseException e) {
        if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) return null;
        if (e.getStatusCode() == HttpStatus.SC_CONFLICT) {
          ConflictException conflictException = new ConflictException("Conflict detected on " + key +
            ", conflict must be resolved before the document will be accessible. Cannot get the conflicts ids because" +
            " a HEAD request was performed. A GET request will provide more information, and if you have a document conflict listener, will automatically resolve the conflict", true);
          conflictException.setEtag(e.getEtag());

          throw conflictException;
        }
        throw e;
      }
    }
  }

  public RavenJToken executeGetRequest(final String requestUrl) {
    ensureIsNotNullOrEmpty(requestUrl, "url");
    return executeWithReplication(HttpMethods.GET, new Function1<OperationMetadata, RavenJToken>() {
      @Override
      public RavenJToken apply(OperationMetadata operationMetadata) {
        RavenJObject metadata = new RavenJObject();
        try (HttpJsonRequest jsonRequest = jsonRequestFactory.createHttpJsonRequest(
          new CreateHttpJsonRequestParams(ServerClient.this, operationMetadata.getUrl() + requestUrl, HttpMethods.GET, metadata, operationMetadata.getCredentials(), convention).
          addOperationHeaders(operationsHeaders))) {
          return jsonRequest.readResponseJson();
        }
      }
    });
  }

  @Override
  public HttpJsonRequest createRequest(HttpMethods method, String requestUrl) {
    return createRequest(method, requestUrl, false, false, null);
  }

  public HttpJsonRequest createRequest(HttpMethods method, String requestUrl, boolean disableRequestCompression,
    boolean disableAuthentication, Long timeout) {
    RavenJObject metadata = new RavenJObject();
    CreateHttpJsonRequestParams createHttpJsonRequestParams = new CreateHttpJsonRequestParams(this, url + requestUrl, method, metadata, credentialsThatShouldBeUsedOnlyInOperationsWithoutReplication, convention, timeout)
    .addOperationHeaders(operationsHeaders);
    createHttpJsonRequestParams.setDisableRequestCompression(disableRequestCompression);
    createHttpJsonRequestParams.setDisableAuthentication(disableAuthentication);
    return jsonRequestFactory.createHttpJsonRequest(createHttpJsonRequestParams);
  }

  public HttpJsonRequest createRequest(OperationMetadata operationMetadata, HttpMethods method, String requestUrl, boolean disableRequestCompression,
    boolean disableAuthentication, Long timeout) {
    RavenJObject metadata = new RavenJObject();
    CreateHttpJsonRequestParams createHttpJsonRequestParams = new CreateHttpJsonRequestParams(this, operationMetadata.getUrl() + requestUrl, method, metadata, credentialsThatShouldBeUsedOnlyInOperationsWithoutReplication, convention, timeout)
    .addOperationHeaders(operationsHeaders);
    createHttpJsonRequestParams.setDisableRequestCompression(disableRequestCompression);
    createHttpJsonRequestParams.setDisableAuthentication(disableAuthentication);
    return jsonRequestFactory.createHttpJsonRequest(createHttpJsonRequestParams);
  }

  public HttpJsonRequest createReplicationAwareRequest(String currentServerUrl, String requestUrl, HttpMethods method) {
    return createReplicationAwareRequest(currentServerUrl, requestUrl, method, false);
  }

  public HttpJsonRequest createReplicationAwareRequest(String currentServerUrl, String requestUrl, HttpMethods method, boolean disableRequestCompression) {
    RavenJObject metadata = new RavenJObject();

    CreateHttpJsonRequestParams createHttpJsonRequestParams =
      new CreateHttpJsonRequestParams(this, url + requestUrl, method, metadata, credentialsThatShouldBeUsedOnlyInOperationsWithoutReplication, convention)
    .addOperationHeaders(operationsHeaders);
    createHttpJsonRequestParams.setDisableRequestCompression(disableRequestCompression);
    return jsonRequestFactory.createHttpJsonRequest(createHttpJsonRequestParams).addReplicationStatusHeaders(url, currentServerUrl, replicationInformer, convention.getFailoverBehavior(), new HandleReplicationStatusChangesCallback());
  }

  @Override
  @Deprecated
  public void updateAttachmentMetadata(final String key, final Etag etag, final RavenJObject metadata) {
    executeWithReplication(HttpMethods.POST,new Function1<OperationMetadata, Void>() {
      @Override
      public Void apply(OperationMetadata operationMetadata) {
        directUpdateAttachmentMetadata(key, metadata, etag, operationMetadata);
        return null;
      }
    });
  }

  @Deprecated
  protected void directUpdateAttachmentMetadata(String key, RavenJObject metadata, Etag etag, OperationMetadata operationMetadata) {
    if (etag != null) {
      metadata.set(Constants.METADATA_ETAG_FIELD, new RavenJValue(etag.toString()));
    }

    try (HttpJsonRequest jsonRequest = jsonRequestFactory.createHttpJsonRequest(
      new CreateHttpJsonRequestParams(this, operationMetadata.getUrl() + "/static/" + key, HttpMethods.POST, metadata, operationMetadata.getCredentials(), convention))
      .addReplicationStatusHeaders(url, operationMetadata.getUrl(), replicationInformer, convention.getFailoverBehavior(), new HandleReplicationStatusChangesCallback())) {
      ErrorResponseException responseException;
      try {
        jsonRequest.executeRequest();
        return;
      } catch (ErrorResponseException e) {
        responseException = e;
      }
      if (!handleException(responseException)) throw responseException;
    }
  }

  @Override
  @Deprecated
  public List<Attachment> getAttachmentHeadersStartingWith(final String idPrefix, final int start, final int pageSize) {
    return executeWithReplication(HttpMethods.GET, new Function1<OperationMetadata, List<Attachment>>() {
      @Override
      public List<Attachment> apply(OperationMetadata operationMetadata) {
        return directGetAttachmentHeadersStartingWith(HttpMethods.GET, idPrefix, start, pageSize, operationMetadata);
      }
    });
  }

  @Deprecated
  protected List<Attachment> directGetAttachmentHeadersStartingWith(HttpMethods method, String idPrefix, int start, int pageSize, OperationMetadata operationMetadata) {
    try (HttpJsonRequest jsonRequest = jsonRequestFactory.createHttpJsonRequest(
      new CreateHttpJsonRequestParams(this, operationMetadata.getUrl() + "/static/?startsWith=" + idPrefix + "&start=" + start + "&pageSize=" + pageSize, method, new RavenJObject(), operationMetadata.getCredentials(),
        convention))
        .addReplicationStatusHeaders(url, operationMetadata.getUrl(), replicationInformer, convention.getFailoverBehavior(), new HandleReplicationStatusChangesCallback())) {
      RavenJToken responseJson = jsonRequest.readResponseJson();
      // this method not-exists in .net version
      return SerializationHelper.deserializeAttachements(responseJson, false);
    }
  }

  @SuppressWarnings("boxing")
  protected void handleReplicationStatusChanges(Map<String, String> headers, String primaryUrl, String currentUrl) {
    if (!primaryUrl.equalsIgnoreCase(currentUrl)) {
      String forceCheck = headers.get(Constants.RAVEN_FORCE_PRIMARY_SERVER_CHECK);
      boolean shouldForceCheck;
      if (StringUtils.isNotEmpty(forceCheck)) {
        shouldForceCheck = Boolean.valueOf(forceCheck);
        replicationInformer.forceCheck(primaryUrl, shouldForceCheck);
      }
    }
  }

  public <S> S executeWithReplication(HttpMethods method, Function1<OperationMetadata, S> operation) {
    int currentRequest = ++requestCount;
    return replicationInformer.executeWithReplication(method, url, credentialsThatShouldBeUsedOnlyInOperationsWithoutReplication, currentRequest, readStripingBase, operation);
  }

  @SuppressWarnings("boxing")
  private boolean assertNonConflictedDocumentAndCheckIfNeedToReload(OperationMetadata operationMetadata,RavenJObject docResult, Function1<String, ConflictException> onClictedQueryResult) {
    if (docResult == null) {
      return false;
    }
    RavenJToken metadata = docResult.get(Constants.METADATA);
    if (metadata == null) {
      return false;
    }

    if (metadata.value(Integer.TYPE, "@Http-Status-Code") == 409) {
      ConflictException concurrencyException = tryResolveConflictOrCreateConcurrencyException(operationMetadata, metadata.value(String.class, "@id"), docResult, HttpExtensions.etagHeaderToEtag(metadata.value(String.class, "@etag")));
      if (concurrencyException == null) {
        return true;
      }
      throw concurrencyException;
    }

    if (metadata.value(Boolean.TYPE, Constants.RAVEN_REPLICATION_CONFLICT) && onClictedQueryResult != null) {
      throw onClictedQueryResult.apply(metadata.value(String.class, "@id"));
    }

    return false;
  }

  @SuppressWarnings("boxing")
  private ConflictException tryResolveConflictOrCreateConcurrencyException(OperationMetadata operationMetadata, String key, RavenJObject conflictsDoc, Etag etag) {
    RavenJArray ravenJArray = conflictsDoc.value(RavenJArray.class, "Conflicts");
    if (ravenJArray == null) {
      throw new IllegalArgumentException("Could not get conflict ids from conflicted document, are you trying to resolve a conflict when using metadata-only?");
    }

    List<String> conflictIds = new ArrayList<>();
    for (RavenJToken token: ravenJArray) {
      conflictIds.add(token.value(String.class));
    }

    boolean result = tryResolveConflictByUsingRegisteredListeners(operationMetadata, key, etag, conflictIds, operationMetadata);
    if (result)
      return null;

    ConflictException conflictException = new ConflictException("Conflict detected on " + key +
      ", conflict must be resolved before the document will be accessible", true);
    conflictException.setConflictedVersionIds(conflictIds.toArray(new String[0]));
    conflictException.setEtag(etag);
    return conflictException;
  }

  @SuppressWarnings("boxing")
  @Override
  public Boolean tryResolveConflictByUsingRegisteredListeners(OperationMetadata operationMetadata, String key,
    Etag etag, List<String> conflictedIds,
    OperationMetadata opUrl) {

    if (operationMetadata == null) {
      operationMetadata = new OperationMetadata(url);
    }

    if (conflictListeners.length > 0 && resolvingConflict == false) {
      resolvingConflict = true;
      try {
        MultiLoadResult multiLoadResult = get(conflictedIds.toArray(new String[0]), null);

        List<JsonDocument> results = new ArrayList<>();
        for (RavenJObject r: multiLoadResult.getResults()) {
          results.add(SerializationHelper.toJsonDocument(r));
        }

        for(IDocumentConflictListener conflictListener: conflictListeners) {
          Reference<JsonDocument> resolvedDocument = new Reference<>();
          if (conflictListener.tryResolveConflict(key, results, resolvedDocument)) {
            put(key, etag, resolvedDocument.value.getDataAsJson(), resolvedDocument.value.getMetadata());
            return true;
          }
        }
      }
      finally {
        resolvingConflict = false;
      }
    }
    return false;
  }

  private <T> T retryOperationBecauseOfConflict(OperationMetadata operationMetadata,
    List<RavenJObject> docResults, T currentResult, Function0<T> nextTry, Function1<String, ConflictException> onConflictedQueryResult) {

    boolean requiresRetry = false;
    for (RavenJObject docResult: docResults) {
      requiresRetry |= assertNonConflictedDocumentAndCheckIfNeedToReload(operationMetadata, docResult, onConflictedQueryResult);
    }
    if (!requiresRetry) {
      return currentResult;
    }

    if (resolvingConflictRetries) {
      throw new IllegalStateException("Encountered another conflict after already resolving a conflict. Conflict resultion cannot recurse.");
    }
    resolvingConflictRetries = true;
    try {
      return nextTry.apply();
    } finally {
      resolvingConflictRetries = false;
    }
  }

  public RavenJToken getOperationStatus(long id) {
    try (HttpJsonRequest request = jsonRequestFactory.createHttpJsonRequest(
      new CreateHttpJsonRequestParams(this, url + "/operation/status?id=" + id, HttpMethods.GET, new RavenJObject(), credentialsThatShouldBeUsedOnlyInOperationsWithoutReplication, convention)
      .addOperationHeaders(operationsHeaders))) {
      return request.readResponseJson();
    } catch (ErrorResponseException e) {
      if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) return null;
      throw e;
    }
  }

  public String getSingleAuthToken(OperationMetadata operationMetadata) {
    try (HttpJsonRequest tokenRequest = createRequest(operationMetadata, HttpMethods.GET, "/singleAuthToken", true, true, null)) {
      return tokenRequest.readResponseJson().value(String.class, "Token");
    }
  }

  private String validateThatWeCanUseAuthenticateTokens(OperationMetadata operationMetadata, String token) {
    try (HttpJsonRequest request = createRequest(operationMetadata, HttpMethods.GET, "/singleAuthToken", true, true, null)) {
      request.removeAuthorizationHeader();
      request.addOperationHeader("Single-Use-Auth-Token", token);
      RavenJToken result = request.readResponseJson();
      return result.value(String.class, "Token");
    }
  }

  public boolean isInFailoverMode() {
    return replicationInformer.getFailureCount(url).longValue() > 0;
  }

  public class HandleReplicationStatusChangesCallback implements Action3<Map<String, String>, String, String> {
    @Override
    public void apply(Map<String, String> headers, String primaryUrl, String currentUrl) {
      handleReplicationStatusChanges(headers, primaryUrl, currentUrl);
    }
  }

  @Override
  public DatabaseStatistics getStatistics() {
    try (HttpJsonRequest httpJsonRequest = jsonRequestFactory.createHttpJsonRequest(
      new CreateHttpJsonRequestParams(this,  url + "/stats", HttpMethods.GET, new RavenJObject(), credentialsThatShouldBeUsedOnlyInOperationsWithoutReplication, convention))) {

      RavenJObject jo = (RavenJObject)httpJsonRequest.readResponseJson();
      return convention.createSerializer().deserialize(jo, DatabaseStatistics.class);
    }
  }

  @Override
  public boolean isExpect100Continue() {
    return expect100Continue;
  }

  public void setExpect100Continue(boolean expect100Continue) {
    this.expect100Continue = expect100Continue;
  }

  public ReplicationDocument directGetReplicationDestinations(OperationMetadata operationMetadata) {
    CreateHttpJsonRequestParams createHttpJsonRequestParams = new CreateHttpJsonRequestParams(this, operationMetadata.getUrl() + "/replication/topology", HttpMethods.GET, null, operationMetadata.getCredentials(), convention);
    try (HttpJsonRequest request = jsonRequestFactory.createHttpJsonRequest(createHttpJsonRequestParams
      .addOperationHeaders(getOperationsHeaders())).addReplicationStatusHeaders(url, operationMetadata.getUrl(), replicationInformer, convention.getFailoverBehavior(), new HandleReplicationStatusChangesCallback())) {
      try {
        RavenJToken requestJson = request.readResponseJson();
        return convention.createSerializer().deserialize(requestJson, ReplicationDocument.class);
      } catch (ErrorResponseException e) {
        if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND || e.getStatusCode() == HttpStatus.SC_BAD_REQUEST) {
          return null;
        }
        throw e;
      }
    }
  }

  public HttpJsonRequestFactory getJsonRequestFactory() {
    return jsonRequestFactory;
  }

}
