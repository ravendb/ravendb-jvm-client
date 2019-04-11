package net.ravendb.client.documents.session;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Defaults;
import com.google.common.collect.Lists;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.DocumentStoreBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.IdTypeAndName;
import net.ravendb.client.documents.commands.GetDocumentsResult;
import net.ravendb.client.documents.commands.batches.*;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.identity.GenerateEntityIdOnTheClient;
import net.ravendb.client.documents.operations.OperationExecutor;
import net.ravendb.client.documents.operations.SessionOperationExecutor;
import net.ravendb.client.documents.session.operations.lazy.ILazyOperation;
import net.ravendb.client.exceptions.documents.session.NonUniqueObjectException;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.http.CurrentIndexAndNode;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.BatchCommandResult;
import net.ravendb.client.json.JsonOperation;
import net.ravendb.client.json.MetadataAsDictionary;
import net.ravendb.client.primitives.*;
import net.ravendb.client.util.IdentityHashSet;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SuppressWarnings("SameParameterValue")
public abstract class InMemoryDocumentSessionOperations implements CleanCloseable {

    private static final AtomicInteger _clientSessionIdCounter = new AtomicInteger();

    protected final int _clientSessionId = _clientSessionIdCounter.incrementAndGet();

    protected final RequestExecutor _requestExecutor;

    private OperationExecutor _operationExecutor;

    protected final List<ILazyOperation> pendingLazyOperations = new ArrayList<>();
    protected final Map<ILazyOperation, Consumer<Object>> onEvaluateLazy = new HashMap<>();

    private static final AtomicInteger _instancesCounter = new AtomicInteger();

    private final int _hash = _instancesCounter.incrementAndGet();
    protected final boolean generateDocumentKeysOnStore = true;
    protected final SessionInfo sessionInfo;
    BatchOptions _saveChangesOptions;

    private TransactionMode transactionMode;

    private boolean _isDisposed;

    protected final ObjectMapper mapper = JsonExtensions.getDefaultMapper();

    private final UUID id;

    /**
     * The session id
     *
     * @return session id
     */
    public UUID getId() {
        return id;
    }

    /**
     * The entities waiting to be deleted
     */
    public final Set<Object> deletedEntities = new IdentityHashSet<>();

    private final List<EventHandler<BeforeStoreEventArgs>> onBeforeStore = new ArrayList<>();
    private final List<EventHandler<AfterSaveChangesEventArgs>> onAfterSaveChanges = new ArrayList<>();
    private final List<EventHandler<BeforeDeleteEventArgs>> onBeforeDelete = new ArrayList<>();
    private final List<EventHandler<BeforeQueryEventArgs>> onBeforeQuery = new ArrayList<>();

    public void addBeforeStoreListener(EventHandler<BeforeStoreEventArgs> handler) {
        this.onBeforeStore.add(handler);
    }

    public void removeBeforeStoreListener(EventHandler<BeforeStoreEventArgs> handler) {
        this.onBeforeStore.remove(handler);
    }

    public void addAfterSaveChangesListener(EventHandler<AfterSaveChangesEventArgs> handler) {
        this.onAfterSaveChanges.add(handler);
    }

    public void removeAfterSaveChangesListener(EventHandler<AfterSaveChangesEventArgs> handler) {
        this.onAfterSaveChanges.remove(handler);
    }

    public void addBeforeDeleteListener(EventHandler<BeforeDeleteEventArgs> handler) {
        this.onBeforeDelete.add(handler);
    }

    public void removeBeforeDeleteListener(EventHandler<BeforeDeleteEventArgs> handler) {
        this.onBeforeDelete.remove(handler);
    }

    public void addBeforeQueryListener(EventHandler<BeforeQueryEventArgs> handler) {
        this.onBeforeQuery.add(handler);
    }

    public void removeBeforeQueryListener(EventHandler<BeforeQueryEventArgs> handler) {
        this.onBeforeQuery.remove(handler);
    }

    //Entities whose id we already know do not exists, because they are a missing include, or a missing load, etc.
    protected final Set<String> _knownMissingIds = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    private Map<String, Object> externalState;

    public Map<String, Object> getExternalState() {
        if (externalState == null) {
            externalState = new HashMap<>();
        }
        return externalState;
    }

    public ServerNode getCurrentSessionNode() {
        CurrentIndexAndNode result;
        switch (_documentStore.getConventions().getReadBalanceBehavior()) {
            case NONE:
                result = _requestExecutor.getPreferredNode();
                break;
            case ROUND_ROBIN:
                result = _requestExecutor.getNodeBySessionId(_clientSessionId);
                break;
            case FASTEST_NODE:
                result = _requestExecutor.getFastestNode();
                break;
            default:
                throw new IllegalArgumentException(_documentStore.getConventions().getReadBalanceBehavior().toString());
        }

        return result.currentNode;
    }

    /**
     * Translate between an ID and its associated entity
     */
    public final DocumentsById documentsById = new DocumentsById();

    /**
     * Translate between an ID and its associated entity
     */
    public final Map<String, DocumentInfo> includedDocumentsById = new TreeMap<>(String::compareToIgnoreCase);

    /**
     * hold the data required to manage the data for RavenDB's Unit of Work
     */
    public final Map<Object, DocumentInfo> documentsByEntity = new LinkedHashMap<>();

    /**
     * @return map which holds the data required to manage Counters tracking for RavenDB's Unit of Work
     */
    public Map<String, Tuple<Boolean, Map<String, Long>>> getCountersByDocId() {
        if (_countersByDocId == null) {
            _countersByDocId = new TreeMap<>(String::compareToIgnoreCase);
        }
        return _countersByDocId;
    }

    private Map<String, Tuple<Boolean, Map<String, Long>>> _countersByDocId;

    protected final DocumentStoreBase _documentStore;

    private final String databaseName;

    public String getDatabaseName() {
        return databaseName;
    }

    /**
     * The document store associated with this session
     *
     * @return Document store
     */
    public IDocumentStore getDocumentStore() {
        return _documentStore;
    }

    public RequestExecutor getRequestExecutor() {
        return _requestExecutor;
    }

    public OperationExecutor getOperations() {
        if (_operationExecutor == null) {
            _operationExecutor = new SessionOperationExecutor(this);
        }

        return _operationExecutor;
    }

    private int numberOfRequests;

    public int getNumberOfRequests() {
        return numberOfRequests;
    }

    /**
     * Gets the number of entities held in memory to manage Unit of Work
     *
     * @return number of entities held in memory
     */
    public int getNumberOfEntitiesInUnitOfWork() {
        return documentsByEntity.size();
    }

    /**
     * Gets the store identifier for this session.
     * The store identifier is the identifier for the particular RavenDB instance.
     *
     * @return store identifier
     */
    public String storeIdentifier() {
        return _documentStore.getIdentifier() + ";" + databaseName;
    }

    /**
     * Gets the conventions used by this session
     * This instance is shared among all sessions, changes to the DocumentConventions should be done
     * via the IDocumentSTore instance, not on a single session.
     *
     * @return document conventions
     */
    public DocumentConventions getConventions() {
        return _requestExecutor.getConventions();
    }

    private int maxNumberOfRequestsPerSession;

    /**
     * Gets the max number of requests per session.
     * If the NumberOfRequests rise above MaxNumberOfRequestsPerSession, an exception will be thrown.
     *
     * @return maximum number of requests per session
     */
    public int getMaxNumberOfRequestsPerSession() {
        return maxNumberOfRequestsPerSession;
    }

