package net.ravendb.client.documents.conventions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import net.ravendb.client.Constants;
import net.ravendb.client.exceptions.RavenException;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.http.ReadBalanceBehavior;
import net.ravendb.client.primitives.Lang;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.StreamSupport;

public class DocumentConventions {

    public static DocumentConventions defaultConventions = new DocumentConventions();

    private static Map<Class, String> _cachedDefaultTypeCollectionNames = new HashMap<>();

    //TODO: private readonly List<Tuple<Type, Func<string, object, Task<string>>>> _listOfRegisteredIdConventionsAsync = new List<Tuple<Type, Func<string, object, Task<string>>>>();
    //TODO: private readonly List<Tuple<Type, Func<ValueType, string>>> _listOfRegisteredIdLoadConventions = new List<Tuple<Type, Func<ValueType, string>>>();

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

        //TODO FindPropertyNameForIndex = (indexedType, indexedName, path, prop) => (path + prop).Replace("[].", "_").Replace(".", "_");
        //TODO FindPropertyNameForDynamicIndex = (indexedType, indexedName, path, prop) => path + prop;

        _maxNumberOfRequestsPerSession = 30;

        /* TODO

            JsonContractResolver = new DefaultRavenContractResolver();
            CustomizeJsonSerializer = serializer => { }; // todo: remove this or merge with SerializeEntityToJsonStream
            SerializeEntityToJsonStream = (entity, streamWriter) =>
            {
                var jsonSerializer = CreateSerializer();
                jsonSerializer.Serialize(streamWriter, entity);
                streamWriter.Flush();
            };
        }*/

