package net.ravendb.client.documents.conventions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import net.ravendb.client.Constants;
import net.ravendb.client.exceptions.RavenException;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.http.ReadBalanceBehavior;
import net.ravendb.client.primitives.Lang;
import net.ravendb.client.primitives.Tuple;
import net.ravendb.client.serverwide.ClientConfiguration;
import net.ravendb.client.util.Inflector;
import net.ravendb.client.util.ReflectionUtil;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.util.LangUtils;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.StreamSupport;

public class DocumentConventions {

    public static DocumentConventions defaultConventions = new DocumentConventions();

    private static Map<Class, String> _cachedDefaultTypeCollectionNames = new HashMap<>();

    //TBD: private readonly List<(Type Type, TryConvertValueForQueryDelegate<object> Convert)> _listOfQueryValueConverters = new List<(Type, TryConvertValueForQueryDelegate<object>)>();

    private final List<Tuple<Class, BiFunction<String, Object, String>>> _listOfRegisteredIdConventions = new ArrayList<>();

    private boolean _frozen;
    private ClientConfiguration _originalConfiguration;
    private Map<Class, Field> _idPropertyCache = new HashMap<>();
    private boolean _saveEnumsAsIntegers;
    private String _identityPartsSeparator;
    private boolean _disableTopologyUpdates;

    private Function<PropertyDescriptor, Boolean> _findIdentityProperty;

    private Function<String, String> _transformClassCollectionNameToDocumentIdPrefix;
    private BiFunction<String, Object, String> _documentIdGenerator;
    private Function<String, String> _findIdentityPropertyNameFromEntityName;

    private Function<Class, String> _findCollectionName;

    private Function<Class, String> _findJavaClassName;
    private BiFunction<String, ObjectNode, String> _findJavaClass;

    private boolean _useOptimisticConcurrency;
    private boolean _throwIfQueryPageSizeIsNotSet;
    private int _maxNumberOfRequestsPerSession;

    private ReadBalanceBehavior _readBalanceBehavior;
    private int _maxHttpCacheSize;
    private ObjectMapper _entityMapper;