    /**
     * Sets the max number of requests per session.
     * If the NumberOfRequests rise above MaxNumberOfRequestsPerSession, an exception will be thrown.
     *
     * @param maxNumberOfRequestsPerSession sets the value
     */
    public void setMaxNumberOfRequestsPerSession(int maxNumberOfRequestsPerSession) {
        this.maxNumberOfRequestsPerSession = maxNumberOfRequestsPerSession;
    }

    private boolean useOptimisticConcurrency;

    /**
     * Gets value indicating whether the session should use optimistic concurrency.
     * When set to true, a check is made so that a change made behind the session back would fail
     * and raise ConcurrencyException
     *
     * @return true if optimistic concurrency should be used
     */
    public boolean isUseOptimisticConcurrency() {
        return useOptimisticConcurrency;
    }

    /**
     * Sets value indicating whether the session should use optimistic concurrency.
     * When set to true, a check is made so that a change made behind the session back would fail
     * and raise ConcurrencyException
     *
     * @param useOptimisticConcurrency sets the value
     */
    public void setUseOptimisticConcurrency(boolean useOptimisticConcurrency) {
        this.useOptimisticConcurrency = useOptimisticConcurrency;
    }

    protected final List<ICommandData> deferredCommands = new ArrayList<>();

    final Map<IdTypeAndName, ICommandData> deferredCommandsMap = new HashMap<>();

    public final boolean noTracking;

    public int getDeferredCommandsCount() {
        return deferredCommands.size();
    }

    private final GenerateEntityIdOnTheClient generateEntityIdOnTheClient;

    public GenerateEntityIdOnTheClient getGenerateEntityIdOnTheClient() {
        return generateEntityIdOnTheClient;
    }

    private final EntityToJson entityToJson;

    public EntityToJson getEntityToJson() {
        return entityToJson;
    }

    /**
     * Initializes a new instance of the InMemoryDocumentSessionOperations class.
     *
     * @param documentStore   Document store
     * @param id              Identifier
     * @param options         Session options
     */
    protected InMemoryDocumentSessionOperations(DocumentStoreBase documentStore, UUID id, SessionOptions options) {
        this.id = id;
        this.databaseName = ObjectUtils.firstNonNull(options.getDatabase(), documentStore.getDatabase());

        if (StringUtils.isBlank(databaseName)) {
            throwNoDatabase();
        }

        this._documentStore = documentStore;
        this._requestExecutor = ObjectUtils.firstNonNull(options.getRequestExecutor(), documentStore.getRequestExecutor(databaseName));

        noTracking = options.isNoTracking();

        this.useOptimisticConcurrency = _requestExecutor.getConventions().isUseOptimisticConcurrency();
        this.maxNumberOfRequestsPerSession = _requestExecutor.getConventions().getMaxNumberOfRequestsPerSession();
        this.generateEntityIdOnTheClient = new GenerateEntityIdOnTheClient(_requestExecutor.getConventions(), this::generateId);
        this.entityToJson = new EntityToJson(this);

        sessionInfo = new SessionInfo(_clientSessionId, _documentStore.getLastTransactionIndex(databaseName), options.isNoCaching());
        transactionMode = options.getTransactionMode();
    }

    /**
     * Gets the metadata for the specified entity.
     *
     * @param <T>      instance class
     * @param instance Instance to get metadata from
     * @return document metadata
     */
    public <T> IMetadataDictionary getMetadataFor(T instance) {
        if (instance == null) {
            throw new IllegalArgumentException("Instance cannot be null");
        }

        DocumentInfo documentInfo = getDocumentInfo(instance);
        if (documentInfo.getMetadataInstance() != null) {
            return documentInfo.getMetadataInstance();
        }

        ObjectNode metadataAsJson = documentInfo.getMetadata();
        MetadataAsDictionary metadata = new MetadataAsDictionary(metadataAsJson);
        documentInfo.setMetadataInstance(metadata);
        return metadata;
    }

    /**
     * Gets all counter names for the specified entity.
     * @param instance Instance
     * @param <T> Instance class
     * @return All counters names
     */
    public <T> List<String> getCountersFor(T instance) {
        if (instance == null) {
            throw new IllegalArgumentException("Instance cannot be null");
        }

        DocumentInfo documentInfo = getDocumentInfo(instance);

        ArrayNode countersArray = (ArrayNode) documentInfo.getMetadata().get(Constants.Documents.Metadata.COUNTERS);
        if (countersArray == null) {
            return null;
        }

        return IntStream.range(0, countersArray.size())
                .mapToObj(i -> countersArray.get(i).asText())
                .collect(Collectors.toList());
    }

    /**
     * Gets the Change Vector for the specified entity.
     * If the entity is transient, it will load the change vector from the store
     * and associate the current state of the entity with the change vector from the server.
     *
     * @param <T>      instance class
     * @param instance Instance to get change vector from
     * @return change vector
     */
    public <T> String getChangeVectorFor(T instance) {
        if (instance == null) {
            throw new IllegalArgumentException("instance cannot be null");
        }

        DocumentInfo documentInfo = getDocumentInfo(instance);
        JsonNode changeVector = documentInfo.getMetadata().get(Constants.Documents.Metadata.CHANGE_VECTOR);
        if (changeVector != null) {
            return changeVector.asText();
        }
        return null;
    }

    public <T> Date getLastModifiedFor(T instance) {
        if (instance == null) {
            throw new IllegalArgumentException("Instance cannot be null");
        }

        DocumentInfo documentInfo = getDocumentInfo(instance);
        JsonNode lastModified = documentInfo.getMetadata().get(Constants.Documents.Metadata.LAST_MODIFIED);
        if (lastModified != null && !lastModified.isNull()) {
            return mapper.convertValue(lastModified, Date.class);
        }
        return null;
    }

    private <T> DocumentInfo getDocumentInfo(T instance) {
        DocumentInfo documentInfo = documentsByEntity.get(instance);

        if (documentInfo != null) {
            return documentInfo;
        }

        Reference<String> idRef = new Reference<>();
        if (!generateEntityIdOnTheClient.tryGetIdFromInstance(instance, idRef)) {
            throw new IllegalStateException("Could not find the document id for " + instance);
        }

        assertNoNonUniqueInstance(instance, idRef.value);

        throw new IllegalArgumentException("Document " + idRef.value + " doesn't exist in the session");
    }

    /**
     * Returns whether a document with the specified id is loaded in the
     * current session
     *
     * @param id Document id to check
     * @return true is document is loaded
     */
    public boolean isLoaded(String id) {
        return isLoadedOrDeleted(id);
    }

    public boolean isLoadedOrDeleted(String id) {
        DocumentInfo documentInfo = documentsById.getValue(id);
        return (documentInfo != null && (documentInfo.getDocument() != null || documentInfo.getEntity() != null)) || isDeleted(id) || includedDocumentsById.containsKey(id);
    }

    /**
     * Returns whether a document with the specified id is deleted
     * or known to be missing
     *
     * @param id Document id to check
     * @return true is document is deleted
     */
    public boolean isDeleted(String id) {
        return _knownMissingIds.contains(id);
    }

    /**
     * Gets the document id.
     *
     * @param instance instance to get document id from
     * @return document id
     */
    public String getDocumentId(Object instance) {
        if (instance == null) {
            return null;
        }
        DocumentInfo value = documentsByEntity.get(instance);
        return value != null ? value.getId() : null;
    }

