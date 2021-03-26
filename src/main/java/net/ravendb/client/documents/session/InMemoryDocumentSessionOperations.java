package net.ravendb.client.documents.session;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Defaults;
import com.google.common.collect.Iterators;
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
import net.ravendb.client.exceptions.documents.session.NonUniqueObjectException;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.BatchCommandResult;
import net.ravendb.client.json.JsonOperation;
import net.ravendb.client.json.MetadataAsDictionary;
import net.ravendb.client.primitives.*;
import net.ravendb.client.util.IdentityHashSet;
import net.ravendb.client.util.IdentityLinkedHashMap;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@SuppressWarnings("SameParameterValue")
public abstract class InMemoryDocumentSessionOperations implements CleanCloseable {

    protected final RequestExecutor _requestExecutor;

    private OperationExecutor _operationExecutor;

    private static final AtomicInteger _instancesCounter = new AtomicInteger();

    private final int _hash = _instancesCounter.incrementAndGet();
    protected final boolean generateDocumentKeysOnStore = true;
    protected final SessionInfo sessionInfo;

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
        return getSessionInfo().getCurrentSessionNode(_requestExecutor);
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
    public final DocumentsByEntityHolder documentsByEntity = new DocumentsByEntityHolder();

    /**
     * The entities waiting to be deleted
     */
    public final DeletedEntitiesHolder deletedEntities = new DeletedEntitiesHolder();




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

    public SessionInfo getSessionInfo() {
        return sessionInfo;
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

        this.maxNumberOfRequestsPerSession = _requestExecutor.getConventions().getMaxNumberOfRequestsPerSession();
        this.generateEntityIdOnTheClient = new GenerateEntityIdOnTheClient(_requestExecutor.getConventions(), this::generateId);
        this.entityToJson = new EntityToJson(this);

        sessionInfo = new SessionInfo(this, options, _documentStore);
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
            return deserializeFromTransformer(entityType, null, document, false);
        }

        DocumentInfo docInfo = documentsById.getValue(id);
        if (docInfo != null) {
            // the local instance may have been changed, we adhere to the current Unit of Work
            // instance, and return that, ignoring anything new.

            if (docInfo.getEntity() == null) {
                docInfo.setEntity(entityToJson.convertToEntity(entityType, id, document, !noTracking));
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
                docInfo.setEntity(entityToJson.convertToEntity(entityType, id, document, !noTracking));
            }

            if (!noTracking) {
                includedDocumentsById.remove(id);
                documentsById.add(docInfo);
                documentsByEntity.put(docInfo.getEntity(), docInfo);
            }

            return docInfo.getEntity();
        }

        Object entity = entityToJson.convertToEntity(entityType, id, document, !noTracking);

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
        changeVector = null;
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
            throw new IllegalStateException("Can't store object, it was already deleted in this session. Document id: " + id);
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
        if (id != null) {
            _knownMissingIds.remove(id);
        }

        DocumentInfo documentInfo = new DocumentInfo();
        documentInfo.setId(id);
        documentInfo.setMetadata(metadata);
        documentInfo.setChangeVector(changeVector);
        documentInfo.setEntity(entity);
        documentInfo.setNewDocument(true);
        documentInfo.setDocument(null);

        documentsByEntity.put(entity, documentInfo);