        _deserializeEntityFromJson = (clazz, json) -> deserializeEntityFromJson(clazz, json);
    }



    private boolean _frozen;
    private ClientConfiguration _originalConfiguration;
    private Map<Class, Field> _idPropertyCache = new HashMap<>();
    //TODO: private bool _saveEnumsAsIntegers;
    private String _identityPartsSeparator;
    private boolean _disableTopologyUpdates;

    private Function<PropertyDescriptor, Boolean> _findIdentityProperty;

    private Function<String, String> _transformClassCollectionNameToDocumentIdPrefix;
    private BiFunction<String, Object, String> _documentIdGenerator;
    private Function<String, String> _findIdentityPropertyNameFromEntityName;
    //TODO: private Func<Type, string, string, string, string> _findPropertyNameForIndex;

    private Function<Class, String> _findCollectionName;

    //TODO: private IContractResolver _jsonContractResolver;

    private Function<Class, String> _findJavaClassName;
    private BiFunction<String, ObjectNode, String> _findJavaClass;

    private boolean _useOptimisticConcurrency;
    private boolean _throwIfQueryPageSizeIsNotSet;
    private int _maxNumberOfRequestsPerSession;

    //TODO: private Action<JsonSerializer> _customizeJsonSerializer;
    //TODO: private Action<object, StreamWriter> _serializeEntityToJsonStream;
    private ReadBalanceBehavior _readBalanceBehavior;
    private BiFunction<Class, ObjectNode, Object> _deserializeEntityFromJson;

    public BiFunction<Class, ObjectNode, Object> getDeserializeEntityFromJson() {
        return _deserializeEntityFromJson;
    }

    public void setDeserializeEntityFromJson(BiFunction<Class, ObjectNode, Object> _deserializeEntityFromJson) {
        assertNotFrozen();
        this._deserializeEntityFromJson = _deserializeEntityFromJson;
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
/* TODO

        public Action<object, StreamWriter> SerializeEntityToJsonStream
        {
            get => _serializeEntityToJsonStream;
            set
            {
                AssertNotFrozen();
                _serializeEntityToJsonStream = value;
            }
        }

        /// <summary>
        ///     Register an action to customize the json serializer used by the <see cref="DocumentStore" />
        /// </summary>
        public Action<JsonSerializer> CustomizeJsonSerializer
        {
            get => _customizeJsonSerializer;
            set
            {
                AssertNotFrozen();
                _customizeJsonSerializer = value;
            }
        }

*/

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
    /* TODO

        /// <summary>
        ///     Gets or sets the json contract resolver.
        /// </summary>
        /// <value>The json contract resolver.</value>
        public IContractResolver JsonContractResolver
        {
            get => _jsonContractResolver;
            set
            {
                AssertNotFrozen();
                _jsonContractResolver = value;
            }
        }
     */

    public Function<Class, String> getFindCollectionName() {
        return _findCollectionName;
    }

    public void setFindCollectionName(Function<Class, String> _findCollectionName) {
        assertNotFrozen();
        this._findCollectionName = _findCollectionName;
    }

    /*

        /// <summary>
        ///     Gets or sets the function to find the indexed property name
        ///     given the indexed document type, the index name, the current path and the property path.
        /// </summary>
        public Func<Type, string, string, string, string> FindPropertyNameForIndex
        {
            get => _findPropertyNameForIndex;
            set
            {
                AssertNotFrozen();
                _findPropertyNameForIndex = value;
            }
        }

        /// <summary>
        ///     Get or sets the function to get the identity property name from the entity name
        /// </summary>
        public Func<string, string> FindIdentityPropertyNameFromEntityName
        {
            get => _findIdentityPropertyNameFromEntityName;
            set
            {
                AssertNotFrozen();
                _findIdentityPropertyNameFromEntityName = value;
            }
        }
*/

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

    /* TODO

        /// <summary>
        ///     Saves Enums as integers and instruct the Linq provider to query enums as integer values.
        /// </summary>
        public bool SaveEnumsAsIntegers
        {
            get => _saveEnumsAsIntegers;
            set
            {
                AssertNotFrozen();
                _saveEnumsAsIntegers = value;
            }
        }

        public void RegisterCustomQueryTranslator<T>(Expression<Func<T, object>> member, CustomQueryTranslator translator)
        {
            AssertNotFrozen();

            var body = member.Body as UnaryExpression;
            if (body == null)
                throw new NotSupportedException("A custom query translator can only be used to evaluate a simple member access or method call.");

            var info = GetMemberInfoFromExpression(body.Operand);

            if (_customQueryTranslators.ContainsKey(info) == false)
                _customQueryTranslators.Add(info, translator);
        }

*/

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

        /* TODO
        foreach (var typeToRegisteredIdConvention in _listOfRegisteredIdConventionsAsync
                .Where(typeToRegisteredIdConvention => typeToRegisteredIdConvention.Item1.IsAssignableFrom(type)))
                return typeToRegisteredIdConvention.Item2(databaseName, entity);
         */

        return _documentIdGenerator.apply(databaseName, entity);
    }

    /* TODO

        /// <summary>
        ///     Register an async id convention for a single type (and all of its derived types.
        ///     Note that you can still fall back to the DocumentIdGenerator if you want.
        /// </summary>
        public DocumentConventions RegisterAsyncIdConvention<TEntity>(Func<string, TEntity, Task<string>> func)
        {
            AssertNotFrozen();

            var type = typeof(TEntity);
            var entryToRemove = _listOfRegisteredIdConventionsAsync.FirstOrDefault(x => x.Item1 == type);
            if (entryToRemove != null)
                _listOfRegisteredIdConventionsAsync.Remove(entryToRemove);

            int index;
            for (index = 0; index < _listOfRegisteredIdConventionsAsync.Count; index++)
            {
                var entry = _listOfRegisteredIdConventionsAsync[index];
                if (entry.Item1.IsAssignableFrom(type))
                    break;
            }

            var item = new Tuple<Type, Func<string, object, Task<string>>>(typeof(TEntity), (dbName, o) => func(dbName, (TEntity)o));
            _listOfRegisteredIdConventionsAsync.Insert(index, item);

            return this;
        }

        /// <summary>
        ///     Register an id convention for a single type (and all its derived types) to be used when calling
        ///     session.Load{TEntity}(TId id)
        ///     It is used by the default implementation of FindFullDocumentIdFromNonStringIdentifier.
        /// </summary>
        public DocumentConventions RegisterIdLoadConvention<TEntity>(Func<ValueType, string> func)
        {
            AssertNotFrozen();

            var type = typeof(TEntity);
            var entryToRemove = _listOfRegisteredIdLoadConventions.FirstOrDefault(x => x.Item1 == type);
            if (entryToRemove != null)
                _listOfRegisteredIdLoadConventions.Remove(entryToRemove);

            int index;
            for (index = 0; index < _listOfRegisteredIdLoadConventions.Count; index++)
            {
                var entry = _listOfRegisteredIdLoadConventions[index];
                if (entry.Item1.IsAssignableFrom(type))
                    break;
            }

            var item = new Tuple<Type, Func<ValueType, string>>(typeof(TEntity), func);
            _listOfRegisteredIdLoadConventions.Insert(index, item);

            return this;
        }


        /// <summary>
        ///     Creates the serializer.
        /// </summary>
        /// <returns></returns>
        public JsonSerializer CreateSerializer()
        {
            var jsonSerializer = new JsonSerializer
            {
                DateParseHandling = DateParseHandling.None,
                ObjectCreationHandling = ObjectCreationHandling.Auto,
                ContractResolver = JsonContractResolver,
                TypeNameHandling = TypeNameHandling.Auto,
                TypeNameAssemblyFormatHandling = TypeNameAssemblyFormatHandling.Simple,
                ConstructorHandling = ConstructorHandling.AllowNonPublicDefaultConstructor,
                FloatParseHandling = FloatParseHandling.Double
            };

            CustomizeJsonSerializer(jsonSerializer);
            //TODO - EFRAT
            if (SaveEnumsAsIntegers == false)
                jsonSerializer.Converters.Add(new StringEnumConverter());

            jsonSerializer.Converters.Add(JsonDateTimeISO8601Converter.Instance);
            jsonSerializer.Converters.Add(JsonLuceneDateTimeConverter.Instance);
            jsonSerializer.Converters.Add(JsonObjectConverter.Instance);
            jsonSerializer.Converters.Add(JsonDictionaryDateTimeKeysConverter.Instance);
            jsonSerializer.Converters.Add(ParametersConverter.Instance);
            jsonSerializer.Converters.Add(JsonLinqEnumerableConverter.Instance);
            // TODO: Iftah
            //var convertersToUse = SaveEnumsAsIntegers ? DefaultConvertersEnumsAsIntegers : DefaultConverters;
            //if (jsonSerializer.Converters.Count == 0)
            //{
            //    jsonSerializer.Converters = convertersToUse;
            //}
            //else
            //{
            //    for (int i = convertersToUse.Count - 1; i >= 0; i--)
            //    {
            //        jsonSerializer.Converters.Insert(0, convertersToUse[i]);
            //    }
            //}
            return jsonSerializer;
        }
*/

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

    /* TODO

        public static RangeType GetRangeType(object o)
        {
            if (o == null)
                return RangeType.None;

            var type = o as Type ?? o.GetType();
            return GetRangeType(type);
        }

        public static RangeType GetRangeType(Type type)
        {
            var nonNullable = Nullable.GetUnderlyingType(type);
            if (nonNullable != null)
                type = nonNullable;

            if (type == typeof(int) || type == typeof(long) || type == typeof(short) || type == typeof(TimeSpan))
                return RangeType.Long;

            if (type == typeof(double) || type == typeof(float) || type == typeof(decimal))
                return RangeType.Double;

            return RangeType.None;
        }

*/

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

    /* TODO

        private static IEnumerable<MemberInfo> GetPropertiesForType(Type type)
        {
            foreach (var propertyInfo in ReflectionUtil.GetPropertiesAndFieldsFor(type, BindingFlags.Public | BindingFlags.Instance | BindingFlags.NonPublic))
                yield return propertyInfo;

            foreach (var @interface in type.GetInterfaces())
                foreach (var propertyInfo in GetPropertiesForType(@interface))
                    yield return propertyInfo;
        }

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

        internal LinqPathProvider.Result TranslateCustomQueryExpression(LinqPathProvider provider, Expression expression)
        {
            var member = GetMemberInfoFromExpression(expression);

            return _customQueryTranslators.TryGetValue(member, out var translator) == false
                ? null
                : translator.Invoke(provider, expression);
        }

        private static MemberInfo GetMemberInfoFromExpression(Expression expression)
        {
            var callExpression = expression as MethodCallExpression;
            if (callExpression != null)
                return callExpression.Method;

            var memberExpression = expression as MemberExpression;
            if (memberExpression != null)
                return memberExpression.Member;

            throw new NotSupportedException("A custom query translator can only be used to evaluate a simple member access or method call.");
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