    public DocumentConventions() {
        _readBalanceBehavior = ReadBalanceBehavior.NONE;
        _findIdentityProperty = q -> q.getName().equals("id");
        _identityPartsSeparator = "/";
        _findIdentityPropertyNameFromEntityName = entityName -> "Id";
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
        _transformClassCollectionNameToDocumentIdPrefix = collectionName -> defaultTransformCollectionNameToDocumentIdPrefix(collectionName);

        _findCollectionName = type -> defaultGetCollectionName(type);

        _maxNumberOfRequestsPerSession = 30;
        _maxHttpCacheSize = 128 * 1024 * 1024; //TODO:
        _entityMapper = JsonExtensions.getDefaultEntityMapper();
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

    public void setReadBalanceBehavior(ReadBalanceBehavior _readBalanceBehavior) {
        this._readBalanceBehavior = _readBalanceBehavior;
    }

    public Object deserializeEntityFromJson(Class documentType, ObjectNode document) {
        try {
            return JsonExtensions.getDefaultMapper().treeToValue(document, documentType);
        } catch (JsonProcessingException e) {
            throw new RavenException("Cannot deserialize entity", e);
        }
    }

    public int getMaxHttpCacheSize() {
        return _maxHttpCacheSize;
    }

    public void setMaxHttpCacheSize(int maxHttpCacheSize) {
        this._maxHttpCacheSize = maxHttpCacheSize;
    }

    public int getMaxNumberOfRequestsPerSession() {
        return _maxNumberOfRequestsPerSession;
    }

    public void setMaxNumberOfRequestsPerSession(int _maxNumberOfRequestsPerSession) {
        this._maxNumberOfRequestsPerSession = _maxNumberOfRequestsPerSession;
    }

    /**
     * If set to 'true' then it will throw an exception when any query is performed (in session)
     * without explicit page size set.
     * This can be useful for development purposes to pinpoint all the possible performance bottlenecks
     * since from 4.0 there is no limitation for number of results returned from server.
     */
    public boolean isThrowIfQueryPageSizeIsNotSet() {
        return _throwIfQueryPageSizeIsNotSet;
    }

    /**
     * If set to 'true' then it will throw an exception when any query is performed (in session)
     * without explicit page size set.
     * This can be useful for development purposes to pinpoint all the possible performance bottlenecks
     * since from 4.0 there is no limitation for number of results returned from server.
     */
    public void setThrowIfQueryPageSizeIsNotSet(boolean _throwIfQueryPageSizeIsNotSet) {
        assertNotFrozen();
        this._throwIfQueryPageSizeIsNotSet = _throwIfQueryPageSizeIsNotSet;
    }

    /**
     * Whether UseOptimisticConcurrency is set to true by default for all opened sessions
     */
    public boolean isUseOptimisticConcurrency() {
        return _useOptimisticConcurrency;
    }

    /**
     * Whether UseOptimisticConcurrency is set to true by default for all opened sessions
     */
    public void setUseOptimisticConcurrency(boolean _useOptimisticConcurrency) {
        this._useOptimisticConcurrency = _useOptimisticConcurrency;
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

    public void setFindJavaClassName(Function<Class, String> _findJavaClassName) {
        assertNotFrozen();
        this._findJavaClassName = _findJavaClassName;
    }

    public Function<Class, String> getFindCollectionName() {
        return _findCollectionName;
    }

    public void setFindCollectionName(Function<Class, String> _findCollectionName) {
        assertNotFrozen();
        this._findCollectionName = _findCollectionName;
    }

    public Function<String, String> getFindIdentityPropertyNameFromEntityName() {
        return _findIdentityPropertyNameFromEntityName;
    }

    public void setFindIdentityPropertyNameFromEntityName(Function<String, String> findIdentityPropertyNameFromEntityName) {
        assertNotFrozen();
        this._findIdentityPropertyNameFromEntityName = findIdentityPropertyNameFromEntityName;
    }

    public BiFunction<String, Object, String> getDocumentIdGenerator() {
        return _documentIdGenerator;
    }

    public void setDocumentIdGenerator(BiFunction<String, Object, String> _documentIdGenerator) {
        assertNotFrozen();
        this._documentIdGenerator = _documentIdGenerator;
    }


    /**
     *  Translates the types collection name to the document id prefix
     */
    public Function<String, String> getTransformClassCollectionNameToDocumentIdPrefix() {
        return _transformClassCollectionNameToDocumentIdPrefix;
    }

    /**
     *  Translates the types collection name to the document id prefix
     */
    public void setTransformClassCollectionNameToDocumentIdPrefix(Function<String, String> _transformClassCollectionNameToDocumentIdPrefix) {
        assertNotFrozen();
        this._transformClassCollectionNameToDocumentIdPrefix = _transformClassCollectionNameToDocumentIdPrefix;
    }

    public Function<PropertyDescriptor, Boolean> getFindIdentityProperty() {
        return _findIdentityProperty;
    }

    public void setFindIdentityProperty(Function<PropertyDescriptor, Boolean> _findIdentityProperty) {
        this._findIdentityProperty = _findIdentityProperty;
    }

    public boolean isDisableTopologyUpdates() {
        return _disableTopologyUpdates;
    }

    public void setDisableTopologyUpdates(boolean _disableTopologyUpdates) {
        assertNotFrozen();
        this._disableTopologyUpdates = _disableTopologyUpdates;
    }

    public String getIdentityPartsSeparator() {
        return _identityPartsSeparator;
    }

    public void setIdentityPartsSeparator(String _identityPartsSeparator) {
        this._identityPartsSeparator = _identityPartsSeparator;
    }

    /**
     * Saves Enums as integers and instruct the Linq provider to query enums as integer values.
     */
    public boolean isSaveEnumsAsIntegers() {
        return _saveEnumsAsIntegers;
    }

    /**
     * Saves Enums as integers and instruct the Linq provider to query enums as integer values.
     */
    public void setSaveEnumsAsIntegers(boolean saveEnumsAsIntegers) {
        assertNotFrozen();
        this._saveEnumsAsIntegers = saveEnumsAsIntegers;
    }

    /**
     *  Default method used when finding a collection name for a type
     */
    public static String defaultGetCollectionName(Class clazz) {
        String result = _cachedDefaultTypeCollectionNames.get(clazz);
        if (result != null) {
            return result;
        }

        result = Inflector.pluralize(clazz.getSimpleName());

        _cachedDefaultTypeCollectionNames.put(clazz, result);

        return result;
    }

    /**
     * Gets the collection name for a given type.
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
     */
    public String getCollectionName(Object entity) {
        if (entity == null) {
            return null;
        }

        return getCollectionName(entity.getClass());
    }

    /**
     * Generates the document id.
     */
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
     */
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

        _listOfRegisteredIdConventions.add(index, Tuple.create(clazz, function));

        return this;
    }

    /**
     * Get the java class (if exists) from the document
     */
    public String getJavaClass(String id, ObjectNode document) {
        return _findJavaClass.apply(id, document);
    }

    /**
     * Get the Java class name to be stored in the entity metadata
     */
    public String getJavaClassName(Class entityType) {
        return _findJavaClassName.apply(entityType);
    }

    /**
     * Clone the current conventions to a new instance
     */
    public DocumentConventions clone() {
        DocumentConventions cloned = new DocumentConventions();
        try {
            BeanUtils.copyProperties(cloned, this);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return cloned;
    }

    private static Field getField(Class<?> clazz, String name) {
        Field field = null;
        while (clazz != null && field == null) {
            try {
                field = clazz.getDeclaredField(name);
            } catch (Exception e) {
            }
            clazz = clazz.getSuperclass();
        }
        return field;
    }

    /**
     *  Gets the identity property.
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
                _maxNumberOfRequestsPerSession = _originalConfiguration.getMaxNumberOfRequestsPerSession();
                _readBalanceBehavior = _originalConfiguration.getReadBalanceBehavior();

                _originalConfiguration = null;
                return;
            }

            if (_originalConfiguration == null) {
                _originalConfiguration = new ClientConfiguration();
                _originalConfiguration.setEtag(-1);
                _originalConfiguration.setMaxNumberOfRequestsPerSession(_maxNumberOfRequestsPerSession);
                _originalConfiguration.setReadBalanceBehavior(_readBalanceBehavior);
            }

            _maxNumberOfRequestsPerSession = Lang.coalesce(configuration.getMaxNumberOfRequestsPerSession(), _originalConfiguration.getMaxNumberOfRequestsPerSession());
            _readBalanceBehavior = Lang.coalesce(configuration.getReadBalanceBehavior(), _originalConfiguration.getReadBalanceBehavior());
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

    /* TBD
        public void RegisterQueryValueConverter<T>(TryConvertValueForQueryDelegate<T> converter)
        {
            AssertNotFrozen();

            int index;
            for (index = 0; index < _listOfQueryValueConverters.Count; index++)
            {
                var entry = _listOfQueryValueConverters[index];
                if (entry.Type.IsAssignableFrom(typeof(T)))
                    break;
            }

            _listOfQueryValueConverters.Insert(index, (typeof(T), Actual));

            bool Actual(string name, object value, bool forRange, out string strValue)
            {
                if (value is T)
                    return converter(name, (T)value, forRange, out strValue);
                strValue = null;
                return false;
            }
        }

        public bool TryConvertValueForQuery(string fieldName, object value, bool forRange, out string strValue)
        {
            foreach (var queryValueConverter in _listOfQueryValueConverters)
            {
                if (queryValueConverter.Type.IsInstanceOfType(value) == false)
                    continue;

                return queryValueConverter.Convert(fieldName, value, forRange, out strValue);
            }

            strValue = null;
            return false;
        }
     */

    public void freeze() {
        _frozen = true;
    }


    private void assertNotFrozen() {
        if (_frozen) {
            throw new IllegalStateException("Conventions has been frozen after documentStore.initialize() and no changes can be applied to them");
        }
    }
}