        if (id != null) {
            documentsById.add(documentInfo);
        }
    }

    protected void assertNoNonUniqueInstance(Object entity, String id) {
        if (StringUtils.isEmpty(id)
                || id.charAt(id.length() - 1) == '|'
                || id.charAt(id.length() - 1) == getConventions().getIdentityPartsSeparator()) {
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
        try (CleanCloseable deletes = deletedEntities.prepareEntitiesDeletes()) {

            for (DeletedEntitiesHolder.DeletedEntitiesEnumeratorResult deletedEntity : deletedEntities) {
                DocumentInfo documentInfo = documentsByEntity.get(deletedEntity.entity);
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

                    changeVector = null;
                    result.getSessionCommands().add(new DeleteCommandData(documentInfo.getId(), changeVector));
                }

                if (changes == null) {
                    result.onSuccess.clearDeletedEntities();
                }
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void prepareForEntitiesPuts(SaveChangesData result) {
        try (CleanCloseable putsContext = documentsByEntity.prepareEntitiesPuts()) {

            for (DocumentsByEntityHolder.DocumentsByEntityEnumeratorResult entity : documentsByEntity) {

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

                result.getEntities().add(entity.getKey());

                if (entity.getValue().getId() != null) {
                    result.onSuccess.removeDocumentById(entity.getValue().getId());
                }

                result.onSuccess.updateEntityDocumentInfo(entity.getValue(), document);

                String changeVector;
                changeVector = null;

                result.getSessionCommands().add(new PutCommandDataWithJson(entity.getValue().getId(), changeVector, document));
            }
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

        getAllEntitiesChanges(changes);
        prepareForEntitiesDeletion(null, changes);

        return changes;
    }


    /**
     * Gets a value indicating whether any of the entities tracked by the session has changes.
     *
     * @return true if session has changes
     */
    public boolean hasChanges() {
        for (DocumentsByEntityHolder.DocumentsByEntityEnumeratorResult entity : documentsByEntity) {
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
     * Clears this instance.
     * Remove all entities from the delete queue and stops tracking changes for all entities.
     */
    public void clear() {
        documentsByEntity.clear();
        deletedEntities.clear();
        documentsById.clear();
        _knownMissingIds.clear();
        deferredCommands.clear();
        deferredCommandsMap.clear();
        entityToJson.clear();
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

        addCommand(command, command.getId(), command.getType(), command.getName());
    }

    private void addCommand(ICommandData command, String id, CommandType commandType, String commandName) {
        deferredCommandsMap.put(IdTypeAndName.create(id, commandType, commandName), command);
        deferredCommandsMap.put(IdTypeAndName.create(id, CommandType.CLIENT_ANY_COMMAND, null), command);


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

    public void registerMissing(String[] ids) {
        if (noTracking) {
            return;
        }

        _knownMissingIds.addAll(Arrays.asList(ids));
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




    @Override
    public int hashCode() {
        return _hash;
    }

    private Object deserializeFromTransformer(Class<?> clazz, String id, ObjectNode document, boolean trackEntity) {
        return entityToJson.convertToEntity(clazz, id, document, trackEntity);
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

            if (documentInfo.getEntity() == null && documentInfo.getDocument() == null) {
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


    protected void updateSessionAfterSaveChanges(BatchCommandResult result) {
        Long returnedTransactionIndex = result.getTransactionIndex();
        _documentStore.setLastTransactionIndex(getDatabaseName(), returnedTransactionIndex);
        sessionInfo.setLastClusterTransactionIndex(returnedTransactionIndex);
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
        private final ActionsToRunOnSuccess onSuccess;

        public SaveChangesData(InMemoryDocumentSessionOperations session) {
            deferredCommands = new ArrayList<>(session.deferredCommands);
            deferredCommandsMap = new HashMap<>(session.deferredCommandsMap);
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


        public Map<IdTypeAndName, ICommandData> getDeferredCommandsMap() {
            return deferredCommandsMap;
        }

        public static class ActionsToRunOnSuccess {

            private final InMemoryDocumentSessionOperations _session;
            private final List<String> _documentsByIdToRemove = new ArrayList<>();
            private final List<Object> _documentsByEntityToRemove = new ArrayList<>();
            private final List<Tuple<DocumentInfo, ObjectNode>> _documentInfosToUpdate = new ArrayList<>();

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

                _session.deferredCommands.clear();
                _session.deferredCommandsMap.clear();
            }

            public void clearDeletedEntities() {
                _clearDeletedEntities = true;
            }
        }
    }





    public static class DocumentsByEntityHolder implements Iterable<DocumentsByEntityHolder.DocumentsByEntityEnumeratorResult> {
        private final Map<Object, DocumentInfo> _documentsByEntity = new IdentityLinkedHashMap<>();

        private Map<Object, DocumentInfo> _onBeforeStoreDocumentsByEntity;

        private boolean _prepareEntitiesPuts;

        public int size() {
            return _documentsByEntity.size() + (_onBeforeStoreDocumentsByEntity != null ? _onBeforeStoreDocumentsByEntity.size() : 0);
        }

        public void remove(Object entity) {
            _documentsByEntity.remove(entity);
            if (_onBeforeStoreDocumentsByEntity != null) {
                _onBeforeStoreDocumentsByEntity.remove(entity);
            }
        }



        public void put(Object entity, DocumentInfo documentInfo) {
            if (!_prepareEntitiesPuts) {
                _documentsByEntity.put(entity, documentInfo);
                return;
            }

            createOnBeforeStoreDocumentsByEntityIfNeeded();
            _onBeforeStoreDocumentsByEntity.put(entity, documentInfo);
        }

        private void createOnBeforeStoreDocumentsByEntityIfNeeded() {
            if (_onBeforeStoreDocumentsByEntity != null) {
                return ;
            }

            _onBeforeStoreDocumentsByEntity = new IdentityLinkedHashMap<>();
        }

        public void clear() {
            _documentsByEntity.clear();
            if (_onBeforeStoreDocumentsByEntity != null) {
                _onBeforeStoreDocumentsByEntity.clear();
            }
        }

        public DocumentInfo get(Object entity) {
            DocumentInfo documentInfo = _documentsByEntity.get(entity);
            if (documentInfo != null) {
                return documentInfo;
            }

            if (_onBeforeStoreDocumentsByEntity != null) {
                return _onBeforeStoreDocumentsByEntity.get(entity);
            }

            return null;
        }

        @Override
        public Iterator<DocumentsByEntityEnumeratorResult> iterator() {
            Iterator<DocumentsByEntityEnumeratorResult> firstIterator
                    = Iterators.transform(_documentsByEntity.entrySet().iterator(),
                        x -> new DocumentsByEntityEnumeratorResult(x.getKey(), x.getValue(), true));

            if (_onBeforeStoreDocumentsByEntity == null) {
                return firstIterator;
            }

            Iterator<DocumentsByEntityEnumeratorResult> secondIterator
                    = Iterators.transform(_onBeforeStoreDocumentsByEntity.entrySet().iterator(),
                        x -> new DocumentsByEntityEnumeratorResult(x.getKey(), x.getValue(), false));

            return Iterators.concat(firstIterator, secondIterator);
        }

        @Override
        public Spliterator<DocumentsByEntityEnumeratorResult> spliterator() {
            return Spliterators.spliterator(iterator(), size(), Spliterator.ORDERED);
        }

        public CleanCloseable prepareEntitiesPuts() {
            _prepareEntitiesPuts = true;

            return () -> _prepareEntitiesPuts = false;
        }

        public static class DocumentsByEntityEnumeratorResult {
            private Object key;
            private DocumentInfo value;
            private boolean executeOnBeforeStore;

            public DocumentsByEntityEnumeratorResult(Object key, DocumentInfo value, boolean executeOnBeforeStore) {
                this.key = key;
                this.value = value;
                this.executeOnBeforeStore = executeOnBeforeStore;
            }

            public Object getKey() {
                return key;
            }

            public DocumentInfo getValue() {
                return value;
            }

            public boolean isExecuteOnBeforeStore() {
                return executeOnBeforeStore;
            }

        }

    }

    public static class DeletedEntitiesHolder implements Iterable<DeletedEntitiesHolder.DeletedEntitiesEnumeratorResult> {
        private final Set<Object> _deletedEntities = new IdentityHashSet<>();

        private Set<Object> _onBeforeDeletedEntities;

        private boolean _prepareEntitiesDeletes;

        public boolean isEmpty() {
            return size() == 0;
        }

        public int size() {
            return _deletedEntities.size() + (_onBeforeDeletedEntities != null ? _onBeforeDeletedEntities.size() : 0);
        }

        public void add(Object entity) {
            if (_prepareEntitiesDeletes) {
                if (_onBeforeDeletedEntities == null) {
                    _onBeforeDeletedEntities = new IdentityHashSet<>();
                }

                _onBeforeDeletedEntities.add(entity);
                return;
            }

            _deletedEntities.add(entity);
        }

        public void remove(Object entity) {
            _deletedEntities.remove(entity);
            if (_onBeforeDeletedEntities != null) {
                _onBeforeDeletedEntities.remove(entity);
            }
        }



        public boolean contains(Object entity) {
            if (_deletedEntities.contains(entity)) {
                return true;
            }

            if (_onBeforeDeletedEntities == null) {
                return false;
            }

            return _onBeforeDeletedEntities.contains(entity);
        }

        public void clear() {
            _deletedEntities.clear();
            if (_onBeforeDeletedEntities != null) {
                _onBeforeDeletedEntities.clear();
            }
        }

        @Override
        public Iterator<DeletedEntitiesEnumeratorResult> iterator() {
            Iterator<Object> deletedIterator = _deletedEntities.iterator();
            Iterator<DeletedEntitiesEnumeratorResult> deletedTransformedIterator
                    = Iterators.transform(deletedIterator, x -> new DeletedEntitiesEnumeratorResult(x, true));

            if (_onBeforeDeletedEntities == null) {
                return deletedTransformedIterator;
            }

            Iterator<DeletedEntitiesEnumeratorResult> onBeforeDeletedIterator
                    = Iterators.transform(_deletedEntities.iterator(), x -> new DeletedEntitiesEnumeratorResult(x, false));

            return Iterators.concat(deletedTransformedIterator, onBeforeDeletedIterator);
        }

        @Override
        public Spliterator<DeletedEntitiesEnumeratorResult> spliterator() {
            return Spliterators.spliterator(iterator(), size(), Spliterator.ORDERED);
        }

        public CleanCloseable prepareEntitiesDeletes() {
            _prepareEntitiesDeletes = true;

            return () -> _prepareEntitiesDeletes = false;
        }

        public static class DeletedEntitiesEnumeratorResult {
            private Object entity;
            private boolean executeOnBeforeDelete;

            public DeletedEntitiesEnumeratorResult(Object entity, boolean executeOnBeforeDelete) {
                this.entity = entity;
                this.executeOnBeforeDelete = executeOnBeforeDelete;
            }

            public Object getEntity() {
                return entity;
            }

            public boolean isExecuteOnBeforeDelete() {
                return executeOnBeforeDelete;
            }

        }
    }
}
