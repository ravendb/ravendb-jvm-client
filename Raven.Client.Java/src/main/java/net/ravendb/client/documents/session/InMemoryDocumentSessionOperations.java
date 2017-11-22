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
import net.ravendb.client.documents.commands.GetDocumentResult;
import net.ravendb.client.documents.commands.batches.*;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.identity.GenerateEntityIdOnTheClient;
import net.ravendb.client.documents.session.operations.lazy.ILazyOperation;
import net.ravendb.client.exceptions.documents.session.NonUniqueObjectException;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.http.CurrentIndexAndNode;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.JsonOperation;
import net.ravendb.client.json.MetadataAsDictionary;
import net.ravendb.client.primitives.*;
import net.ravendb.client.util.IdentityHashSet;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import java.awt.event.KeyEvent;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class InMemoryDocumentSessionOperations implements CleanCloseable {

    private static AtomicInteger _clientSessionIdCounter = new AtomicInteger(); //tODO: thread static?

    protected final int _clientSessionId = _clientSessionIdCounter.incrementAndGet();

    protected final RequestExecutor _requestExecutor;

    protected final List<ILazyOperation> pendingLazyOperations = new ArrayList<>();
    protected final Map<ILazyOperation, Consumer<Object>> onEvaluateLazy = new HashMap<>();

    private static AtomicInteger _instancesCounter = new AtomicInteger();

    private final int _hash = _instancesCounter.incrementAndGet();
    protected boolean generateDocumentKeysOnStore = true;
    protected SessionInfo sessionInfo;
    private BatchOptions _saveChangesOptions;

    private boolean _isDisposed;

    protected ObjectMapper mapper = JsonExtensions.getDefaultMapper();

    private UUID id;

    /**
     * The session id
     */
    public UUID getId() {
        return id;
    }

    /**
     * The entities waiting to be deleted
     */
    protected final Set<Object> deletedEntities = new IdentityHashSet<>();

    private List<EventHandler<BeforeStoreEventArgs>> onBeforeStore = new ArrayList<>();
    private List<EventHandler<AfterStoreEventArgs>> onAfterStore = new ArrayList<>();
    private List<EventHandler<BeforeDeleteEventArgs>> onBeforeDelete = new ArrayList<>();
    private List<EventHandler<BeforeQueryExecutedEventArgs>> onBeforeQueryExecuted = new ArrayList<>();

    public void addBeforeStoreListener(EventHandler<BeforeStoreEventArgs> handler) {
        this.onBeforeStore.add(handler);

    }

    public void removeBeforeStoreListener(EventHandler<BeforeStoreEventArgs> handler) {
        this.onBeforeStore.remove(handler);
    }

    public void addAfterStoreListener(EventHandler<AfterStoreEventArgs> handler) {
        this.onAfterStore.add(handler);
    }

    public void removeAfterStoreListener(EventHandler<AfterStoreEventArgs> handler) {
        this.onAfterStore.remove(handler);
    }

    public void addBeforeDeleteListener(EventHandler<BeforeDeleteEventArgs> handler) {
        this.onBeforeDelete.add(handler);
    }

    public void removeBeforeDeleteListener(EventHandler<BeforeDeleteEventArgs> handler) {
        this.onBeforeDelete.remove(handler);
    }

    public void addBeforeQueryExecutedListener(EventHandler<BeforeQueryExecutedEventArgs> handler) {
        this.onBeforeQueryExecuted.add(handler);
    }

    public void removeBeforeQueryExecutedListener(EventHandler<BeforeQueryExecutedEventArgs> handler) {
        this.onBeforeQueryExecuted.remove(handler);
    }

    //Entities whose id we already know do not exists, because they are a missing include, or a missing load, etc.
    protected final Set<String> knownMissingIds = new TreeSet<>(String.CASE_INSENSITIVE_ORDER); //TODO: do we need this this requires select token syntax in IncludesUtils

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
    public final Map<Object, DocumentInfo> documentsByEntity = new TreeMap<>((o1, o2) -> o1 == o2 ? 0 : 1);

    protected final DocumentStoreBase _documentStore;

    private String databaseName;

    public String getDatabaseName() {
        return databaseName;
    }

    /**
     * The document store associated with this session
     */
    public IDocumentStore getDocumentStore() {
        return _documentStore;
    }

    public RequestExecutor getRequestExecutor() {
        return _requestExecutor;
    }

    private int numberOfRequests;

    public int getNumberOfRequests() {
        return numberOfRequests;
    }

    /**
     * Gets the number of entities held in memory to manage Unit of Work
     */
    public int getNumberOfEntitiesInUnitOfWork() {
        return documentsByEntity.size();
    }

    /**
     * Gets the store identifier for this session.
     * The store identifier is the identifier for the particular RavenDB instance.
     */
    public String storeIdentifier() {
        return _documentStore.getIdentifier() + ";" + databaseName;
    }

    /**
     * Gets the conventions used by this session
     * This instance is shared among all sessions, changes to the DocumentConventions should be done
     * via the IDocumentSTore instance, not on a single session.
     */
    public DocumentConventions getConventions() {
        return _requestExecutor.getConventions();
    }

    private int maxNumberOfRequestsPerSession;

    /**
     * Gets the max number of requests per session.
     * If the NumberOfRequests rise above MaxNumberOfRequestsPerSession, an exception will be thrown.
     */
    public int getMaxNumberOfRequestsPerSession() {
        return maxNumberOfRequestsPerSession;
    }

    /**
     * Sets the max number of requests per session.
     * If the NumberOfRequests rise above MaxNumberOfRequestsPerSession, an exception will be thrown.
     */
    public void setMaxNumberOfRequestsPerSession(int maxNumberOfRequestsPerSession) {
        this.maxNumberOfRequestsPerSession = maxNumberOfRequestsPerSession;
    }

    private boolean useOptimisticConcurrency;

    /**
     * Gets value indicating whether the session should use optimistic concurrency.
     * When set to true, a check is made so that a change made behind the session back would fail
     * and raise ConcurrencyException
     */
    public boolean isUseOptimisticConcurrency() {
        return useOptimisticConcurrency;
    }

    /**
     * Sets value indicating whether the session should use optimistic concurrency.
     * When set to true, a check is made so that a change made behind the session back would fail
     * and raise ConcurrencyException
     */
    public void setUseOptimisticConcurrency(boolean useOptimisticConcurrency) {
        this.useOptimisticConcurrency = useOptimisticConcurrency;
    }

    protected final List<ICommandData> deferredCommands = new ArrayList<>();

    protected final Map<IdTypeAndName, ICommandData> deferredCommandsMap = new HashMap<>();

    public int getDeferredCommandsCount() {
        return deferredCommands.size();
    }

    private GenerateEntityIdOnTheClient generateEntityIdOnTheClient;

    public GenerateEntityIdOnTheClient getGenerateEntityIdOnTheClient() {
        return generateEntityIdOnTheClient;
    }

    private EntityToJson entityToJson;

    public EntityToJson getEntityToJson() {
        return entityToJson;
    }

    /**
     * Initializes a new instance of the InMemoryDocumentSessionOperations class.
     */
    protected InMemoryDocumentSessionOperations(String databaseName, DocumentStoreBase documentStore, RequestExecutor requestExecutor, UUID id) {
        this.id = id;
        this.databaseName = databaseName;
        this._documentStore = documentStore;
        this._requestExecutor = requestExecutor;

        this.useOptimisticConcurrency = requestExecutor.getConventions().isUseOptimisticConcurrency();
        this.maxNumberOfRequestsPerSession = requestExecutor.getConventions().getMaxNumberOfRequestsPerSession();
        this.generateEntityIdOnTheClient = new GenerateEntityIdOnTheClient(_requestExecutor.getConventions(), this::generateId);
        this.entityToJson = new EntityToJson(this);

        sessionInfo = new SessionInfo(_clientSessionId);
    }

    /**
     * Gets the metadata for the specified entity.
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
     * Gets the Change Vector for the specified entity.
     * If the entity is transient, it will load the change vector from the store
     * and associate the current state of the entity with the change vector from the server.
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
     */
    public boolean isLoaded(String id) {
        return isLoadedOrDeleted(id);
    }

    public boolean isLoadedOrDeleted(String id) {
        DocumentInfo documentInfo = documentsById.getValue(id);
        return (documentInfo != null && documentInfo.getDocument() != null) || isDeleted(id) || includedDocumentsById.containsKey(id);
    }

    /**
     * Returns whether a document with the specified id is deleted
     * or known to be missing
     */
    public boolean isDeleted(String id) {
        return knownMissingIds.contains(id);
    }

    /**
     * Gets the document id.
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
     */
    public <T> T trackEntity(Class<T> clazz, DocumentInfo documentFound) {
        return (T) trackEntity(clazz, documentFound.getId(), documentFound.getDocument(), documentFound.getMetadata(), false);
    }

    /**
     * Tracks the entity.
     */
    private Object trackEntity(Class entityType, String id, ObjectNode document, ObjectNode metadata, boolean noTracking) {
        /* TODO:
         if (string.IsNullOrEmpty(id))
            {
                return DeserializeFromTransformer(entityType, null, document);
            }
            */


        DocumentInfo docInfo = documentsById.getValue(id);
        if (docInfo != null) {
            // the local instance may have been changed, we adhere to the current Unit of Work
            // instance, and return that, ignoring anything new.

            if (docInfo.getEntity() == null) {
                docInfo.setEntity(convertToEntity(entityType, id, document));
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
                docInfo.setEntity(convertToEntity(entityType, id, document));
            }

            if (!noTracking) {
                includedDocumentsById.remove(id);
                documentsById.add(docInfo);
                documentsByEntity.put(docInfo.getEntity(), docInfo);
            }

            return docInfo.getEntity();
        }

        Object entity = convertToEntity(entityType, id, document);

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
     * Converts the json document to an entity.
     */
    public Object convertToEntity(Class entityType, String id, ObjectNode documentFound) {
        return entityToJson.convertToEntity(entityType, id, documentFound);
    }

    private void registerMissingProperties(Object o, String id, Object value) {
        Map<String, Object> map = entityToJson.getMissingDictionary().get(o);
        if (map == null) {
            map = new HashMap<>();
            entityToJson.getMissingDictionary().put(o, map);
        }

        map.put(id, value);
    }

    /**
     * Gets the default value of the specified type.
     */
    public static Object getDefaultValue(Class clazz) {
        return Defaults.defaultValue(clazz);
    }

    /**
     * Marks the specified entity for deletion. The entity will be deleted when SaveChanges is called.
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
        knownMissingIds.add(value.getId());
    }

    /**
     * Marks the specified entity for deletion. The entity will be deleted when IDocumentSession.SaveChanges is called.
     * WARNING: This method will not call beforeDelete listener!
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

        knownMissingIds.add(id);
        changeVector = isUseOptimisticConcurrency() ? changeVector : null;
        defer(new DeleteCommandData(id, Lang.coalesce(expectedChangeVector, changeVector)));
    }

    /**
     * Stores the specified entity in the session. The entity will be saved when SaveChanges is called.
     */
    public void store(Object entity) {
        Reference<String> stringReference = new Reference<>();
        boolean hasId = generateEntityIdOnTheClient.tryGetIdFromInstance(entity, stringReference);
        storeInternal(entity, null, null, hasId == false ? ConcurrencyCheckMode.FORCED : ConcurrencyCheckMode.AUTO);
    }

    /**
     * Stores the specified entity in the session, explicitly specifying its Id. The entity will be saved when SaveChanges is called.
     */
    public void store(Object entity, String id) {
        storeInternal(entity, null, id, ConcurrencyCheckMode.AUTO);
    }

    /**
     * Stores the specified entity in the session, explicitly specifying its Id. The entity will be saved when SaveChanges is called.
     */
    public void store(Object entity, String changeVector, String id) {
        storeInternal(entity, changeVector, id, changeVector == null ? ConcurrencyCheckMode.DISABLED : ConcurrencyCheckMode.FORCED);
    }

    private void storeInternal(Object entity, String changeVector, String id, ConcurrencyCheckMode forceConcurrencyCheck) {
        if (null == entity) {
            throw new IllegalArgumentException("Entity cannot be null");
        }

        DocumentInfo value = documentsByEntity.get(entity);
        if (value != null) {
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

        String tag = _requestExecutor.getConventions().getCollectionName(entity);

        ObjectMapper mapper = JsonExtensions.getDefaultMapper();
        ObjectNode metadata = mapper.createObjectNode();

        if (tag != null) {
            metadata.set(Constants.Documents.Metadata.COLLECTION, mapper.convertValue(tag, JsonNode.class));
        }

        String javaType = _requestExecutor.getConventions().getJavaClassName(entity.getClass());
        if (javaType != null) {
            metadata.set(Constants.Documents.Metadata.RAVEN_JAVA_TYPE, mapper.convertValue(javaType, TextNode.class));
        }

        if (id != null) {
            knownMissingIds.remove(id);
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
            knownMissingIds.remove(id);
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

        deferredCommands.clear();
        deferredCommandsMap.clear();

        prepareForEntitiesDeletion(result, null);
        prepareForEntitiesPuts(result);

        return result;
    }

    private static void updateMetadataModifications(DocumentInfo documentInfo) {
        /* TODO
          if (documentInfo.MetadataInstance == null || ((MetadataAsDictionary)documentInfo.MetadataInstance).Changed == false)
            return;

        if (documentInfo.Metadata.Modifications == null || documentInfo.Metadata.Modifications.Properties.Count == 0)
        {
            documentInfo.Metadata.Modifications = new DynamicJsonValue();
        }
        foreach (var prop in documentInfo.MetadataInstance.Keys)
        {
            documentInfo.Metadata.Modifications[prop] = documentInfo.MetadataInstance[prop];
        }
         */
    }


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
                        // TODO var afterStoreEventArgs = new AfterStoreEventArgs(this, documentInfo.Id, documentInfo.Entity);
                        //TODO: OnAfterStore?.Invoke(this, afterStoreEventArgs);

                        documentsByEntity.remove(documentInfo.getEntity());
                        result.getEntities().add(documentInfo.getEntity());
                    }

                    documentsById.remove(documentInfo.getId());
                }

                changeVector = useOptimisticConcurrency ? changeVector : null;
                BeforeDeleteEventArgs beforeDeleteEventArgs = new BeforeDeleteEventArgs(this, documentInfo.getId(), documentInfo.getEntity());
                EventHelper.invoke(onBeforeDelete, this, beforeDeleteEventArgs);
                result.getSessionCommands().add(new DeleteCommandData(documentInfo.getId(), changeVector));
            }

            deletedEntities.clear();
        }

    }

    private void prepareForEntitiesPuts(SaveChangesData result) {
        for (Map.Entry<Object, DocumentInfo> entity : documentsByEntity.entrySet()) {
            //TODO: updateMetadataModifications(entity.getValue());

            ObjectNode document = entityToJson.convertEntityToJson(entity.getKey(), entity.getValue());

            if (entity.getValue().isIgnoreChanges() || !entityChanged(document, entity.getValue(), null)) {
                continue;
            }

            ICommandData command = result.deferredCommandsMap.get(IdTypeAndName.create(entity.getValue().getId(), CommandType.CLIENT_ANY_COMMAND, null));
            if (command != null) {
                throwInvalidModifiedDocumentWithDeferredCommand(command);
            }
            /* TODO
            var onOnBeforeStore = OnBeforeStore;
            if (onOnBeforeStore != null)
            {
                var beforeStoreEventArgs = new BeforeStoreEventArgs(this, entity.Value.Id, entity.Key);
                onOnBeforeStore(this, beforeStoreEventArgs);
                if (beforeStoreEventArgs.MetadataAccessed)
                    UpdateMetadataModifications(entity.Value);
                if (beforeStoreEventArgs.MetadataAccessed ||
                        EntityChanged(document, entity.Value, null))
                    document = EntityToBlittable.ConvertEntityToBlittable(entity.Key, entity.Value);
            }*/

            entity.getValue().setNewDocument(false);
            result.getEntities().add(entity.getKey());

            if (entity.getValue().getId() != null) {
                documentsById.remove(entity.getValue().getId());
            }

            entity.getValue().setDocument(document);

            String changeVector = useOptimisticConcurrency && entity.getValue().getConcurrencyCheckMode() != ConcurrencyCheckMode.DISABLED
                    || entity.getValue().getConcurrencyCheckMode() == ConcurrencyCheckMode.FORCED
                    ? entity.getValue().getChangeVector() : null;

            result.getSessionCommands().add(new PutCommandDataWithJson(entity.getValue().getId(), changeVector, document));
        }
    }

    private static void throwInvalidModifiedDocumentWithDeferredCommand(ICommandData resultCommand) {
        throw new IllegalStateException("Cannot perform save because document " + resultCommand.getId()
                + " has been deleted by the session and is also taking part in deferred " + resultCommand.getType() + " command");
    }

    private static void throwInvalidDeletedDocumentWithDeferredCommand(ICommandData resultCommand) {
        throw new IllegalStateException("Cannot perform save because document " + resultCommand.getId()
                + " has been deleted by the session and is also taking part in deferred " + resultCommand.getType() + " command");
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
     */
    public boolean hasChanged(Object entity) {
        DocumentInfo documentInfo = documentsByEntity.get(entity);

        if (documentInfo == null) {
            return false;
        }

        ObjectNode document = entityToJson.convertEntityToJson(entity, documentInfo);
        return entityChanged(document, documentInfo, null);
    }

    /* TODO

    public void WaitForReplicationAfterSaveChanges(TimeSpan? timeout = null, bool throwOnTimeout = true,
                                                   int replicas = 1, bool majority = false)
    {
        var realTimeout = timeout ?? TimeSpan.FromSeconds(15);
        if (_saveChangesOptions == null)
            _saveChangesOptions = new BatchOptions();
        _saveChangesOptions.WaitForReplicas = true;
        _saveChangesOptions.Majority = majority;
        _saveChangesOptions.NumberOfReplicasToWaitFor = replicas;
        _saveChangesOptions.WaitForReplicasTimeout = realTimeout;
        _saveChangesOptions.ThrowOnTimeoutInWaitForReplicas = throwOnTimeout;
    }

    public void WaitForIndexesAfterSaveChanges(TimeSpan? timeout = null, bool throwOnTimeout = false,
                                               string[] indexes = null)
    {
        var realTimeout = timeout ?? TimeSpan.FromSeconds(15);
        if (_saveChangesOptions == null)
            _saveChangesOptions = new BatchOptions();
        _saveChangesOptions.WaitForIndexes = true;
        _saveChangesOptions.WaitForIndexesTimeout = realTimeout;
        _saveChangesOptions.ThrowOnTimeoutInWaitForIndexes = throwOnTimeout;
        _saveChangesOptions.WaitForSpecificIndexes = indexes;
    }


    */

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
     */
    public void ignoreChangesFor(Object entity) {
        getDocumentInfo(entity).setIgnoreChanges(true);
    }

    /**
     * Evicts the specified entity from the session.
     * Remove the entity from the delete queue and stops tracking changes for this entity.
     */
    public <T> void evict(T entity) {
        DocumentInfo documentInfo = documentsByEntity.get(entity);
        if (documentInfo != null) {
            documentsByEntity.remove(entity);
            documentsById.remove(documentInfo.getId());
        }

        deletedEntities.remove(entity);
    }

    /**
     * Clears this instance.
     * Remove all entities from the delete queue and stops tracking changes for all entities.
     */
    public void clear() {
        documentsByEntity.clear();
        deletedEntities.clear();
        documentsById.clear();
        knownMissingIds.clear();
        includedDocumentsById.clear();
    }

    /**
     * Defer commands to be executed on saveChanges()
     */
    public void defer(ICommandData command, ICommandData... commands) {
        deferredCommands.add(command);
        deferInternal(command);

        if (commands != null && commands.length > 0)
            defer(commands);
    }

    /**
     * Defer commands to be executed on saveChanges()
     */
    public void defer(ICommandData[] commands) {
        deferredCommands.addAll(Arrays.asList(commands));
        for (ICommandData command : commands) {
            deferInternal(command);
        }
    }

    private void deferInternal(ICommandData command) {
        deferredCommandsMap.put(IdTypeAndName.create(command.getId(), command.getType(), command.getName()), command);
        deferredCommandsMap.put(IdTypeAndName.create(command.getId(), CommandType.CLIENT_ANY_COMMAND, null), command);

        if (!CommandType.ATTACHMENT_PUT.equals(command.getType())) {
            deferredCommandsMap.put(IdTypeAndName.create(command.getId(), CommandType.CLIENT_NOT_ATTACHMENT_PUT, null), command);
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
        knownMissingIds.add(id);
    }

    public void unregisterMissing(String id) {
        knownMissingIds.remove(id);
    }

    public void registerIncludes(ObjectNode includes) {
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
            /* TODO

            if (newDocumentInfo.Metadata.TryGetConflict(out var conflict) && conflict)
                continue;
             */

            includedDocumentsById.put(newDocumentInfo.getId(), newDocumentInfo);
        }
    }

    public void registerMissingIncludes(ArrayNode results, ObjectNode includes, String[] includePaths) {
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

                        /* TODO:
                         var metadata = document.GetMetadata();
                            if (metadata.TryGetConflict(out var conflict) && conflict)
                                return;
                         */
                    }

                    registerMissing(id);
                });
            }
        }
    }

    @Override
    public int hashCode() {
        return _hash;
    }

    /* TODO

    internal void HandleInternalMetadata(BlittableJsonReaderObject result)
    {
        // Implant a property with "id" value ... if it doesn't exist
        BlittableJsonReaderObject metadata;
        string id;
        if (result.TryGet(Constants.Documents.Metadata.Key, out metadata) == false ||
                metadata.TryGet(Constants.Documents.Metadata.Id, out id) == false)
        {
            // if the item doesn't have meta data, then nested items might have, so we need to check them
            var propDetail = new BlittableJsonReaderObject.PropertyDetails();
            for (int index = 0; index < result.Count; index++)
            {
                result.GetPropertyByIndex(index, ref propDetail, addObjectToCache: true);
                var jsonObj = propDetail.Value as BlittableJsonReaderObject;
                if (jsonObj != null)
                {
                    HandleInternalMetadata(jsonObj);
                    continue;
                }

                var jsonArray = propDetail.Value as BlittableJsonReaderArray;
                if (jsonArray != null)
                {
                    HandleInternalMetadata(jsonArray);
                }
            }
            return;
        }

        string entityName;
        if (metadata.TryGet(Constants.Documents.Metadata.Collection, out entityName) == false)
            return;

        var idPropName = Conventions.FindIdentityPropertyNameFromEntityName(entityName);

        result.Modifications = new DynamicJsonValue
        {
                [idPropName] = id // this is being read by BlittableJsonReader for additional properties on the object
        };
    }

    internal void HandleInternalMetadata(BlittableJsonReaderArray values)
    {
        foreach (var nested in values)
        {
            var bObject = nested as BlittableJsonReaderObject;
            if (bObject != null)
                HandleInternalMetadata(bObject);
            var bArray = nested as BlittableJsonReaderArray;
            if (bArray == null)
                continue;
            HandleInternalMetadata(bArray);
        }
    }

    public object DeserializeFromTransformer(Type entityType, string id, BlittableJsonReaderObject document)
    {
        HandleInternalMetadata(document);
        return EntityToBlittable.ConvertToEntity(entityType, id, document);
    }
*/

    public boolean checkIfIdAlreadyIncluded(String[] ids, Map.Entry<String, Class>[] includes) {
        return checkIfIdAlreadyIncluded(ids, Arrays.stream(includes).map(x -> x.getKey()).collect(Collectors.toList()));
    }

    public boolean checkIfIdAlreadyIncluded(String[] ids, Collection<String> includes) {
        for (String id : ids) {
            if (knownMissingIds.contains(id)) {
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
                final boolean[] hasAll = {true}; //using fake arary here to force final keyword on variable

                IncludesUtil.include(documentInfo.getDocument(), include, s -> {
                    hasAll[0] &= isLoaded(s);
                });

                if (!hasAll[0]) {
                    return false;
                }

            }

        }

        return true;
    }

    protected <T> void refreshInternal(T entity, RavenCommand<GetDocumentResult> cmd, DocumentInfo documentInfo) {
        ObjectNode document = (ObjectNode) cmd.getResult().getResults().get(0);
        if (document == null) {
            throw new IllegalStateException("Document '" + documentInfo.getId() + "' no longer exists and was probably deleted");
        }

        ObjectNode value = (ObjectNode) document.get(Constants.Documents.Metadata.KEY);
        documentInfo.setMetadata(value);

        JsonNode changeVector = document.get(Constants.Documents.Metadata.CHANGE_VECTOR); //TODO: metadata here?
        documentInfo.setChangeVector(changeVector.asText());

        documentInfo.setDocument(document);

        documentInfo.setEntity(convertToEntity(entity.getClass(), documentInfo.getId(), document));

        /* TODO

        var type = entity.GetType();
        foreach (var property in ReflectionUtil.GetPropertiesAndFieldsFor(type, BindingFlags.Instance | BindingFlags.Public | BindingFlags.NonPublic))
        {
            var prop = property;
            if (prop.DeclaringType != type && prop.DeclaringType != null)
                prop = prop.DeclaringType.GetProperty(prop.Name, BindingFlags.Instance | BindingFlags.Public | BindingFlags.NonPublic) ?? property;

            if (!prop.CanWrite() || !prop.CanRead() || prop.GetIndexParameters().Length != 0)
                continue;
            prop.SetValue(entity, prop.GetValue(documentInfo.Entity));
        }
         */
    }

    /*TODO

    protected static T GetOperationResult<T>(object result)
    {
        if (result == null)
            return default(T);

        if (result is T)
        return (T)result;

        var resultsArray = result as T[];
        if (resultsArray != null && resultsArray.Length > 0)
            return resultsArray[0];

        var resultsDictionary = result as Dictionary<string, T>;
        if (resultsDictionary != null)
        {
            if (resultsDictionary.Count == 0)
                return default(T);

            if (resultsDictionary.Count == 1)
                return resultsDictionary.Values.FirstOrDefault();
        }

        throw new InvalidCastException($"Unable to cast {result.GetType().Name} to {typeof(T).Name}");
    }

*/
    public void onAfterStoreInvoke(AfterStoreEventArgs afterStoreEventArgs) {
        EventHelper.invoke(onAfterStore, this, afterStoreEventArgs);
    }

    /* TODO

    public void OnBeforeQueryExecutedInvoke(BeforeQueryExecutedEventArgs beforeQueryExecutedEventArgs)
    {
        OnBeforeQueryExecuted?.Invoke(this, beforeQueryExecutedEventArgs);
    }

    protected (string IndexName, string CollectionName) ProcessQueryParameters(Type type, string indexName, string collectionName, DocumentConventions conventions)
    {
        var isIndex = string.IsNullOrWhiteSpace(indexName) == false;
        var isCollection = string.IsNullOrWhiteSpace(collectionName) == false;

        if (isIndex && isCollection)
            throw new InvalidOperationException($"Parameters '{nameof(indexName)}' and '{nameof(collectionName)}' are mutually exclusive. Please specify only one of them.");

        if (isIndex == false && isCollection == false)
            collectionName = Conventions.GetCollectionName(type);

        return (indexName, collectionName);
    }
}

        */
    //TBD public AttachmentName[] GetAttachmentNames(object entity)
    //TBD public void StoreAttachment(string documentId, string name, Stream stream, string contentType = null)
    //TBD public void StoreAttachment(object entity, string name, Stream stream, string contentType = null)
    /* TODO

        protected void ThrowEntityNotInSession(object entity)
        {
            throw new ArgumentException(entity + " is not associated with the session, cannot add attachment to it. " +
                                        "Use documentId instead or track the entity in the session.", nameof(entity));
        }



     */
    //TBD public void DeleteAttachment(object entity, string name)
    //TBD public void DeleteAttachment(string documentId, string name)

    public enum ConcurrencyCheckMode {
        /**
         * Automatic optimistic concurrency check depending on UseOptimisticConcurrency setting or provided Change Vector
         */
        AUTO,

        /**
         * Force optimistic concurrency check even if UseOptimisticConcurrency is not set
         */
        FORCED,

        /**
         * Disable optimistic concurrency check even if UseOptimisticConcurrency is set
         */
        DISABLED
    }

    public static class SaveChangesData {
        private final List<ICommandData> deferredCommands;
        private final Map<IdTypeAndName, ICommandData> deferredCommandsMap;
        private final List<ICommandData> sessionCommands = new ArrayList<>();
        private final List<Object> entities = new ArrayList<>();
        private final BatchOptions options;

        public SaveChangesData(InMemoryDocumentSessionOperations session) {
            deferredCommands = new ArrayList<>(session.deferredCommands);
            deferredCommandsMap = new HashMap<>(session.deferredCommandsMap);
            options = session._saveChangesOptions;
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
    }

}
