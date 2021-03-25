package net.ravendb.client.documents.session;

import com.google.common.base.Defaults;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.commands.GetDocumentsCommand;
import net.ravendb.client.documents.commands.GetDocumentsResult;
import net.ravendb.client.documents.commands.HeadDocumentCommand;
import net.ravendb.client.documents.commands.batches.SingleNodeBatchCommand;
import net.ravendb.client.documents.linq.IDocumentQueryGenerator;
import net.ravendb.client.documents.queries.Query;
import net.ravendb.client.documents.session.loaders.IIncludeBuilder;
import net.ravendb.client.documents.session.loaders.ILoaderWithInclude;
import net.ravendb.client.documents.session.loaders.IncludeBuilder;
import net.ravendb.client.documents.session.loaders.MultiLoaderWithInclude;
import net.ravendb.client.documents.session.operations.BatchOperation;
import net.ravendb.client.documents.session.operations.LoadOperation;
import net.ravendb.client.documents.session.operations.LoadStartingWithOperation;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.primitives.Tuple;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class DocumentSession extends InMemoryDocumentSessionOperations
        implements IAdvancedSessionOperations, IDocumentSessionImpl, IDocumentQueryGenerator {

    /**
     * Get the accessor for advanced operations
     *
     * Note: Those operations are rarely needed, and have been moved to a separate
     * property to avoid cluttering the API
     */
    @Override
    public IAdvancedSessionOperations advanced() {
        return this;
    }




    /**
     * Initializes new DocumentSession
     * @param documentStore Parent document store
     * @param id Identifier
     * @param options SessionOptions
     */
    public DocumentSession(DocumentStore documentStore, UUID id, SessionOptions options) {
        super(documentStore, id, options);
    }

    /**
     * Saves all the changes to the Raven server.
     */
    @Override
    public void saveChanges() {
        BatchOperation saveChangeOperation = new BatchOperation(this);

        try (SingleNodeBatchCommand command = saveChangeOperation.createRequest()) {
            if (command == null) {
                return;
            }

            if (noTracking) {
                throw new IllegalStateException("Cannot execute saveChanges when entity tracking is disabled in session.");
            }

            _requestExecutor.execute(command, sessionInfo);
            updateSessionAfterSaveChanges(command.getResult());
            saveChangeOperation.setResult(command.getResult());
        }
    }

    /**
     * Check if document exists without loading it
     */
    public boolean exists(String id) {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }

        if (_knownMissingIds.contains(id)) {
            return false;
        }

        if (documentsById.getValue(id) != null) {
            return true;
        }

        HeadDocumentCommand command = new HeadDocumentCommand(id, null);

        _requestExecutor.execute(command, sessionInfo);

        return command.getResult() != null;
    }

    /**
     * Refreshes the specified entity from Raven server.
     */
    public <T> void refresh(T entity) {
        DocumentInfo documentInfo = documentsByEntity.get(entity);
        if (documentInfo == null) {
            throw new IllegalStateException("Cannot refresh a transient instance");
        }

        incrementRequestCount();

        GetDocumentsCommand command = new GetDocumentsCommand(new String[]{documentInfo.getId()}, null, false);
        _requestExecutor.execute(command, sessionInfo);

        refreshInternal(entity, command, documentInfo);
    }

    /**
     * Generates the document ID.
     */
    @Override
    protected String generateId(Object entity) {
        return getConventions().generateDocumentId(getDatabaseName(), entity);
    }




    /**
     * Begin a load while including the specified path
     */
    public ILoaderWithInclude include(String path) {
        return new MultiLoaderWithInclude(this).include(path);
    }

    @Override
    public <T> T load(Class<T> clazz, String id) {
        if (id == null) {
            return Defaults.defaultValue(clazz);
        }

        LoadOperation loadOperation = new LoadOperation(this);

        loadOperation.byId(id);

        GetDocumentsCommand command = loadOperation.createRequest();

        if (command != null) {
            _requestExecutor.execute(command, sessionInfo);
            loadOperation.setResult(command.getResult());
        }

        return loadOperation.getDocument(clazz);
    }

    public <T> Map<String, T> load(Class<T> clazz, String... ids) {
        if (ids == null) {
            throw new IllegalArgumentException("Ids cannot be null");
        }
        LoadOperation loadOperation = new LoadOperation(this);
        loadInternal(ids, loadOperation, null);
        return loadOperation.getDocuments(clazz);
    }

    /**
     * Loads the specified entities with the specified ids.
     */
    public <T> Map<String, T> load(Class<T> clazz, Collection<String> ids) {
        LoadOperation loadOperation = new LoadOperation(this);
        loadInternal(ids.toArray(new String[0]), loadOperation, null);
        return loadOperation.getDocuments(clazz);
    }

    private <T> void loadInternal(String[] ids, LoadOperation operation, OutputStream stream) {
        operation.byIds(ids);

        GetDocumentsCommand command = operation.createRequest();
        if (command != null) {
            _requestExecutor.execute(command, sessionInfo);

            if (stream != null) {
                try {
                    GetDocumentsResult result = command.getResult();
                    JsonExtensions.getDefaultMapper().writeValue(stream, result);
                } catch (IOException e) {
                    throw new RuntimeException("Unable to serialize returned value into stream" + e.getMessage(), e);
                }
            } else {
                operation.setResult(command.getResult());
            }
        }
    }

    @Override
    public <T> T load(Class<T> clazz, String id, Consumer<IIncludeBuilder> includes) {
        if (id == null) {
            return null;
        }

        Collection<T> values = load(clazz, Collections.singletonList(id), includes).values();
        return values.isEmpty() ? null : values.iterator().next();
    }

    @Override
    public <TResult> Map<String, TResult> load(Class<TResult> clazz, Collection<String> ids, Consumer<IIncludeBuilder> includes) {
        if (ids == null) {
            throw new IllegalArgumentException("ids cannot be null");
        }

        if (includes == null) {
            return load(clazz, ids);
        }

        IncludeBuilder includeBuilder = new IncludeBuilder(getConventions());
        includes.accept(includeBuilder);



        return loadInternal(clazz,
                ids.toArray(new String[0]),
                includeBuilder.documentsToInclude != null ? includeBuilder.documentsToInclude.toArray(new String[0]) : null);
    }

    public <TResult> Map<String, TResult> loadInternal(Class<TResult> clazz, String[] ids, String[] includes) {

        if (ids == null) {
            throw new IllegalArgumentException("Ids cannot be null");
        }

        LoadOperation loadOperation = new LoadOperation(this);
        loadOperation.byIds(ids);
        loadOperation.withIncludes(includes);


        GetDocumentsCommand command = loadOperation.createRequest();
        if (command != null) {
            _requestExecutor.execute(command, sessionInfo);
            loadOperation.setResult(command.getResult());
        }

        return loadOperation.getDocuments(clazz);
    }

    public <T> T[] loadStartingWith(Class<T> clazz, String idPrefix) {
        return loadStartingWith(clazz, idPrefix, null, 0, 25, null, null);
    }

    public <T> T[] loadStartingWith(Class<T> clazz, String idPrefix, String matches) {
        return loadStartingWith(clazz, idPrefix, matches, 0, 25, null, null);
    }

    public <T> T[] loadStartingWith(Class<T> clazz, String idPrefix, String matches, int start) {
        return loadStartingWith(clazz, idPrefix, matches, start, 25, null, null);
    }

    public <T> T[] loadStartingWith(Class<T> clazz, String idPrefix, String matches, int start, int pageSize) {
        return loadStartingWith(clazz, idPrefix, matches, start, pageSize, null, null);
    }

    public <T> T[] loadStartingWith(Class<T> clazz, String idPrefix, String matches, int start, int pageSize, String exclude) {
        return loadStartingWith(clazz, idPrefix, matches, start, pageSize, exclude, null);
    }

    public <T> T[] loadStartingWith(Class<T> clazz, String idPrefix, String matches, int start, int pageSize, String exclude, String startAfter) {
        LoadStartingWithOperation loadStartingWithOperation = new LoadStartingWithOperation(this);
        loadStartingWithInternal(idPrefix, loadStartingWithOperation, null, matches, start, pageSize, exclude, startAfter);
        return loadStartingWithOperation.getDocuments(clazz);
    }



    @SuppressWarnings("UnusedReturnValue")
    private GetDocumentsCommand loadStartingWithInternal(String idPrefix, LoadStartingWithOperation operation, OutputStream stream,
                                                         String matches, int start, int pageSize, String exclude, String startAfter) {
        operation.withStartWith(idPrefix, matches, start, pageSize, exclude, startAfter);

        GetDocumentsCommand command = operation.createRequest();
        if (command != null) {
            _requestExecutor.execute(command, sessionInfo);

            if (stream != null) {
                try {
                    GetDocumentsResult result = command.getResult();
                    JsonExtensions.getDefaultMapper().writeValue(stream, result);
                } catch (IOException e) {
                    throw new RuntimeException("Unable to serialize returned value into stream" + e.getMessage(), e);
                }
            } else {
                operation.setResult(command.getResult());
            }
        }
        return command;
    }


    /**
     * Query the specified index using Lucene syntax
     * @param clazz The result of the query
     */
    public <T> IDocumentQuery<T> documentQuery(Class<T> clazz) {
        return documentQuery(clazz, null, null, false);
    }

    /**
     * Query the specified index using Lucene syntax
     * @param clazz The result of the query
     * @param indexName Name of the index (mutually exclusive with collectionName)
     * @param collectionName Name of the collection (mutually exclusive with indexName)
     * @param isMapReduce Whether we are querying a map/reduce index (modify how we treat identifier properties)
     */
    public <T> IDocumentQuery<T> documentQuery(Class<T> clazz, String indexName, String collectionName, boolean isMapReduce) {
        Tuple<String, String> indexNameAndCollection = processQueryParameters(clazz, indexName, collectionName, getConventions());
        indexName = indexNameAndCollection.first;
        collectionName = indexNameAndCollection.second;

        return new DocumentQuery<>(clazz, this, indexName, collectionName, isMapReduce);
    }

    @Override
    public InMemoryDocumentSessionOperations getSession() {
        return this;
    }

    @Override
    public <T> IDocumentQuery<T> query(Class<T> clazz) {
        return documentQuery(clazz, null, null, false);
    }

    @Override
    public <T> IDocumentQuery<T> query(Class<T> clazz, Query collectionOrIndexName) {
        if (StringUtils.isNotEmpty(collectionOrIndexName.getCollection())) {
            return documentQuery(clazz, null, collectionOrIndexName.getCollection(), false);
        }

        return documentQuery(clazz, collectionOrIndexName.getIndexName(), null, false);
    }

}