    public void incrementRequestCount() {
        if (++numberOfRequests > maxNumberOfRequestsPerSession)
            throw new IllegalStateException(String.format("The maximum number of requests (%d) allowed for this session has been reached." +
                    "Raven limits the number of remote calls that a session is allowed to make as an early warning system. Sessions are expected to be short lived, and " +
                    "Raven provides facilities like load(String[] keys) to load multiple documents at once and batch saves (call SaveChanges() only once)." +
                    "You can increase the limit by setting DocumentConvention.MaxNumberOfRequestsPerSession or MaxNumberOfRequestsPerSession, but it is" +
                    "advisable that you'll look into reducing the number of remote calls first, since that will speed up your application significantly and result in a" +
                    "more responsive application.", maxNumberOfRequestsPerSession));
    }

    /**
     * Tracks the entity inside the unit of work
     *
     * @param <T>           entity class
     * @param clazz         entity class
     * @param documentFound Document info
     * @return tracked entity
     */
    @SuppressWarnings("unchecked")
    public <T> T trackEntity(Class<T> clazz, DocumentInfo documentFound) {
        return (T) trackEntity(clazz, documentFound.getId(), documentFound.getDocument(), documentFound.getMetadata(), noTracking);
    }

    public void registerExternalLoadedIntoTheSession(DocumentInfo info) {
        if (noTracking) {
            return;
        }

        DocumentInfo existing = documentsById.getValue(info.getId());
        if (existing != null) {
            if (existing.getEntity() == info.getEntity()) {
                return;
            }

            throw new IllegalStateException("The document " + info.getId() + " is already in the session with a different entity instance.");
        }

        DocumentInfo existingEntity = documentsByEntity.get(info.getEntity());
        if (existingEntity != null) {
            if (existingEntity.getId().equalsIgnoreCase(info.getId())) {
                return;
            }

            throw new IllegalStateException("Attempted to load an entity with id " + info.getId() + ", but the entity instance already exists in the session with id: " + existing.getId());
        }

        documentsByEntity.put(info.getEntity(), info);
        documentsById.add(info);
        includedDocumentsById.remove(info.getId());

    }

    /**
     * Tracks the entity.
     *
     * @param entityType Entity class
     * @param id         Id of document
     * @param document   raw entity
     * @param metadata   raw document metadata
     * @param noTracking no tracking
     * @return entity
     */
    public Object trackEntity(Class entityType, String id, ObjectNode document, ObjectNode metadata, boolean noTracking) {

        noTracking = this.noTracking || noTracking;  // if noTracking is session-wide then we want to override the passed argument

        if (StringUtils.isEmpty(id)) {
            return deserializeFromTransformer(entityType, null, document);
        }

        DocumentInfo docInfo = documentsById.getValue(id);
        if (docInfo != null) {
            // the local instance may have been changed, we adhere to the current Unit of Work
            // instance, and return that, ignoring anything new.

            if (docInfo.getEntity() == null) {
                docInfo.setEntity(entityToJson.convertToEntity(entityType, id, document));
            }

            if (!noTracking) {
                includedDocumentsById.remove(id);
                documentsByEntity.put(docInfo.getEntity(), docInfo);
            }

            return docInfo.getEntity();
        }

        docInfo = includedDocumentsById.get(id);
        if (docInfo != null) {
            if (docInfo.getEntity() == null) {
                docInfo.setEntity(entityToJson.convertToEntity(entityType, id, document));
            }

            if (!noTracking) {
                includedDocumentsById.remove(id);
                documentsById.add(docInfo);
                documentsByEntity.put(docInfo.getEntity(), docInfo);
            }

            return docInfo.getEntity();
        }

        Object entity = entityToJson.convertToEntity(entityType, id, document);

        String changeVector = metadata.get(Constants.Documents.Metadata.CHANGE_VECTOR).asText();
        if (changeVector == null) {
            throw new IllegalStateException("Document " + id + " must have Change Vector");
        }

        if (!noTracking) {
            DocumentInfo newDocumentInfo = new DocumentInfo();
            newDocumentInfo.setId(id);
            newDocumentInfo.setDocument(document);
            newDocumentInfo.setMetadata(metadata);
            newDocumentInfo.setEntity(entity);
            newDocumentInfo.setChangeVector(changeVector);

            documentsById.add(newDocumentInfo);
            documentsByEntity.put(entity, newDocumentInfo);
        }

        return entity;
    }

    /**
     * Gets the default value of the specified type.
     *
     * @param clazz Class
     * @return Default value to given class
     */
    @SuppressWarnings("unchecked")
    public static Object getDefaultValue(Class clazz) {
        return Defaults.defaultValue(clazz);
    }

    /**
     * Marks the specified entity for deletion. The entity will be deleted when SaveChanges is called.
     *
     * @param <T>    entity class
     * @param entity Entity to delete
     */
    public <T> void delete(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }

        DocumentInfo value = documentsByEntity.get(entity);
        if (value == null) {
            throw new IllegalStateException(entity + " is not associated with the session, cannot delete unknown entity instance");
        }

