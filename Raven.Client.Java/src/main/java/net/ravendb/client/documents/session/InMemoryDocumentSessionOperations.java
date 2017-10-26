package net.ravendb.client.documents.session;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Defaults;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.DocumentStoreBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.IdTypeAndName;
import net.ravendb.client.documents.commands.batches.*;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.identity.GenerateEntityIdOnTheClient;
import net.ravendb.client.documents.session.operations.lazy.ILazyOperation;
import net.ravendb.client.exceptions.documents.session.NonUniqueObjectException;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.http.CurrentIndexAndNode;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.JsonOperation;
import net.ravendb.client.primitives.CleanCloseable;
import net.ravendb.client.primitives.Lang;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.util.IdentityHashSet;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import sun.plugin.dom.exception.InvalidStateException;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

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

    /* TODO:

        public event EventHandler<BeforeStoreEventArgs> OnBeforeStore;
        public event EventHandler<AfterStoreEventArgs> OnAfterStore;
        public event EventHandler<BeforeDeleteEventArgs> OnBeforeDelete;
        public event EventHandler<BeforeQueryExecutedEventArgs> OnBeforeQueryExecuted;

*/
    //Entities whose id we already know do not exists, because they are a missing include, or a missing load, etc.
    protected final Set<String> knownMissingIds = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

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
    protected final Map<String, DocumentInfo> includedDocumentsById = new TreeMap<>(String::compareToIgnoreCase);

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

        this.useOptimisticConcurrency = requestExecutor.getConventions().getUseOptimisticConcurrency();
        this.maxNumberOfRequestsPerSession = requestExecutor.getConventions().getMaxNumberOfRequestsPerSession();
        this.generateEntityIdOnTheClient = new GenerateEntityIdOnTheClient(_requestExecutor.getConventions(), this::generateId);
        this.entityToJson = new EntityToJson(this);

        sessionInfo = new SessionInfo(_clientSessionId);
    }

    /* TODO:

        /// <summary>
        /// Gets the metadata for the specified entity.
        /// </summary>
        /// <typeparam name="T"></typeparam>
        /// <param name="instance">The instance.</param>
        /// <returns></returns>
        public IMetadataDictionary GetMetadataFor<T>(T instance)
        {
            if (instance == null)
                throw new ArgumentNullException(nameof(instance));

            var documentInfo = GetDocumentInfo(instance);

            if (documentInfo.MetadataInstance != null)
                return documentInfo.MetadataInstance;

            var metadataAsBlittable = documentInfo.Metadata;
            var metadata = new MetadataAsDictionary(metadataAsBlittable);
            documentInfo.MetadataInstance = metadata;
            return metadata;
        }*/

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

        /* TODO:

            DocumentInfo docInfo;

            if (IncludedDocumentsById.TryGetValue(id, out docInfo))
            {
                if (docInfo.Entity == null)
                    docInfo.Entity = ConvertToEntity(entityType, id, document);

                if (noTracking == false)
                {
                    IncludedDocumentsById.Remove(id);
                    DocumentsById.Add(docInfo);
                    DocumentsByEntity[docInfo.Entity] = docInfo;
                }
                return docInfo.Entity;
            }*/


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

    /* TODO
        private void RegisterMissingProperties(object o, string id, object value)
        {
            Dictionary<string, object> dictionary;
            if (EntityToBlittable.MissingDictionary.TryGetValue(o, out dictionary) == false)
            {
                EntityToBlittable.MissingDictionary[o] = dictionary = new Dictionary<string, object>();
            }

            dictionary[id] = value;
        }
*/

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
            throw new InvalidStateException("Can't store document, there is a deferred command registered for this document in the session. Document id: " + id);
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

    /* TODO:

    protected internal async Task<string> GenerateDocumentIdForStorageAsync(object entity)
    {
        if (entity is IDynamicMetaObjectProvider)
        {
            if (GenerateEntityIdOnTheClient.TryGetIdFromDynamic(entity, out string id))
                return id;

            id = await GenerateIdAsync(entity).ConfigureAwait(false);
            // If we generated a new id, store it back into the Id field so the client has access to it
            if (id != null)
                GenerateEntityIdOnTheClient.TrySetIdOnDynamic(entity, id);
            return id;
        }

        var result = await GetOrGenerateDocumentIdAsync(entity).ConfigureAwait(false);
        GenerateEntityIdOnTheClient.TrySetIdentity(entity, result);
        return result;
    }

*/
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

    /* TODO

    protected async Task<string> GetOrGenerateDocumentIdAsync(object entity)
    {
        string id;
        GenerateEntityIdOnTheClient.TryGetIdFromInstance(entity, out id);

        Task<string> generator =
                id != null
                        ? Task.FromResult(id)
                        : GenerateIdAsync(entity);

        var result = await generator.ConfigureAwait(false);
        if (result != null && result.StartsWith("/"))
            throw new InvalidOperationException("Cannot use value '" + id + "' as a document id because it begins with a '/'");

        return result;
    }
*/
    public SaveChangesData prepareForSaveChanges() {
        SaveChangesData result = new SaveChangesData(this);

        deferredCommands.clear();
        deferredCommandsMap.clear();

        prepareForEntitiesDeletion(result, null);
        prepareForEntitiesPuts(result);

        return result;
    }

    /* TODO:

    private static void UpdateMetadataModifications(DocumentInfo documentInfo)
    {
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
    }
    */

    private void prepareForEntitiesDeletion(SaveChangesData result, Map<String, DocumentsChanges> changes) {
        /* TODO
         foreach (var deletedEntity in DeletedEntities)
        {
            if (DocumentsByEntity.TryGetValue(deletedEntity, out DocumentInfo documentInfo) == false)
                continue;

            if (changes != null)
            {
                var docChanges = new List<DocumentsChanges>();
                var change = new DocumentsChanges
                {
                    FieldNewValue = string.Empty,
                            FieldOldValue = string.Empty,
                            Change = DocumentsChanges.ChangeType.DocumentDeleted
                };

                docChanges.Add(change);
                changes[documentInfo.Id] = docChanges.ToArray();
            }
            else
            {
                if (result.DeferredCommandsDictionary.TryGetValue((documentInfo.Id, CommandType.ClientAnyCommand, null), out ICommandData command))
                ThrowInvalidDeletedDocumentWithDeferredCommand(command);

                string changeVector = null;
                if (DocumentsById.TryGetValue(documentInfo.Id, out documentInfo))
                {
                    changeVector = documentInfo.ChangeVector;

                    if (documentInfo.Entity != null)
                    {
                        var afterStoreEventArgs = new AfterStoreEventArgs(this, documentInfo.Id, documentInfo.Entity);
                        OnAfterStore?.Invoke(this, afterStoreEventArgs);

                        DocumentsByEntity.Remove(documentInfo.Entity);
                        result.Entities.Add(documentInfo.Entity);
                    }

                    DocumentsById.Remove(documentInfo.Id);
                }
                changeVector = UseOptimisticConcurrency ? changeVector : null;
                var beforeDeleteEventArgs = new BeforeDeleteEventArgs(this, documentInfo.Id, documentInfo.Entity);
                OnBeforeDelete?.Invoke(this, beforeDeleteEventArgs);
                result.SessionCommands.Add(new DeleteCommandData(documentInfo.Id, changeVector));
            }
        }
        DeletedEntities.Clear();
         */
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

    /* TODO:

    public IDictionary<string, DocumentsChanges[]> WhatChanged()
    {
        var changes = new Dictionary<string, DocumentsChanges[]>();

        PrepareForEntitiesDeletion(null, changes);
        GetAllEntitiesChanges(changes);
        return changes;
    }

    /// <summary>
    /// Gets a value indicating whether any of the entities tracked by the session has changes.
    /// </summary>
    /// <value></value>
    public bool HasChanges
    {
        get
        {
            foreach (var entity in DocumentsByEntity)
            {
                var document = EntityToBlittable.ConvertEntityToBlittable(entity.Key, entity.Value);
                if (EntityChanged(document, entity.Value, null))
                {
                    return true;
                }
            }
            return DeletedEntities.Count > 0;
        }
    }

    /// <summary>
    /// Determines whether the specified entity has changed.
    /// </summary>
    /// <param name="entity">The entity.</param>
    /// <returns>
    /// 	<c>true</c> if the specified entity has changed; otherwise, <c>false</c>.
    /// </returns>
    public bool HasChanged(object entity)
    {
        DocumentInfo documentInfo;
        if (DocumentsByEntity.TryGetValue(entity, out documentInfo) == false)
            return false;
        var document = EntityToBlittable.ConvertEntityToBlittable(entity, documentInfo);
        return EntityChanged(document, documentInfo, null);
    }

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

    private void GetAllEntitiesChanges(IDictionary<string, DocumentsChanges[]> changes)
    {
        foreach (var pair in DocumentsById)
        {
            UpdateMetadataModifications(pair.Value);
            var newObj = EntityToBlittable.ConvertEntityToBlittable(pair.Value.Entity, pair.Value);
            EntityChanged(newObj, pair.Value, changes);
        }
    }

    /// <summary>
    /// Mark the entity as one that should be ignore for change tracking purposes,
    /// it still takes part in the session, but is ignored for SaveChanges.
    /// </summary>
    public void IgnoreChangesFor(object entity)
    {
        GetDocumentInfo(entity).IgnoreChanges = true;
    }
    */

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

    /*


    public void RegisterMissing(string id)
    {
        KnownMissingIds.Add(id);
    }

    public void UnregisterMissing(string id)
    {
        KnownMissingIds.Remove(id);
    }

    internal void RegisterIncludes(BlittableJsonReaderObject includes)
    {
        if (includes == null)
            return;

        var propertyDetails = new BlittableJsonReaderObject.PropertyDetails();
        foreach (var propertyIndex in includes.GetPropertiesByInsertionOrder())
        {
            includes.GetPropertyByIndex(propertyIndex, ref propertyDetails);

            if (propertyDetails.Value == null)
                continue;

            var json = (BlittableJsonReaderObject)propertyDetails.Value;

            var newDocumentInfo = DocumentInfo.GetNewDocumentInfo(json);
            if (newDocumentInfo.Metadata.TryGetConflict(out var conflict) && conflict)
                continue;

            IncludedDocumentsById[newDocumentInfo.Id] = newDocumentInfo;
        }
    }

    public void RegisterMissingIncludes(BlittableJsonReaderArray results, BlittableJsonReaderObject includes, ICollection<string> includePaths)
    {
        if (includePaths == null || includePaths.Count == 0)
            return;

        foreach (BlittableJsonReaderObject result in results)
        {
            foreach (var include in includePaths)
            {
                if (include == Constants.Documents.Indexing.Fields.DocumentIdFieldName)
                    continue;

                IncludesUtil.Include(result, include, id =>
                        {
                if (id == null)
                    return;

                if (IsLoaded(id))
                    return;

                if (includes.TryGet(id, out BlittableJsonReaderObject document))
                {
                    var metadata = document.GetMetadata();
                    if (metadata.TryGetConflict(out var conflict) && conflict)
                        return;
                }

                RegisterMissing(id);
                    });
            }
        }
    }

    public override int GetHashCode()
    {
        return _hash;
    }

    public override bool Equals(object obj)
    {
        return ReferenceEquals(obj, this);
    }

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

    public bool CheckIfIdAlreadyIncluded(string[] ids, KeyValuePair<string, Type>[] includes)
    {
        return CheckIfIdAlreadyIncluded(ids, includes.Select(x => x.Key));
    }

    public bool CheckIfIdAlreadyIncluded(string[] ids, IEnumerable<string> includes)
    {
        foreach (var id in ids)
        {
            if (KnownMissingIds.Contains(id))
                continue;

            // Check if document was already loaded, the check if we've received it through include
            if (DocumentsById.TryGetValue(id, out DocumentInfo documentInfo) == false &&
                    IncludedDocumentsById.TryGetValue(id, out documentInfo) == false)
                return false;

            if (documentInfo.Entity == null)
                return false;

            if (includes == null)
                continue;

            foreach (var include in includes)
            {
                var hasAll = true;
                IncludesUtil.Include(documentInfo.Document, include, s =>
                        {
                                hasAll &= IsLoaded(s);
                    });

                if (hasAll == false)
                    return false;
            }
        }
        return true;
    }

    protected void RefreshInternal<T>(T entity, RavenCommand<GetDocumentResult> cmd, DocumentInfo documentInfo)
    {
        var document = (BlittableJsonReaderObject)cmd.Result.Results[0];
        if (document == null)
            throw new InvalidOperationException("Document '" + documentInfo.Id +
                    "' no longer exists and was probably deleted");

        document.TryGetMember(Constants.Documents.Metadata.Key, out object value);
        documentInfo.Metadata = value as BlittableJsonReaderObject;

        document.TryGetMember(Constants.Documents.Metadata.ChangeVector, out var changeVector);
        documentInfo.ChangeVector = changeVector as string;

        documentInfo.Document = document;

        documentInfo.Entity = ConvertToEntity(typeof(T), documentInfo.Id, document);

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
    }

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


    public void OnAfterStoreInvoke(AfterStoreEventArgs afterStoreEventArgs)
    {
        OnAfterStore?.Invoke(this, afterStoreEventArgs);
    }

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


  public AttachmentName[] GetAttachmentNames(object entity)
        {
            if (entity == null ||
                DocumentsByEntity.TryGetValue(entity, out DocumentInfo document) == false ||
                document.Metadata.TryGet(Constants.Documents.Metadata.Attachments, out BlittableJsonReaderArray attachments) == false)
                return Array.Empty<AttachmentName>();

            var results = new AttachmentName[attachments.Length];
            for (var i = 0; i < attachments.Length; i++)
            {
                var attachment = (BlittableJsonReaderObject)attachments[i];
                results[i] = JsonDeserializationClient.AttachmentName(attachment);
            }
            return results;
        }

        public void StoreAttachment(string documentId, string name, Stream stream, string contentType = null)
        {
            if (string.IsNullOrWhiteSpace(documentId))
                throw new ArgumentNullException(nameof(documentId));
            if (string.IsNullOrWhiteSpace(name))
                throw new ArgumentNullException(nameof(name));

            if (DeferredCommandsDictionary.ContainsKey((documentId, CommandType.DELETE, null)))
                throw new InvalidOperationException($"Can't store attachment {name} of document {documentId}, there is a deferred command registered for this document to be deleted.");

            if (DeferredCommandsDictionary.ContainsKey((documentId, CommandType.AttachmentPUT, name)))
                throw new InvalidOperationException($"Can't store attachment {name} of document {documentId}, there is a deferred command registered to create an attachment with the same name.");

            if (DeferredCommandsDictionary.ContainsKey((documentId, CommandType.AttachmentDELETE, name)))
                throw new InvalidOperationException($"Can't store attachment {name} of document {documentId}, there is a deferred command registered to delete an attachment with the same name.");

            if (DocumentsById.TryGetValue(documentId, out DocumentInfo documentInfo) &&
                DeletedEntities.Contains(documentInfo.Entity))
                throw new InvalidOperationException($"Can't store attachment {name} of document {documentId}, the document was already deleted in this session.");

            Defer(new PutAttachmentCommandData(documentId, name, stream, contentType, null));
        }

        public void StoreAttachment(object entity, string name, Stream stream, string contentType = null)
        {
            if (DocumentsByEntity.TryGetValue(entity, out DocumentInfo document) == false)
                ThrowEntityNotInSession(entity);

            StoreAttachment(document.Id, name, stream, contentType);
        }

        protected void ThrowEntityNotInSession(object entity)
        {
            throw new ArgumentException(entity + " is not associated with the session, cannot add attachment to it. " +
                                        "Use documentId instead or track the entity in the session.", nameof(entity));
        }

        public void DeleteAttachment(object entity, string name)
        {
            if (DocumentsByEntity.TryGetValue(entity, out DocumentInfo document) == false)
                ThrowEntityNotInSession(entity);

            DeleteAttachment(document.Id, name);
        }

        public void DeleteAttachment(string documentId, string name)
        {
            if (string.IsNullOrWhiteSpace(documentId))
                throw new ArgumentNullException(nameof(documentId));
            if (string.IsNullOrWhiteSpace(name))
                throw new ArgumentNullException(nameof(name));

            if (DeferredCommandsDictionary.ContainsKey((documentId, CommandType.DELETE, null)) ||
                DeferredCommandsDictionary.ContainsKey((documentId, CommandType.AttachmentDELETE, name)))
                return; // no-op

            if (DocumentsById.TryGetValue(documentId, out DocumentInfo documentInfo) &&
                DeletedEntities.Contains(documentInfo.Entity))
                return; // no-op

            if (DeferredCommandsDictionary.ContainsKey((documentId, CommandType.AttachmentPUT, name)))
                throw new InvalidOperationException($"Can't delete attachment {name} of document {documentId}, there is a deferred command registered to create an attachment with the same name.");

            Defer(new DeleteAttachmentCommandData(documentId, name, null));
        }
    }

     */

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
