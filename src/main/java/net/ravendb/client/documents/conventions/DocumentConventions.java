package net.ravendb.client.documents.conventions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.operations.configuration.ClientConfiguration;
import net.ravendb.client.exceptions.RavenException;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.http.AggressiveCacheMode;
import net.ravendb.client.http.AggressiveCacheOptions;
import net.ravendb.client.http.LoadBalanceBehavior;
import net.ravendb.client.http.ReadBalanceBehavior;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.primitives.Tuple;
import net.ravendb.client.util.Inflector;
import net.ravendb.client.util.ReflectionUtil;
import org.apache.commons.lang3.ObjectUtils;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class DocumentConventions {

    public static final DocumentConventions defaultConventions = new DocumentConventions();
    public static final DocumentConventions defaultForServerConventions = new DocumentConventions();

    static {
        defaultConventions.freeze();
        defaultForServerConventions._sendApplicationIdentifier = false;
        defaultForServerConventions.freeze();
    }

    private static final Map<Class, String> _cachedDefaultTypeCollectionNames = new HashMap<>();

    private final List<Tuple<Class, IValueForQueryConverter<Object>>> _listOfQueryValueToObjectConverters = new ArrayList<>();

    private List<Tuple<Class, BiFunction<String, Object, String>>> _listOfRegisteredIdConventions = new ArrayList<>();

    private boolean _frozen;
    private ClientConfiguration _originalConfiguration;
    private final Map<Class, Field> _idPropertyCache = new HashMap<>();
    private boolean _saveEnumsAsIntegers;
    private char _identityPartsSeparator;
    private boolean _disableTopologyUpdates;

    private Boolean _disableAtomicDocumentWritesInClusterWideTransaction;
    private IShouldIgnoreEntityChanges _shouldIgnoreEntityChanges;
    private Function<PropertyDescriptor, Boolean> _findIdentityProperty;

    private Function<String, String> _transformClassCollectionNameToDocumentIdPrefix;
    private BiFunction<String, Object, String> _documentIdGenerator;
    private Function<String, String> _findIdentityPropertyNameFromCollectionName;
    private Function<String, String> _loadBalancerPerSessionContextSelector;

    private Function<Class, String> _findCollectionName;

    private Function<Class, String> _findJavaClassName;
    private BiFunction<String, ObjectNode, String> _findJavaClass;
    private Function<String, Class> _findJavaClassByName;

    private boolean _useOptimisticConcurrency;
    private boolean _throwIfQueryPageSizeIsNotSet;
    private int _maxNumberOfRequestsPerSession;

    private Duration _requestTimeout;
    private Duration _firstBroadcastAttemptTimeout;
    private Duration _secondBroadcastAttemptTimeout;
    private Duration _waitForIndexesAfterSaveChangesTimeout;
    private Duration _waitForReplicationAfterSaveChangesTimeout;
    private Duration _waitForNonStaleResultsTimeout;

    private int _loadBalancerContextSeed;
    private LoadBalanceBehavior _loadBalanceBehavior;
    private ReadBalanceBehavior _readBalanceBehavior;
    private int _maxHttpCacheSize;
    private ObjectMapper _entityMapper;
    private Boolean _useCompression;
    private boolean _sendApplicationIdentifier;

    private final BulkInsertConventions _bulkInsert;

    private final AggressiveCacheConventions _aggressiveCache;

    public AggressiveCacheConventions aggressiveCache() {
        return _aggressiveCache;
    }

    public static class AggressiveCacheConventions {
        private final DocumentConventions _conventions;
        private final AggressiveCacheOptions _aggressiveCacheOptions;

        public AggressiveCacheConventions(DocumentConventions conventions) {
            _conventions = conventions;
            _aggressiveCacheOptions = new AggressiveCacheOptions(Duration.ofDays(1), AggressiveCacheMode.TRACK_CHANGES);
        }

        public Duration getDuration() {
            return _aggressiveCacheOptions.getDuration();
        }

        public void setDuration(Duration duration) {
            _aggressiveCacheOptions.setDuration(duration);
        }

        public AggressiveCacheMode getMode() {
            return _aggressiveCacheOptions.getMode();
        }

        public void setMode(AggressiveCacheMode mode) {
            _aggressiveCacheOptions.setMode(mode);
        }
    }

    public BulkInsertConventions bulkInsert() {
        return _bulkInsert;
    }

    public static class BulkInsertConventions {
        private final DocumentConventions _conventions;
        private int _timeSeriesBatchSize;

        public BulkInsertConventions(DocumentConventions conventions) {
            _conventions = conventions;
            _timeSeriesBatchSize = 1024;
        }

        public int getTimeSeriesBatchSize() {
            return _timeSeriesBatchSize;
        }

        public void setTimeSeriesBatchSize(int batchSize) {
            _conventions.assertNotFrozen();

            if (batchSize <= 0) {
                throw new IllegalArgumentException("BatchSize must be positive");
            }
            _timeSeriesBatchSize = batchSize;
        }

    }

    public DocumentConventions() {
        _readBalanceBehavior = ReadBalanceBehavior.NONE;
        _findIdentityProperty = q -> q.getName().equals("id");
        _identityPartsSeparator = '/';
        _findIdentityPropertyNameFromCollectionName = entityName -> "Id";
        _findJavaClass = (String id, ObjectNode doc) -> {
            JsonNode metadata = doc.get(Constants.Documents.Metadata.KEY);
            if (metadata != null) {
                TextNode javaType = (TextNode) metadata.get(Constants.Documents.Metadata.RAVEN_JAVA_TYPE);
                if (javaType != null) {
                    return javaType.asText();
                }
            }

            return null;
        };
        _findJavaClassName = type -> ReflectionUtil.getFullNameWithoutVersionInformation(type);
        _findJavaClassByName = name -> {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException e) {
                throw new RavenException("Unable to find class by name = " + name, e);
            }
        };
        _transformClassCollectionNameToDocumentIdPrefix = collectionName -> defaultTransformCollectionNameToDocumentIdPrefix(collectionName);

        _findCollectionName = type -> defaultGetCollectionName(type);

        _maxNumberOfRequestsPerSession = 30;
        _bulkInsert = new BulkInsertConventions(this);
        _maxHttpCacheSize = 128 * 1024 * 1024;

        _entityMapper = JsonExtensions.getDefaultEntityMapper();

        _aggressiveCache = new AggressiveCacheConventions(this);
        _firstBroadcastAttemptTimeout = Duration.ofSeconds(5);
        _secondBroadcastAttemptTimeout = Duration.ofSeconds(30);

        _waitForIndexesAfterSaveChangesTimeout = Duration.ofSeconds(15);
        _waitForReplicationAfterSaveChangesTimeout = Duration.ofSeconds(15);
        _waitForNonStaleResultsTimeout = Duration.ofSeconds(15);

        _sendApplicationIdentifier = true;
    }

    public boolean hasExplicitlySetCompressionUsage() {
        return _useCompression != null;
    }

    public Duration getRequestTimeout() {
        return _requestTimeout;
    }

    public void setRequestTimeout(Duration requestTimeout) {
        assertNotFrozen();
        _requestTimeout = requestTimeout;
    }

    /**
     * Enables sending a unique application identifier to the RavenDB Server that is used for Client API usage tracking.
     * It allows RavenDB Server to issue performance hint notifications e.g. during robust topology update requests which could indicate Client API misuse impacting the overall performance
     * @return if option is enabled
     */
    public boolean isSendApplicationIdentifier() {
        return _sendApplicationIdentifier;
    }

    /**
     * Enables sending a unique application identifier to the RavenDB Server that is used for Client API usage tracking.
     * It allows RavenDB Server to issue performance hint notifications e.g. during robust topology update requests which could indicate Client API misuse impacting the overall performance
     * @param sendApplicationIdentifier if option should be enabled
     */
    public void setSendApplicationIdentifier(boolean sendApplicationIdentifier) {
        assertNotFrozen();
        _sendApplicationIdentifier = sendApplicationIdentifier;
    }

    /**
     * Get the timeout for the second broadcast attempt.
     * Default: 30 seconds
     *
     * Upon failure of the first attempt the request executor will resend the command to all nodes simultaneously.
     * @return broadcast timeout
     */
    public Duration getSecondBroadcastAttemptTimeout() {
        return _secondBroadcastAttemptTimeout;
    }

    /**
     * Set the timeout for the second broadcast attempt.
     * Default: 30 seconds
     *
     * Upon failure of the first attempt the request executor will resend the command to all nodes simultaneously.
     * @param secondBroadcastAttemptTimeout broadcast timeout
     */
    public void setSecondBroadcastAttemptTimeout(Duration secondBroadcastAttemptTimeout) {
        assertNotFrozen();
        _secondBroadcastAttemptTimeout = secondBroadcastAttemptTimeout;
    }

    /**
     * Get the timeout for the first broadcast attempt.
     * Default: 5 seconds
     *
     * First attempt will send a single request to a selected node.
     * @return broadcast timeout
     */
    public Duration getFirstBroadcastAttemptTimeout() {
        return _firstBroadcastAttemptTimeout;
    }

    /**
     * Set the timeout for the first broadcast attempt.
     * Default: 5 seconds
     *
     * First attempt will send a single request to a selected node.
     * @param firstBroadcastAttemptTimeout broadcast timeout
     */
    public void setFirstBroadcastAttemptTimeout(Duration firstBroadcastAttemptTimeout) {
        assertNotFrozen();
        _firstBroadcastAttemptTimeout = firstBroadcastAttemptTimeout;
    }

    /**
     * Get the wait for indexes after save changes timeout
     * Default: 15 seconds
     * @return wait timeout
     */
    public Duration getWaitForIndexesAfterSaveChangesTimeout() {
        return _waitForIndexesAfterSaveChangesTimeout;
    }

    /**
     * Set the wait for indexes after save changes timeout
     * Default: 15 seconds
     * @param waitForIndexesAfterSaveChangesTimeout wait timeout
     */
    public void setWaitForIndexesAfterSaveChangesTimeout(Duration waitForIndexesAfterSaveChangesTimeout) {
        assertNotFrozen();
        _waitForIndexesAfterSaveChangesTimeout = waitForIndexesAfterSaveChangesTimeout;
    }

    /**
     * Get the default timeout for DocumentSession waitForNonStaleResults methods.
     * Default: 15 seconds
     * @return wait timeout
     */
    public Duration getWaitForNonStaleResultsTimeout() {
        return _waitForNonStaleResultsTimeout;
    }

    /**
     * Sets the default timeout for DocumentSession waitForNonStaleResults methods.
     * @param waitForNonStaleResultsTimeout wait timeout
     */
    public void setWaitForNonStaleResultsTimeout(Duration waitForNonStaleResultsTimeout) {
        assertNotFrozen();
        _waitForNonStaleResultsTimeout = waitForNonStaleResultsTimeout;
    }

    /**
     * Gets the default timeout for DocumentSession.advanced().waitForReplicationAfterSaveChanges method.
     * @return wait timeout
     */
    public Duration getWaitForReplicationAfterSaveChangesTimeout() {
        return _waitForReplicationAfterSaveChangesTimeout;
    }

    /**
     * Sets the default timeout for DocumentSession.advanced().waitForReplicationAfterSaveChanges method.
     * @param waitForReplicationAfterSaveChangesTimeout wait timeout
     */
    public void setWaitForReplicationAfterSaveChangesTimeout(Duration waitForReplicationAfterSaveChangesTimeout) {
        assertNotFrozen();
        _waitForReplicationAfterSaveChangesTimeout = waitForReplicationAfterSaveChangesTimeout;
    }

    public Boolean isUseCompression() {
        if (_useCompression == null) {
            return true;
        }
        return _useCompression;
    }

    public void setUseCompression(Boolean useCompression) {
        assertNotFrozen();
        _useCompression = useCompression;
    }

    public ObjectMapper getEntityMapper() {
        return _entityMapper;
    }

    public void setEntityMapper(ObjectMapper entityMapper) {
        _entityMapper = entityMapper;
    }

    public ReadBalanceBehavior getReadBalanceBehavior() {
        return _readBalanceBehavior;
    }

    public void setReadBalanceBehavior(ReadBalanceBehavior readBalanceBehavior) {
        assertNotFrozen();
        _readBalanceBehavior = readBalanceBehavior;
    }

    public int getLoadBalancerContextSeed() {
        return _loadBalancerContextSeed;
    }

    public void setLoadBalancerContextSeed(int seed) {
        assertNotFrozen();
        _loadBalancerContextSeed = seed;
    }

    /**
     * We have to make this check so if admin activated this, but client code did not provide the selector,
     * it is still disabled. Relevant if we have multiple clients / versions at once.
     * @return load balance behavior
     */
    public LoadBalanceBehavior getLoadBalanceBehavior() {
        return _loadBalanceBehavior;
    }

    public void setLoadBalanceBehavior(LoadBalanceBehavior loadBalanceBehavior) {
        assertNotFrozen();
        _loadBalanceBehavior = loadBalanceBehavior;
    }

    /**
     * @return Gets the function that allow to specialize the topology
     *  selection for a particular session. Used in load balancing
     *  scenarios
     */
    public Function<String, String> getLoadBalancerPerSessionContextSelector() {
        return _loadBalancerPerSessionContextSelector;
    }

    /**
     * Sets the function that allow to specialize the topology
     *  selection for a particular session. Used in load balancing
     *  scenarios
     * @param loadBalancerPerSessionContextSelector selector to use
     */
    public void setLoadBalancerPerSessionContextSelector(Function<String, String> loadBalancerPerSessionContextSelector) {
        _loadBalancerPerSessionContextSelector = loadBalancerPerSessionContextSelector;
    }

    public int getMaxHttpCacheSize() {
        return _maxHttpCacheSize;
    }

    public void setMaxHttpCacheSize(int maxHttpCacheSize) {
        assertNotFrozen();
        this._maxHttpCacheSize = maxHttpCacheSize;
    }

    public int getMaxNumberOfRequestsPerSession() {
        return _maxNumberOfRequestsPerSession;
    }

    public void setMaxNumberOfRequestsPerSession(int maxNumberOfRequestsPerSession) {
        assertNotFrozen();
        _maxNumberOfRequestsPerSession = maxNumberOfRequestsPerSession;
    }

    /**
     * If set to 'true' then it will throw an exception when any query is performed (in session)
     * without explicit page size set.
     * This can be useful for development purposes to pinpoint all the possible performance bottlenecks
     * since from 4.0 there is no limitation for number of results returned from server.
     * @return true if should we throw if page size is not set
     */
    public boolean isThrowIfQueryPageSizeIsNotSet() {
        return _throwIfQueryPageSizeIsNotSet;
    }

    /**
     * If set to 'true' then it will throw an exception when any query is performed (in session)
     * without explicit page size set.
     * This can be useful for development purposes to pinpoint all the possible performance bottlenecks
     * since from 4.0 there is no limitation for number of results returned from server.
     * @param throwIfQueryPageSizeIsNotSet value to set
     */
    public void setThrowIfQueryPageSizeIsNotSet(boolean throwIfQueryPageSizeIsNotSet) {
        assertNotFrozen();
        this._throwIfQueryPageSizeIsNotSet = throwIfQueryPageSizeIsNotSet;
    }

    /**
     * Whether UseOptimisticConcurrency is set to true by default for all opened sessions
     * @return true if optimistic concurrency is enabled
     */
    public boolean isUseOptimisticConcurrency() {
        return _useOptimisticConcurrency;
    }

    /**
     * Whether UseOptimisticConcurrency is set to true by default for all opened sessions
     * @param useOptimisticConcurrency value to set
     */
    public void setUseOptimisticConcurrency(boolean useOptimisticConcurrency) {
        assertNotFrozen();
        this._useOptimisticConcurrency = useOptimisticConcurrency;
    }

    public BiFunction<String, ObjectNode, String> getFindJavaClass() {
        return _findJavaClass;
    }

    public void setFindJavaClass(BiFunction<String, ObjectNode, String> _findJavaClass) {
        assertNotFrozen();
        this._findJavaClass = _findJavaClass;
    }

    public Function<Class, String> getFindJavaClassName() {
        return _findJavaClassName;
    }

    public void setFindJavaClassName(Function<Class, String> findJavaClassName) {
        assertNotFrozen();
        _findJavaClassName = findJavaClassName;
    }

    public Function<Class, String> getFindCollectionName() {
        return _findCollectionName;
    }

    public Function<String, Class> getFindJavaClassByName() {
        return _findJavaClassByName;
    }

    public void setFindJavaClassByName(Function<String, Class> findJavaClassByName) {
        _findJavaClassByName = findJavaClassByName;
    }

    public void setFindCollectionName(Function<Class, String> findCollectionName) {
        assertNotFrozen();
        _findCollectionName = findCollectionName;
    }

    public Function<String, String> getFindIdentityPropertyNameFromCollectionName() {
        return _findIdentityPropertyNameFromCollectionName;
    }

    public void setFindIdentityPropertyNameFromCollectionName(Function<String, String> findIdentityPropertyNameFromCollectionName) {
        assertNotFrozen();
        this._findIdentityPropertyNameFromCollectionName = findIdentityPropertyNameFromCollectionName;
    }

    public BiFunction<String, Object, String> getDocumentIdGenerator() {
        return _documentIdGenerator;
    }

    public void setDocumentIdGenerator(BiFunction<String, Object, String> documentIdGenerator) {
        assertNotFrozen();
        _documentIdGenerator = documentIdGenerator;
    }


    /**
     *  Translates the types collection name to the document id prefix
     *  @return translation function
     */
    public Function<String, String> getTransformClassCollectionNameToDocumentIdPrefix() {
        return _transformClassCollectionNameToDocumentIdPrefix;
    }

    /**
     *  Translates the types collection name to the document id prefix
     *  @param transformClassCollectionNameToDocumentIdPrefix value to set
     */
    public void setTransformClassCollectionNameToDocumentIdPrefix(Function<String, String> transformClassCollectionNameToDocumentIdPrefix) {
        assertNotFrozen();
        this._transformClassCollectionNameToDocumentIdPrefix = transformClassCollectionNameToDocumentIdPrefix;
    }

    public Function<PropertyDescriptor, Boolean> getFindIdentityProperty() {
        return _findIdentityProperty;
    }

    public void setFindIdentityProperty(Function<PropertyDescriptor, Boolean> findIdentityProperty) {
        assertNotFrozen();
        this._findIdentityProperty = findIdentityProperty;
    }

    public IShouldIgnoreEntityChanges getShouldIgnoreEntityChanges() {
        return _shouldIgnoreEntityChanges;
    }

    public void setShouldIgnoreEntityChanges(IShouldIgnoreEntityChanges shouldIgnoreEntityChanges) {
        assertNotFrozen();
        _shouldIgnoreEntityChanges = shouldIgnoreEntityChanges;
    }

    public boolean isDisableTopologyUpdates() {
        return _disableTopologyUpdates;
    }

    public void setDisableTopologyUpdates(boolean disableTopologyUpdates) {
        assertNotFrozen();
        _disableTopologyUpdates = disableTopologyUpdates;
    }

    public char getIdentityPartsSeparator() {
        return _identityPartsSeparator;
    }

    public void setIdentityPartsSeparator(char identityPartsSeparator) {
        assertNotFrozen();

        if (identityPartsSeparator == '|') {
            throw new IllegalArgumentException("Cannot set identity parts separator to '|'");
        }

        _identityPartsSeparator = identityPartsSeparator;
    }

    /**
     * Saves Enums as integers and instruct the Linq provider to query enums as integer values.
     * @return true if we should save enums as integers
     */
    public boolean isSaveEnumsAsIntegers() {
        return _saveEnumsAsIntegers;
    }

    /**
     * Saves Enums as integers and instruct the Linq provider to query enums as integer values.
     * @param saveEnumsAsIntegers value to set
     */
    public void setSaveEnumsAsIntegers(boolean saveEnumsAsIntegers) {
        assertNotFrozen();
        this._saveEnumsAsIntegers = saveEnumsAsIntegers;
    }

    /**
     *  Default method used when finding a collection name for a type
     *  @param clazz Class
     *  @return default collection name for class
     */
    public static String defaultGetCollectionName(Class clazz) {
        String result = _cachedDefaultTypeCollectionNames.get(clazz);
        if (result != null) {
            return result;
        }

        // we want to reject queries and other operations on abstract types, because you usually
        // want to use them for polymorphic queries, and that require the conventions to be
        // applied properly, so we reject the behavior and hint to the user explicitly
        if (clazz.isInterface()) {
            throw new IllegalStateException("Cannot find collection name for interface " + clazz.getName() + ", only concrete classes are supported. Did you forget to customize conventions.findCollectionName?");
        }
        if (Modifier.isAbstract(clazz.getModifiers())) {
            throw new IllegalStateException("Cannot find collection name for abstract class " + clazz.getName() + ", only concrete class are supported. Did you forget to customize conventions.findCollectionName?");
        }

        result = Inflector.pluralize(clazz.getSimpleName());

        _cachedDefaultTypeCollectionNames.put(clazz, result);

        return result;
    }

    /**
     * Gets the collection name for a given type.
     * @param clazz Class
     * @return collection name
     */
    public String getCollectionName(Class clazz) {
        String collectionName = _findCollectionName.apply(clazz);

        if (collectionName != null) {
            return collectionName;
        }

        return defaultGetCollectionName(clazz);
    }

    /**
     * Gets the collection name for a given type.
     * @param entity entity to get collection name
     * @return collection name
     */
    public String getCollectionName(Object entity) {
        if (entity == null) {
            return null;
        }

        return getCollectionName(entity.getClass());
    }

    /**
     * Generates the document id.
     * @param databaseName Database name
     * @param entity Entity
     * @return document id
     */
    @SuppressWarnings("unchecked")
    public String generateDocumentId(String databaseName, Object entity) {
        Class<?> clazz = entity.getClass();

        for (Tuple<Class, BiFunction<String, Object, String>> listOfRegisteredIdConvention : _listOfRegisteredIdConventions) {
            if (listOfRegisteredIdConvention.first.isAssignableFrom(clazz)) {
                return listOfRegisteredIdConvention.second.apply(databaseName, entity);
            }
        }

        return _documentIdGenerator.apply(databaseName, entity);
    }

    /**
     * Register an id convention for a single type (and all of its derived types.
     * Note that you can still fall back to the DocumentIdGenerator if you want.
     * @param <TEntity> Entity class
     * @param clazz Class
     * @param function Function to use
     * @return document conventions
     */
    @SuppressWarnings("unchecked")
    public <TEntity> DocumentConventions registerIdConvention(Class<TEntity> clazz, BiFunction<String, TEntity, String> function) {
        assertNotFrozen();

        _listOfRegisteredIdConventions.stream()
                .filter(x -> x.first.equals(clazz))
                .findFirst()
                .ifPresent(x -> _listOfRegisteredIdConventions.remove(x));

        int index;
        for (index = 0; index < _listOfRegisteredIdConventions.size(); index++) {
            Tuple<Class, BiFunction<String, Object, String>> entry = _listOfRegisteredIdConventions.get(index);
            if (entry.first.isAssignableFrom(clazz)) {
                break;
            }
        }

        _listOfRegisteredIdConventions.add(index, Tuple.create(clazz, (BiFunction<String, Object, String>) function));

        return this;
    }

    /**
     * Get the java class (if exists) from the document
     * @param id document id
     * @param document document to get java class from
     * @return java class
     */
    public String getJavaClass(String id, ObjectNode document) {
        return _findJavaClass.apply(id, document);
    }

    /**
     * Get the class instance by it's name
     * @param name class name
     * @return java class
     */
    public Class getJavaClassByName(String name) {
        return _findJavaClassByName.apply(name);
    }

    /**
     * Get the Java class name to be stored in the entity metadata
     * @param entityType Entity type
     * @return java class name
     */
    public String getJavaClassName(Class entityType) {
        return _findJavaClassName.apply(entityType);
    }

    /**
     * EXPERT: Disable automatic atomic writes with cluster write transactions. If set to 'true', will only consider explicitly
     * added compare exchange values to validate cluster wide transactions.
     * @return disable atomic writes
     */
    public Boolean getDisableAtomicDocumentWritesInClusterWideTransaction() {
        return _disableAtomicDocumentWritesInClusterWideTransaction;
    }

    /**
     * EXPERT: Disable automatic atomic writes with cluster write transactions. If set to 'true', will only consider explicitly
     * added compare exchange values to validate cluster wide transactions.
     * @param disableAtomicDocumentWritesInClusterWideTransaction disable atomic writes
     */
    public void setDisableAtomicDocumentWritesInClusterWideTransaction(Boolean disableAtomicDocumentWritesInClusterWideTransaction) {
        _disableAtomicDocumentWritesInClusterWideTransaction = disableAtomicDocumentWritesInClusterWideTransaction;
    }

    /**
     * Clone the current conventions to a new instance
     */
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public DocumentConventions clone() {
        DocumentConventions cloned = new DocumentConventions();
        cloned._listOfRegisteredIdConventions = new ArrayList<>(_listOfRegisteredIdConventions);
        cloned._frozen = _frozen;
        cloned._shouldIgnoreEntityChanges = _shouldIgnoreEntityChanges;
        cloned._originalConfiguration = _originalConfiguration;
        cloned._saveEnumsAsIntegers = _saveEnumsAsIntegers;
        cloned._identityPartsSeparator = _identityPartsSeparator;
        cloned._disableTopologyUpdates = _disableTopologyUpdates;
        cloned._findIdentityProperty = _findIdentityProperty;
        cloned._transformClassCollectionNameToDocumentIdPrefix = _transformClassCollectionNameToDocumentIdPrefix;
        cloned._documentIdGenerator = _documentIdGenerator;
        cloned._findIdentityPropertyNameFromCollectionName = _findIdentityPropertyNameFromCollectionName;
        cloned._findCollectionName = _findCollectionName;
        cloned._findJavaClassName = _findJavaClassName;
        cloned._findJavaClass = _findJavaClass;
        cloned._findJavaClassByName = _findJavaClassByName;
        cloned._useOptimisticConcurrency = _useOptimisticConcurrency;
        cloned._throwIfQueryPageSizeIsNotSet = _throwIfQueryPageSizeIsNotSet;
        cloned._maxNumberOfRequestsPerSession = _maxNumberOfRequestsPerSession;
        cloned._loadBalancerPerSessionContextSelector = _loadBalancerPerSessionContextSelector;
        cloned._readBalanceBehavior = _readBalanceBehavior;
        cloned._loadBalanceBehavior = _loadBalanceBehavior;
        cloned._maxHttpCacheSize = _maxHttpCacheSize;
        cloned._entityMapper = _entityMapper;
        cloned._useCompression = _useCompression;
        return cloned;
    }

    private static Field getField(Class<?> clazz, String name) {
        Field field = null;
        while (clazz != null && field == null) {
            try {
                field = clazz.getDeclaredField(name);
            } catch (Exception ignored) {
            }
            clazz = clazz.getSuperclass();
        }
        return field;
    }

    /**
     *  Gets the identity property.
     *  @param clazz Class of entity
     *  @return Identity property (field)
     */
    public Field getIdentityProperty(Class clazz) {
        Field info = _idPropertyCache.get(clazz);
        if (info != null) {
            return info;
        }

        try {
            Field idField = Arrays.stream(Introspector.getBeanInfo(clazz).getPropertyDescriptors())
                    .filter(x -> _findIdentityProperty.apply(x))
                    .findFirst()
                    .map(x -> getField(clazz, x.getName()))
                    .orElse(null);

            _idPropertyCache.put(clazz, idField);

            return idField;
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateFrom(ClientConfiguration configuration) {
        if (configuration == null) {
            return;
        }

        synchronized (this) {
            if (configuration.isDisabled() && _originalConfiguration == null) { // nothing to do
                return;
            }

            if (configuration.isDisabled() && _originalConfiguration != null) { // need to revert to original values
                _maxNumberOfRequestsPerSession = ObjectUtils.firstNonNull(_originalConfiguration.getMaxNumberOfRequestsPerSession(), _maxNumberOfRequestsPerSession);
                _readBalanceBehavior = ObjectUtils.firstNonNull(_originalConfiguration.getReadBalanceBehavior(), _readBalanceBehavior);
                _identityPartsSeparator = ObjectUtils.firstNonNull(_originalConfiguration.getIdentityPartsSeparator(), _identityPartsSeparator);
                _loadBalanceBehavior = ObjectUtils.firstNonNull(_originalConfiguration.getLoadBalanceBehavior(), _loadBalanceBehavior);
                _loadBalancerContextSeed = ObjectUtils.firstNonNull(_originalConfiguration.getLoadBalancerContextSeed(), _loadBalancerContextSeed);

                _originalConfiguration = null;
                return;
            }

            if (_originalConfiguration == null) {
                _originalConfiguration = new ClientConfiguration();
                _originalConfiguration.setEtag(-1);
                _originalConfiguration.setMaxNumberOfRequestsPerSession(_maxNumberOfRequestsPerSession);
                _originalConfiguration.setReadBalanceBehavior(_readBalanceBehavior);
                _originalConfiguration.setIdentityPartsSeparator(_identityPartsSeparator);
                _originalConfiguration.setLoadBalanceBehavior(_loadBalanceBehavior);
                _originalConfiguration.setLoadBalancerContextSeed(_loadBalancerContextSeed);
            }

            _maxNumberOfRequestsPerSession = ObjectUtils.firstNonNull(
                    configuration.getMaxNumberOfRequestsPerSession(),
                    _originalConfiguration.getMaxNumberOfRequestsPerSession(),
                    _maxNumberOfRequestsPerSession);
            _readBalanceBehavior = ObjectUtils.firstNonNull(
                    configuration.getReadBalanceBehavior(),
                    _originalConfiguration.getReadBalanceBehavior(),
                    _readBalanceBehavior
            );
            _loadBalanceBehavior = ObjectUtils.firstNonNull(
                    configuration.getLoadBalanceBehavior(),
                    _originalConfiguration.getLoadBalanceBehavior(),
                    _loadBalanceBehavior
            );
            _loadBalancerContextSeed = ObjectUtils.firstNonNull(
                    configuration.getLoadBalancerContextSeed(),
                    _originalConfiguration.getLoadBalancerContextSeed(),
                    _loadBalancerContextSeed
            );
            _identityPartsSeparator = ObjectUtils.firstNonNull(
                    configuration.getIdentityPartsSeparator(),
                    _originalConfiguration.getIdentityPartsSeparator(),
                    _identityPartsSeparator);
        }
    }

    public static String defaultTransformCollectionNameToDocumentIdPrefix(String collectionName) {
        long upperCount = collectionName.chars()
                .filter(x -> Character.isUpperCase(x))
                .count();


        if (upperCount <= 1) {
            return collectionName.toLowerCase();
        }

        // multiple capital letters, so probably something that we want to preserve caps on.
        return collectionName;
    }

    @SuppressWarnings("unchecked")
    public <T> void registerQueryValueConverter(Class<T> clazz, IValueForQueryConverter<T> converter) {
        assertNotFrozen();

        int index;
        for (index = 0; index < _listOfQueryValueToObjectConverters.size(); index++) {
            Tuple<Class, IValueForQueryConverter<Object>> entry = _listOfQueryValueToObjectConverters.get(index);
            if (entry.first.isAssignableFrom(clazz)) {
                break;
            }
        }

        _listOfQueryValueToObjectConverters.add(index, Tuple.create(clazz, (fieldName, value, forRange, stringValue) -> {
            if (clazz.isInstance(value)) {
                return converter.tryConvertValueForQuery(fieldName, (T) value, forRange, stringValue);
            }
            stringValue.value = null;
            return false;
        }));
    }

    public boolean tryConvertValueToObjectForQuery(String fieldName, Object value, boolean forRange, Reference<Object> strValue) {
        for (Tuple<Class, IValueForQueryConverter<Object>> queryValueConverter : _listOfQueryValueToObjectConverters) {
            if (!queryValueConverter.first.isInstance(value)) {
                continue;
            }

            return queryValueConverter.second.tryConvertValueForQuery(fieldName, value, forRange, strValue);
        }

        strValue.value = null;
        return false;
    }

    public void freeze() {
        _frozen = true;
    }

    private void assertNotFrozen() {
        if (_frozen) {
            throw new IllegalStateException("Conventions has been frozen after documentStore.initialize() and no changes can be applied to them");
        }
    }
}