        deletedEntities.add(entity);
        includedDocumentsById.remove(value.getId());
        if (_countersByDocId != null) {
            _countersByDocId.remove(value.getId());
        }
        _knownMissingIds.add(value.getId());
    }

    /**
     * Marks the specified entity for deletion. The entity will be deleted when IDocumentSession.SaveChanges is called.
     * WARNING: This method will not call beforeDelete listener!
     *
     * @param id Id of document
     */
    public void delete(String id) {
        delete(id, null);
    }

    public void delete(String id, String expectedChangeVector) {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }

        String changeVector = null;
        DocumentInfo documentInfo = documentsById.getValue(id);
        if (documentInfo != null) {
            ObjectNode newObj = entityToJson.convertEntityToJson(documentInfo.getEntity(), documentInfo);
            if (documentInfo.getEntity() != null && entityChanged(newObj, documentInfo, null)) {
                throw new IllegalStateException("Can't delete changed entity using identifier. Use delete(Class clazz, T entity) instead.");
            }

            if (documentInfo.getEntity() != null) {
                documentsByEntity.remove(documentInfo.getEntity());
            }

            documentsById.remove(id);
            changeVector = documentInfo.getChangeVector();
        }

        _knownMissingIds.add(id);
        changeVector = isUseOptimisticConcurrency() ? changeVector : null;
        if (_countersByDocId != null) {
            _countersByDocId.remove(id);
        }
        defer(new DeleteCommandData(id, ObjectUtils.firstNonNull(expectedChangeVector, changeVector)));
    }

    /**
     * Stores the specified entity in the session. The entity will be saved when SaveChanges is called.
     *
     * @param entity Entity to store
     */
    public void store(Object entity) {
        Reference<String> stringReference = new Reference<>();
        boolean hasId = generateEntityIdOnTheClient.tryGetIdFromInstance(entity, stringReference);
        storeInternal(entity, null, null, !hasId ? ConcurrencyCheckMode.FORCED : ConcurrencyCheckMode.AUTO);
    }

    /**
     * Stores the specified entity in the session, explicitly specifying its Id. The entity will be saved when SaveChanges is called.
     *
     * @param entity Entity to store
     * @param id     Entity identifier
     */
    public void store(Object entity, String id) {
        storeInternal(entity, null, id, ConcurrencyCheckMode.AUTO);
    }

    /**
     * Stores the specified entity in the session, explicitly specifying its Id. The entity will be saved when SaveChanges is called.
     *
     * @param entity       Entity to store
     * @param changeVector Change vector
     * @param id           Entity identifier
     */
    public void store(Object entity, String changeVector, String id) {
        storeInternal(entity, changeVector, id, changeVector == null ? ConcurrencyCheckMode.DISABLED : ConcurrencyCheckMode.FORCED);
    }

    private void storeInternal(Object entity, String changeVector, String id, ConcurrencyCheckMode forceConcurrencyCheck) {
        if (noTracking) {
            throw new IllegalStateException("Cannot store entity. Entity tracking is disabled in this session.");
        }
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }

        DocumentInfo value = documentsByEntity.get(entity);
        if (value != null) {
            value.setChangeVector(ObjectUtils.firstNonNull(changeVector, value.getChangeVector()));
            value.setConcurrencyCheckMode(forceConcurrencyCheck);
            return;
        }

        if (id == null) {
            if (generateDocumentKeysOnStore) {
                id = generateEntityIdOnTheClient.generateDocumentKeyForStorage(entity);
            } else {
                rememberEntityForDocumentIdGeneration(entity);
            }
        } else {
            // Store it back into the Id field so the client has access to it
            generateEntityIdOnTheClient.trySetIdentity(entity, id);
        }

        if (deferredCommandsMap.containsKey(IdTypeAndName.create(id, CommandType.CLIENT_ANY_COMMAND, null))) {
            throw new IllegalStateException("Can't store document, there is a deferred command registered for this document in the session. Document id: " + id);
        }

        if (deletedEntities.contains(entity)) {
            throw new IllegalStateException("Can't store object, it was already deleted in this session.  Document id: " + id);
        }


        // we make the check here even if we just generated the ID
        // users can override the ID generation behavior, and we need
        // to detect if they generate duplicates.
        assertNoNonUniqueInstance(entity, id);

        String collectionName = _requestExecutor.getConventions().getCollectionName(entity);

        ObjectMapper mapper = JsonExtensions.getDefaultMapper();
        ObjectNode metadata = mapper.createObjectNode();

        if (collectionName != null) {
            metadata.set(Constants.Documents.Metadata.COLLECTION, mapper.convertValue(collectionName, JsonNode.class));
        }

        String javaType = _requestExecutor.getConventions().getJavaClassName(entity.getClass());
        if (javaType != null) {
            metadata.set(Constants.Documents.Metadata.RAVEN_JAVA_TYPE, mapper.convertValue(javaType, TextNode.class));
        }

        if (id != null) {
            _knownMissingIds.remove(id);
        }

        storeEntityInUnitOfWork(id, entity, changeVector, metadata, forceConcurrencyCheck);
    }

    protected abstract String generateId(Object entity);

    protected void rememberEntityForDocumentIdGeneration(Object entity) {
        throw new NotImplementedException("You cannot set GenerateDocumentIdsOnStore to false without implementing RememberEntityForDocumentIdGeneration");
    }

    protected void storeEntityInUnitOfWork(String id, Object entity, String changeVector, ObjectNode metadata, ConcurrencyCheckMode forceConcurrencyCheck) {
        deletedEntities.remove(entity);

        if (id != null) {
            _knownMissingIds.remove(id);
        }

        DocumentInfo documentInfo = new DocumentInfo();
        documentInfo.setId(id);
        documentInfo.setMetadata(metadata);
        documentInfo.setChangeVector(changeVector);
        documentInfo.setConcurrencyCheckMode(forceConcurrencyCheck);
        documentInfo.setEntity(entity);
        documentInfo.setNewDocument(true);
        documentInfo.setDocument(null);

        documentsByEntity.put(entity, documentInfo);

        if (id != null) {
            documentsById.add(documentInfo);
        }
    }

    protected void assertNoNonUniqueInstance(Object entity, String id) {
        if (StringUtils.isEmpty(id) || id.charAt(id.length() - 1) == '|' || id.charAt(id.length() - 1) == '/') {
            return;
        }
        DocumentInfo info = documentsById.getValue(id);
        if (info == null || info.getEntity() == entity) {
            return;
        }

        throw new NonUniqueObjectException("Attempted to associate a different object with id '" + id + "'.");
    }

    public SaveChangesData prepareForSaveChanges() {
        SaveChangesData result = new SaveChangesData(this);

        int deferredCommandsCount = deferredCommands.size();

        prepareForEntitiesDeletion(result, null);
        prepareForEntitiesPuts(result);

        prepareCompareExchangeEntities(result);

        if (deferredCommands.size() > deferredCommandsCount) {
            // this allow OnBeforeStore to call Defer during the call to include
            // additional values during the same SaveChanges call

            for (int i = deferredCommandsCount; i < deferredCommands.size(); i++) {
                result.deferredCommands.add(deferredCommands.get(i));
            }

            for (Map.Entry<IdTypeAndName, ICommandData> item : deferredCommandsMap.entrySet()) {
                result.deferredCommandsMap.put(item.getKey(), item.getValue());
            }
        }

        for (ICommandData deferredCommand : result.getDeferredCommands()) {
            deferredCommand.onBeforeSaveChanges(this);
        }

        return result;
    }

    public void validateClusterTransaction(SaveChangesData result) {
        if (transactionMode != TransactionMode.CLUSTER_WIDE) {
            return;
        }

        if (isUseOptimisticConcurrency()) {
            throw new IllegalStateException("useOptimisticConcurrency is not supported with TransactionMode set to " + TransactionMode.CLUSTER_WIDE);
        }

        for (ICommandData commandData : result.getSessionCommands()) {

            switch (commandData.getType()) {
                case PUT:
                case DELETE:
                    if (commandData.getChangeVector() != null) {
                        throw new IllegalStateException("Optimistic concurrency for " + commandData.getId() + " is not supported when using a cluster transaction");
                    }
                    break;
                case COMPARE_EXCHANGE_DELETE:
                case COMPARE_EXCHANGE_PUT:
                    break;
                default:
                    throw new IllegalStateException("The command '" + commandData.getType() + "' is not supported in a cluster session.");
            }
        }

    }

    private void prepareCompareExchangeEntities(SaveChangesData result) {
        ClusterTransactionOperationsBase clusterTransactionOperations = getClusterSession();

        if (clusterTransactionOperations == null || !clusterTransactionOperations.hasCommands()) {
            return;
        }

        if (transactionMode != TransactionMode.CLUSTER_WIDE) {
            throw new IllegalStateException("Performing cluster transaction operation require the TransactionMode to be set to CLUSTER_WIDE");
        }

        if (clusterTransactionOperations.getStoreCompareExchange() != null) {
            for (Map.Entry<String, ClusterTransactionOperationsBase.StoredCompareExchange> item : clusterTransactionOperations.getStoreCompareExchange().entrySet()) {

                ObjectMapper mapper = getConventions().getEntityMapper();
                JsonNode entityAsTree = EntityToJson.convertEntityToJson(item.getValue().entity, getConventions(), null, false);
                ObjectNode rootNode = mapper.createObjectNode();
                rootNode.set("Object", entityAsTree);

                result.getSessionCommands().add(new PutCompareExchangeCommandData(item.getKey(), rootNode, item.getValue().index));
            }
        }

        if (clusterTransactionOperations.getDeleteCompareExchange() != null) {
            for (Map.Entry<String, Long> item : clusterTransactionOperations.getDeleteCompareExchange().entrySet()) {
                result.getSessionCommands().add(new DeleteCompareExchangeCommandData(item.getKey(), item.getValue()));
            }
        }

        result.onSuccess.clearClusterTransactionOperations(clusterTransactionOperations);
    }

    protected abstract ClusterTransactionOperationsBase getClusterSession();

    private static boolean updateMetadataModifications(DocumentInfo documentInfo) {
        boolean dirty = false;
        ObjectMapper mapper = JsonExtensions.getDefaultMapper();
        if (documentInfo.getMetadataInstance() != null) {
            if (documentInfo.getMetadataInstance().isDirty()) {
                dirty = true;
            }
            for (String prop : documentInfo.getMetadataInstance().keySet()) {
                Object propValue = documentInfo.getMetadataInstance().get(prop);
                if (propValue == null || propValue instanceof MetadataAsDictionary && (((MetadataAsDictionary) propValue).isDirty())) {
                    dirty = true;
                }
                documentInfo.getMetadata().set(prop, mapper.convertValue(propValue, JsonNode.class));
            }
        }
        return dirty;
    }

    @SuppressWarnings("ConstantConditions")
    private void prepareForEntitiesDeletion(SaveChangesData result, Map<String, List<DocumentsChanges>> changes) {
        for (Object deletedEntity : deletedEntities) {
            DocumentInfo documentInfo = documentsByEntity.get(deletedEntity);
            if (documentInfo == null) {
                continue;
            }

            if (changes != null) {
                List<DocumentsChanges> docChanges = new ArrayList<>();
                DocumentsChanges change = new DocumentsChanges();
                change.setFieldNewValue("");
                change.setFieldOldValue("");
                change.setChange(DocumentsChanges.ChangeType.DOCUMENT_DELETED);

                docChanges.add(change);
                changes.put(documentInfo.getId(), docChanges);
            } else {
                ICommandData command = result.getDeferredCommandsMap().get(IdTypeAndName.create(documentInfo.getId(), CommandType.CLIENT_ANY_COMMAND, null));
                if (command != null) {
                    throwInvalidDeletedDocumentWithDeferredCommand(command);
                }

                String changeVector = null;
                documentInfo = documentsById.getValue(documentInfo.getId());

                if (documentInfo != null) {
                    changeVector = documentInfo.getChangeVector();

                    if (documentInfo.getEntity() != null) {
                        result.onSuccess.removeDocumentByEntity(documentInfo.getEntity());
                        result.getEntities().add(documentInfo.getEntity());
                    }

                    result.onSuccess.removeDocumentByEntity(documentInfo.getId());
                }

                changeVector = useOptimisticConcurrency ? changeVector : null;
                BeforeDeleteEventArgs beforeDeleteEventArgs = new BeforeDeleteEventArgs(this, documentInfo.getId(), documentInfo.getEntity());
                EventHelper.invoke(onBeforeDelete, this, beforeDeleteEventArgs);
                result.getSessionCommands().add(new DeleteCommandData(documentInfo.getId(), changeVector));
            }

            if (changes == null) {
                result.onSuccess.clearDeletedEntities();
            }
        }

    }

    @SuppressWarnings("ConstantConditions")
    private void prepareForEntitiesPuts(SaveChangesData result) {
        for (Map.Entry<Object, DocumentInfo> entity : documentsByEntity.entrySet()) {

            if (entity.getValue().isIgnoreChanges())
                continue;

            if (isDeleted(entity.getValue().getId())) {
                continue;
            }

            boolean dirtyMetadata = updateMetadataModifications(entity.getValue());

            ObjectNode document = entityToJson.convertEntityToJson(entity.getKey(), entity.getValue());

            if ((!entityChanged(document, entity.getValue(), null)) && !dirtyMetadata) {
                continue;
            }

            ICommandData command = result.deferredCommandsMap.get(IdTypeAndName.create(entity.getValue().getId(), CommandType.CLIENT_MODIFY_DOCUMENT_COMMAND, null));
            if (command != null) {
                throwInvalidModifiedDocumentWithDeferredCommand(command);
            }

            List<EventHandler<BeforeStoreEventArgs>> onBeforeStore = this.onBeforeStore;
            if (onBeforeStore != null && !onBeforeStore.isEmpty()) {
                BeforeStoreEventArgs beforeStoreEventArgs = new BeforeStoreEventArgs(this, entity.getValue().getId(), entity.getKey());
                EventHelper.invoke(onBeforeStore, this, beforeStoreEventArgs);

                if (beforeStoreEventArgs.isMetadataAccessed()) {
                    updateMetadataModifications(entity.getValue());
                }

                if (beforeStoreEventArgs.isMetadataAccessed() || entityChanged(document, entity.getValue(), null)) {
                    document = entityToJson.convertEntityToJson(entity.getKey(), entity.getValue());
                }
            }

            result.getEntities().add(entity.getKey());

            if (entity.getValue().getId() != null) {
                result.onSuccess.removeDocumentById(entity.getValue().getId());
            }

            result.onSuccess.updateEntityDocumentInfo(entity.getValue(), document);

            String changeVector;
            if (useOptimisticConcurrency) {
                if (entity.getValue().getConcurrencyCheckMode() != ConcurrencyCheckMode.DISABLED) {
                    // if the user didn't provide a change vector, we'll test for an empty one
                    changeVector = ObjectUtils.firstNonNull(entity.getValue().getChangeVector(), "");
                } else {
                    changeVector = null;
                }
            } else if (entity.getValue().getConcurrencyCheckMode() == ConcurrencyCheckMode.FORCED) {
                changeVector = entity.getValue().getChangeVector();
            } else {
                changeVector = null;
            }

            result.getSessionCommands().add(new PutCommandDataWithJson(entity.getValue().getId(), changeVector, document));
        }
    }

    private static void throwInvalidModifiedDocumentWithDeferredCommand(ICommandData resultCommand) {
        throw new IllegalStateException("Cannot perform save because document " + resultCommand.getId()
                + " has been modified by the session and is also taking part in deferred " + resultCommand.getType() + " command");
    }

    private static void throwInvalidDeletedDocumentWithDeferredCommand(ICommandData resultCommand) {
        throw new IllegalStateException("Cannot perform save because document " + resultCommand.getId()
                + " has been deleted by the session and is also taking part in deferred " + resultCommand.getType() + " command");
    }

    private static void throwNoDatabase() {
        throw new IllegalStateException("Cannot open a Session without specifying a name of a database " +
                "to operate on. Database name can be passed as an argument when Session is" +
                " being opened or default database can be defined using 'DocumentStore.setDatabase()' method");
    }

    protected boolean entityChanged(ObjectNode newObj, DocumentInfo documentInfo, Map<String, List<DocumentsChanges>> changes) {
        return JsonOperation.entityChanged(newObj, documentInfo, changes);
    }

    public Map<String, List<DocumentsChanges>> whatChanged() {
        HashMap<String, List<DocumentsChanges>> changes = new HashMap<>();

        prepareForEntitiesDeletion(null, changes);
        getAllEntitiesChanges(changes);

        return changes;
    }


    /**
     * Gets a value indicating whether any of the entities tracked by the session has changes.
     *
     * @return true if session has changes
     */
    public boolean hasChanges() {
        for (Map.Entry<Object, DocumentInfo> entity : documentsByEntity.entrySet()) {
            ObjectNode document = entityToJson.convertEntityToJson(entity.getKey(), entity.getValue());
            if (entityChanged(document, entity.getValue(), null)) {
                return true;
            }
        }

        return !deletedEntities.isEmpty();
    }

    /**
     * Determines whether the specified entity has changed.
     *
     * @param entity Entity to check
     * @return true if entity has changed
     */
    public boolean hasChanged(Object entity) {
        DocumentInfo documentInfo = documentsByEntity.get(entity);

        if (documentInfo == null) {
            return false;
        }

        ObjectNode document = entityToJson.convertEntityToJson(entity, documentInfo);
        return entityChanged(document, documentInfo, null);
    }

    public void waitForReplicationAfterSaveChanges() {
        waitForReplicationAfterSaveChanges(options -> {
        });
    }

    public void waitForReplicationAfterSaveChanges(Consumer<ReplicationWaitOptsBuilder> options) {
        ReplicationWaitOptsBuilder builder = new ReplicationWaitOptsBuilder();
        options.accept(builder);

        BatchOptions builderOptions = builder.getOptions();
        ReplicationBatchOptions replicationOptions = builderOptions.getReplicationOptions();
        if (replicationOptions == null) {
            builderOptions.setReplicationOptions(replicationOptions = new ReplicationBatchOptions());
        }

        if (replicationOptions.getWaitForReplicasTimeout() == null) {
            replicationOptions.setWaitForReplicasTimeout(Duration.ofSeconds(15));
        }

        replicationOptions.setWaitForReplicas(true);
    }

    public void waitForIndexesAfterSaveChanges() {
        waitForReplicationAfterSaveChanges(options -> {
        });
    }

    public void waitForIndexesAfterSaveChanges(Consumer<InMemoryDocumentSessionOperations.IndexesWaitOptsBuilder> options) {
        IndexesWaitOptsBuilder builder = new IndexesWaitOptsBuilder();
        options.accept(builder);

        BatchOptions builderOptions = builder.getOptions();
        IndexBatchOptions indexOptions = builderOptions.getIndexOptions();

        if (indexOptions == null) {
            builderOptions.setIndexOptions(indexOptions = new IndexBatchOptions());
        }

        if (indexOptions.getWaitForIndexesTimeout() == null) {
            indexOptions.setWaitForIndexesTimeout(Duration.ofSeconds(15));
        }

        indexOptions.setWaitForIndexes(true);
    }

    private void getAllEntitiesChanges(Map<String, List<DocumentsChanges>> changes) {
        for (Map.Entry<String, DocumentInfo> pair : documentsById) {
            updateMetadataModifications(pair.getValue());
            ObjectNode newObj = entityToJson.convertEntityToJson(pair.getValue().getEntity(), pair.getValue());
            entityChanged(newObj, pair.getValue(), changes);
        }
    }

    /**
     * Mark the entity as one that should be ignore for change tracking purposes,
     * it still takes part in the session, but is ignored for SaveChanges.
     *
     * @param entity entity
     */
    public void ignoreChangesFor(Object entity) {
        getDocumentInfo(entity).setIgnoreChanges(true);
    }

    /**
     * Evicts the specified entity from the session.
     * Remove the entity from the delete queue and stops tracking changes for this entity.
     *
     * @param <T>    entity class
     * @param entity Entity to evict
     */
    public <T> void evict(T entity) {
        DocumentInfo documentInfo = documentsByEntity.get(entity);
        if (documentInfo != null) {
            documentsByEntity.remove(entity);
            documentsById.remove(documentInfo.getId());
        }

        deletedEntities.remove(entity);
        if (_countersByDocId != null) {
            _countersByDocId.remove(documentInfo.getId());
        }
    }

    /**
     * Clears this instance.
     * Remove all entities from the delete queue and stops tracking changes for all entities.
     */
    public void clear() {
        documentsByEntity.clear();
        deletedEntities.clear();
        documentsById.clear();
        _knownMissingIds.clear();
        if (_countersByDocId != null) {
            _countersByDocId.clear();
        }
    }

    /**
     * Defer commands to be executed on saveChanges()
     *
     * @param command  Command to defer
     * @param commands More commands to defer
     */
    public void defer(ICommandData command, ICommandData... commands) {
        deferredCommands.add(command);
        deferInternal(command);

        if (commands != null && commands.length > 0)
            defer(commands);
    }

    /**
     * Defer commands to be executed on saveChanges()
     *
     * @param commands Commands to defer
     */
    public void defer(ICommandData[] commands) {
        deferredCommands.addAll(Arrays.asList(commands));
        for (ICommandData command : commands) {
            deferInternal(command);
        }
    }

    private void deferInternal(final ICommandData command) {

        if (command.getType() == CommandType.BATCH_PATCH) {
            BatchPatchCommandData batchPatchCommand = (BatchPatchCommandData) command;
            for (BatchPatchCommandData.IdAndChangeVector kvp : batchPatchCommand.getIds()) {
                addCommand(command, kvp.getId(), CommandType.PATCH, command.getName());
            }
            return;
        }

        addCommand(command, command.getId(), command.getType(), command.getName());
    }

    private void addCommand(ICommandData command, String id, CommandType commandType, String commandName) {
        deferredCommandsMap.put(IdTypeAndName.create(id, commandType, commandName), command);
        deferredCommandsMap.put(IdTypeAndName.create(id, CommandType.CLIENT_ANY_COMMAND, null), command);

        if (!CommandType.ATTACHMENT_PUT.equals(command.getType()) &&
                !CommandType.ATTACHMENT_DELETE.equals(command.getType()) &&
                !CommandType.ATTACHMENT_COPY.equals(command.getType()) &&
                !CommandType.ATTACHMENT_MOVE.equals(command.getType()) &&
                !CommandType.COUNTERS.equals(command.getType())) {
            deferredCommandsMap.put(IdTypeAndName.create(id, CommandType.CLIENT_MODIFY_DOCUMENT_COMMAND, null), command);
        }
    }

    private void close(boolean isDisposing) {
        if (_isDisposed) {
            return;
        }

        _isDisposed = true;

        // nothing more to do for now
    }

    /**
     * Performs application-defined tasks associated with freeing, releasing, or resetting unmanaged resources.
     */
    public void close() {
        this.close(true);
    }

    public void registerMissing(String id) {
        if (noTracking) {
            return;
        }
        _knownMissingIds.add(id);
    }

    public void registerIncludes(ObjectNode includes) {
        if (noTracking) {
            return;
        }

        if (includes == null) {
            return;
        }

        for (String fieldName : Lists.newArrayList(includes.fieldNames())) {
            JsonNode fieldValue = includes.get(fieldName);

            if (fieldValue == null) {
                continue;
            }

            ObjectNode json = (ObjectNode) fieldValue;

            DocumentInfo newDocumentInfo = DocumentInfo.getNewDocumentInfo(json);
            if (JsonExtensions.tryGetConflict(newDocumentInfo.getMetadata())) {
                continue;
            }

            includedDocumentsById.put(newDocumentInfo.getId(), newDocumentInfo);
        }
    }

    public void registerMissingIncludes(ArrayNode results, ObjectNode includes, String[] includePaths) {

        if (noTracking) {
            return;
        }

        if (includePaths == null || includePaths.length == 0) {
            return;
        }

        for (JsonNode result : results) {
            for (String include : includePaths) {
                if (Constants.Documents.Indexing.Fields.DOCUMENT_ID_FIELD_NAME.equals(include)) {
                    continue;
                }

                IncludesUtil.include((ObjectNode) result, include, id -> {
                    if (id == null) {
                        return;
                    }

                    if (isLoaded(id)) {
                        return;
                    }

                    JsonNode document = includes.get(id);
                    if (document != null) {
                        JsonNode metadata = document.get(Constants.Documents.Metadata.KEY);

                        if (JsonExtensions.tryGetConflict(metadata)) {
                            return;
                        }
                    }

                    registerMissing(id);
                });
            }
        }
    }

    public void registerCounters(ObjectNode resultCounters, String[] ids, String[] countersToInclude, boolean gotAll) {
        if (noTracking) {
            return;
        }

        if (resultCounters == null || resultCounters.size() == 0) {
            if (gotAll) {
                for (String id : ids) {
                    setGotAllCountersForDocument(id);
                }

                return;
            }
        } else {
            registerCountersInternal(resultCounters, null, false, gotAll);
        }

        registerMissingCounters(ids, countersToInclude);
    }

    public void registerCounters(ObjectNode resultCounters, Map<String, String[]> countersToInclude) {
        if (noTracking) {
            return;
        }

        if (resultCounters == null || resultCounters.size() == 0) {
            setGotAllInCacheIfNeeded(countersToInclude);
        } else {
            registerCountersInternal(resultCounters, countersToInclude, true, false);
        }

        registerMissingCounters(countersToInclude);
    }

    private void registerCountersInternal(ObjectNode resultCounters, Map<String, String[]> countersToInclude, boolean fromQueryResult, boolean gotAll) {

        Iterator<Map.Entry<String, JsonNode>> fieldsIterator = resultCounters.fields();

        while (fieldsIterator.hasNext()) {
            Map.Entry<String, JsonNode> fieldAndValue = fieldsIterator.next();

            if (fieldAndValue.getValue() == null || fieldAndValue.getValue().isNull()) {
                continue;
            }

            if (fromQueryResult) {
                String[] counters = countersToInclude.get(fieldAndValue.getKey());
                gotAll = counters != null && counters.length == 0;
            }

            if (fieldAndValue.getValue().size() == 0 && !gotAll) {
                continue;
            }

            registerCountersForDocument(fieldAndValue.getKey(), gotAll, (ArrayNode) fieldAndValue.getValue());
        }
    }

    private void registerCountersForDocument(String id, boolean gotAll, ArrayNode counters) {
        Tuple<Boolean, Map<String, Long>> cache = getCountersByDocId().get(id);
        if (cache == null) {
            cache = Tuple.create(gotAll, new TreeMap<>(String::compareToIgnoreCase));
        }

        for (JsonNode counterJson : counters) {
            JsonNode counterName = counterJson.get("CounterName");
            JsonNode totalValue = counterJson.get("TotalValue");

            if (counterName != null && !counterName.isNull() && totalValue != null && !totalValue.isNull()) {
                cache.second.put(counterName.asText(), totalValue.longValue());
            }
        }

        cache.first = gotAll;
        getCountersByDocId().put(id, cache);
    }

    private void setGotAllInCacheIfNeeded(Map<String, String[]> countersToInclude) {
        for (Map.Entry<String, String[]> kvp : countersToInclude.entrySet()) {
            if (kvp.getValue().length > 0) {
                continue;
            }

            setGotAllCountersForDocument(kvp.getKey());
        }
    }

    private void setGotAllCountersForDocument(String id) {
        Tuple<Boolean, Map<String, Long>> cache = getCountersByDocId().get(id);

        if (cache == null) {
            cache = Tuple.create(false, new TreeMap<>(String::compareToIgnoreCase));
        }

        cache.first = true;
        getCountersByDocId().put(id, cache);
    }

    private void registerMissingCounters(Map<String, String[]> countersToInclude) {
        if (countersToInclude == null) {
            return;
        }

        for (Map.Entry<String, String[]> kvp : countersToInclude.entrySet()) {
            Tuple<Boolean, Map<String, Long>> cache = getCountersByDocId().get(kvp.getKey());
            if (cache == null) {
                cache = Tuple.create(false, new TreeMap<>(String::compareToIgnoreCase));
                getCountersByDocId().put(kvp.getKey(), cache);
            }

            for (String counter : kvp.getValue()) {
                if (cache.second.containsKey(counter)) {
                    continue;
                }

                cache.second.put(counter, null);
            }
        }
    }

    private void registerMissingCounters(String[] ids, String[] countersToInclude) {
        if (countersToInclude == null) {
            return;
        }

        for (String counter : countersToInclude) {
            for (String id : ids) {
                Tuple<Boolean, Map<String, Long>> cache = getCountersByDocId().get(id);
                if (cache == null) {
                    cache = Tuple.create(false, new TreeMap<>(String::compareToIgnoreCase));
                    getCountersByDocId().put(id, cache);
                }

                if (cache.second.containsKey(counter)) {
                    continue;
                }

                cache.second.put(counter, null);
            }
        }
    }

    @Override
    public int hashCode() {
        return _hash;
    }

    private Object deserializeFromTransformer(Class clazz, String id, ObjectNode document) {
        return entityToJson.convertToEntity(clazz, id, document);
    }

    public boolean checkIfIdAlreadyIncluded(String[] ids, Map.Entry<String, Class>[] includes) {
        return checkIfIdAlreadyIncluded(ids, Arrays.stream(includes).map(Map.Entry::getKey).collect(Collectors.toList()));
    }

    public boolean checkIfIdAlreadyIncluded(String[] ids, Collection<String> includes) {
        for (String id : ids) {
            if (_knownMissingIds.contains(id)) {
                continue;
            }

            // Check if document was already loaded, the check if we've received it through include
            DocumentInfo documentInfo = documentsById.getValue(id);
            if (documentInfo == null) {
                documentInfo = includedDocumentsById.get(id);
                if (documentInfo == null) {
                    return false;
                }
            }

            if (documentInfo.getEntity() == null) {
                return false;
            }

            if (includes == null) {
                continue;
            }

            for (String include : includes) {
                final boolean[] hasAll = {true}; //using fake array here to force final keyword on variable

                IncludesUtil.include(documentInfo.getDocument(), include, s -> hasAll[0] &= isLoaded(s));

                if (!hasAll[0]) {
                    return false;
                }

            }

        }

        return true;
    }

    protected <T> void refreshInternal(T entity, RavenCommand<GetDocumentsResult> cmd, DocumentInfo documentInfo) {
        ObjectNode document = (ObjectNode) cmd.getResult().getResults().get(0);
        if (document == null) {
            throw new IllegalStateException("Document '" + documentInfo.getId() + "' no longer exists and was probably deleted");
        }

        ObjectNode value = (ObjectNode) document.get(Constants.Documents.Metadata.KEY);
        documentInfo.setMetadata(value);

        if (documentInfo.getMetadata() != null) {
            JsonNode changeVector = value.get(Constants.Documents.Metadata.CHANGE_VECTOR);
            documentInfo.setChangeVector(changeVector.asText());
        }

        documentInfo.setDocument(document);

        documentInfo.setEntity(entityToJson.convertToEntity(entity.getClass(), documentInfo.getId(), document));

        try {
            BeanUtils.copyProperties(entity, documentInfo.getEntity());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Unable to refresh entity: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    protected static <T> T getOperationResult(Class<T> clazz, Object result) {
        if (result == null) {
            return Defaults.defaultValue(clazz);
        }

        if (clazz.isAssignableFrom(result.getClass())) {
            return (T) result;
        }

        if (result instanceof Map) {
            Map map = (Map) result;
            if (map.isEmpty()) {
                return null;
            } else {
                return (T) map.values().iterator().next();
            }
        }

        throw new IllegalStateException("Unable to cast " + result.getClass().getSimpleName() + " to " + clazz.getSimpleName());
    }

    protected void updateSessionAfterSaveChanges(BatchCommandResult result) {
        Long returnedTransactionIndex = result.getTransactionIndex();
        _documentStore.setLastTransactionIndex(getDatabaseName(), returnedTransactionIndex);
        sessionInfo.setLastClusterTransactionIndex(returnedTransactionIndex);
    }

    public void onAfterSaveChangesInvoke(AfterSaveChangesEventArgs afterSaveChangesEventArgs) {
        EventHelper.invoke(onAfterSaveChanges, this, afterSaveChangesEventArgs);
    }

    public void onBeforeQueryInvoke(BeforeQueryEventArgs beforeQueryEventArgs) {
        EventHelper.invoke(onBeforeQuery, this, beforeQueryEventArgs);
    }

    protected Tuple<String, String> processQueryParameters(Class clazz, String indexName, String collectionName, DocumentConventions conventions) {
        boolean isIndex = StringUtils.isNotBlank(indexName);
        boolean isCollection = StringUtils.isNotEmpty(collectionName);

        if (isIndex && isCollection) {
            throw new IllegalStateException("Parameters indexName and collectionName are mutually exclusive. Please specify only one of them.");
        }

        if (!isIndex && !isCollection) {
            collectionName = ObjectUtils.firstNonNull(
                    conventions.getCollectionName(clazz),
                    Constants.Documents.Metadata.ALL_DOCUMENTS_COLLECTION);
        }

        return Tuple.create(indexName, collectionName);
    }

    public static class SaveChangesData {
        private final List<ICommandData> deferredCommands;
        private final Map<IdTypeAndName, ICommandData> deferredCommandsMap;
        private final List<ICommandData> sessionCommands = new ArrayList<>();
        private final List<Object> entities = new ArrayList<>();
        private final BatchOptions options;
        private final ActionsToRunOnSuccess onSuccess;

        public SaveChangesData(InMemoryDocumentSessionOperations session) {
            deferredCommands = new ArrayList<>(session.deferredCommands);
            deferredCommandsMap = new HashMap<>(session.deferredCommandsMap);
            options = session._saveChangesOptions;
            onSuccess = new ActionsToRunOnSuccess(session);
        }

        public ActionsToRunOnSuccess getOnSuccess() {
            return onSuccess;
        }

        public List<ICommandData> getDeferredCommands() {
            return deferredCommands;
        }

        public List<ICommandData> getSessionCommands() {
            return sessionCommands;
        }

        public List<Object> getEntities() {
            return entities;
        }

        public BatchOptions getOptions() {
            return options;
        }

        public Map<IdTypeAndName, ICommandData> getDeferredCommandsMap() {
            return deferredCommandsMap;
        }

        public static class ActionsToRunOnSuccess {

            private final InMemoryDocumentSessionOperations _session;
            private final List<String> _documentsByIdToRemove = new ArrayList<>();
            private final List<Object> _documentsByEntityToRemove = new ArrayList<>();
            private final List<Tuple<DocumentInfo, ObjectNode>> _documentInfosToUpdate = new ArrayList<>();

            private ClusterTransactionOperationsBase _clusterTransactionOperations;
            private boolean _clearDeletedEntities;

            public ActionsToRunOnSuccess(InMemoryDocumentSessionOperations _session) {
                this._session = _session;
            }

            public void removeDocumentById(String id) {
                _documentsByIdToRemove.add(id);
            }

            public void removeDocumentByEntity(Object entity) {
                _documentsByEntityToRemove.add(entity);
            }

            public void clearClusterTransactionOperations(ClusterTransactionOperationsBase clusterTransactionOperations) {
                _clusterTransactionOperations = clusterTransactionOperations;
            }

            public void updateEntityDocumentInfo(DocumentInfo documentInfo, ObjectNode document) {
                _documentInfosToUpdate.add(Tuple.create(documentInfo, document));
            }

            public void clearSessionStateAfterSuccessfulSaveChanges() {
                for (String id : _documentsByIdToRemove) {
                    _session.documentsById.remove(id);
                }

                for (Object entity : _documentsByEntityToRemove) {
                    _session.documentsByEntity.remove(entity);
                }

                for (Tuple<DocumentInfo, ObjectNode> documentInfoObjectNodeTuple : _documentInfosToUpdate) {
                    DocumentInfo info = documentInfoObjectNodeTuple.first;
                    ObjectNode document = documentInfoObjectNodeTuple.second;
                    info.setNewDocument(false);
                    info.setDocument(document);
                }

                if (_clearDeletedEntities) {
                    _session.deletedEntities.clear();
                }

                if (_clusterTransactionOperations != null) {
                    _clusterTransactionOperations.clear();
                }

                _session.deferredCommands.clear();
                _session.deferredCommandsMap.clear();
            }

            public void clearDeletedEntities() {
                _clearDeletedEntities = true;
            }
        }
    }


    public class ReplicationWaitOptsBuilder {

        private BatchOptions getOptions() {
            if (InMemoryDocumentSessionOperations.this._saveChangesOptions == null) {
                InMemoryDocumentSessionOperations.this._saveChangesOptions = new BatchOptions();
            }

            if (InMemoryDocumentSessionOperations.this._saveChangesOptions.getReplicationOptions() == null) {
                InMemoryDocumentSessionOperations.this._saveChangesOptions.setReplicationOptions(new ReplicationBatchOptions());
            }

            return InMemoryDocumentSessionOperations.this._saveChangesOptions;
        }

        public ReplicationWaitOptsBuilder withTimeout(Duration timeout) {
            getOptions().getReplicationOptions().setWaitForReplicasTimeout(timeout);
            return this;
        }

        public ReplicationWaitOptsBuilder throwOnTimeout(boolean shouldThrow) {
            getOptions().getReplicationOptions().setThrowOnTimeoutInWaitForReplicas(shouldThrow);
            return this;
        }

        public ReplicationWaitOptsBuilder numberOfReplicas(int replicas) {
            getOptions().getReplicationOptions().setNumberOfReplicasToWaitFor(replicas);
            return this;
        }

        public ReplicationWaitOptsBuilder majority(boolean waitForMajority) {
            getOptions().getReplicationOptions().setMajority(waitForMajority);
            return this;
        }
    }

    public class IndexesWaitOptsBuilder {

        private BatchOptions getOptions() {
            if (InMemoryDocumentSessionOperations.this._saveChangesOptions == null) {
                InMemoryDocumentSessionOperations.this._saveChangesOptions = new BatchOptions();
            }

            if (InMemoryDocumentSessionOperations.this._saveChangesOptions.getIndexOptions() == null) {
                InMemoryDocumentSessionOperations.this._saveChangesOptions.setIndexOptions(new IndexBatchOptions());
            }

            return InMemoryDocumentSessionOperations.this._saveChangesOptions;
        }

        public IndexesWaitOptsBuilder withTimeout(Duration timeout) {
            getOptions().getIndexOptions().setWaitForIndexesTimeout(timeout);
            return this;
        }

        public IndexesWaitOptsBuilder throwOnTimeout(boolean shouldThrow) {
            getOptions().getIndexOptions().setThrowOnTimeoutInWaitForIndexes(shouldThrow);
            return this;
        }

        public IndexesWaitOptsBuilder waitForIndexes(String... indexes) {
            getOptions().getIndexOptions().setWaitForSpecificIndexes(indexes);
            return this;
        }
    }

    public TransactionMode getTransactionMode() {
        return transactionMode;
    }

    public void setTransactionMode(TransactionMode transactionMode) {
        this.transactionMode = transactionMode;
    }
}
