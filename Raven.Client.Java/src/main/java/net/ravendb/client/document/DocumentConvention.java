package net.ravendb.client.document;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.ravendb.abstractions.basic.Reference;
import net.ravendb.abstractions.basic.Tuple;
import net.ravendb.abstractions.data.Constants;
import net.ravendb.abstractions.indexing.SortOptions;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.client.connection.IDatabaseCommands;
import net.ravendb.client.connection.IDocumentStoreReplicationInformer;
import net.ravendb.client.connection.ReplicationInformer;
import net.ravendb.client.connection.implementation.HttpJsonRequestFactory;
import net.ravendb.client.converters.ITypeConverter;
import net.ravendb.client.converters.Int32Converter;
import net.ravendb.client.converters.Int64Converter;
import net.ravendb.client.converters.UUIDConverter;
import net.ravendb.client.delegates.DocumentKeyFinder;
import net.ravendb.client.delegates.IdConvention;
import net.ravendb.client.delegates.IdValuePartFinder;
import net.ravendb.client.delegates.IdentityPropertyFinder;
import net.ravendb.client.delegates.IdentityPropertyNameFinder;
import net.ravendb.client.delegates.JavaClassFinder;
import net.ravendb.client.delegates.JavaClassNameFinder;
import net.ravendb.client.delegates.PropertyNameFinder;
import net.ravendb.client.delegates.ReplicationInformerFactory;
import net.ravendb.client.delegates.RequestCachePolicy;
import net.ravendb.client.delegates.TypeTagNameToDocumentKeyPrefixTransformer;
import net.ravendb.client.util.Inflector;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.DeserializationProblemHandler;


/**
 * Note: we removed logic related to applyReduceFunction because we don't support map/reduce on shards
 * we also don't support contractResolver - Jackson customization can be performed via JsonExtensions#getDefaultObjectMapper() instance
 */
public class DocumentConvention extends Convention implements Serializable {

  private final List<Tuple<Class<?>, IdConvention>> listOfRegisteredIdConventions =
      new ArrayList<>();

  private boolean preserveDocumentPropertiesNotFoundOnModel = true;

  private boolean disableProfiling;

  private List<ITypeConverter> identityTypeConvertors;

  private int maxNumberOfRequestsPerSession;

  private int maxLengthOfQueryUsingGetUrl;

  private boolean allowQueriesOnId;

  private ConsistencyOptions defaultQueryingConsistency;

  /**
   * Whether UseOptimisticConcurrency is set to true by default for all opened sessions
   */
  private boolean defaultUseOptimisticConcurrency;

  private static Map<Class<?>, String> CACHED_DEFAULT_TYPE_TAG_NAMES = new HashMap<>();

  private JavaClassFinder findJavaClass;

  private JavaClassNameFinder findJavaClassName;

  private DocumentKeyFinder findFullDocumentKeyFromNonStringIdentifier;

  private DeserializationProblemHandler jsonContractResolver;

  private TypeTagNameFinder findTypeTagName;

  private PropertyNameFinder findPropertyNameForIndex;

  private PropertyNameFinder findPropertyNameForDynamicIndex;

  private IdentityPropertyNameFinder findIdentityPropertyNameFromEntityName;

  private DocumentKeyGenerator documentKeyGenerator;

  private boolean useParallelMultiGet;

  private boolean shouldAggressiveCacheTrackChanges;

  private boolean shouldSaveChangesForceAggressiveCacheCheck;

  private IdValuePartFinder findIdValuePartForValueTypeConversion;

  private TypeTagNameToDocumentKeyPrefixTransformer transformTypeTagNameToDocumentKeyPrefix;

  private ReplicationInformerFactory replicationInformerFactory;

  private final Map<String, SortOptions> customDefaultSortOptions = new HashMap<>();

  private final List<Class<?>> customRangeTypes = new ArrayList<>();

  private final List<Tuple<Class<?>, TryConvertValueForQueryDelegate<?>>> listOfQueryValueConverters = new ArrayList<>();

  private JsonSerializer jsonSerializer;

  private EnumSet<IndexAndTransformerReplicationMode> indexAndTransformerReplicationMode;

  private boolean acceptGzipContent;

  public DocumentConvention() {

    setIdentityTypeConvertors(Arrays.<ITypeConverter> asList(new UUIDConverter(), new Int32Converter(), new Int64Converter()));
    setDisableProfiling(true);
    setUseParallelMultiGet(true);
    setDefaultQueryingConsistency(ConsistencyOptions.NONE);
    setFailoverBehavior(FailoverBehaviorSet.of(FailoverBehavior.ALLOW_READS_FROM_SECONDARIES));
    setShouldCacheRequest(new RequestCachePolicy() {
      @SuppressWarnings("boxing")
      @Override
      public Boolean shouldCacheRequest(String url) {
        return true;
      }
    });
    setFindIdentityProperty(new IdentityPropertyFinder() {
      @SuppressWarnings("boxing")
      @Override
      public Boolean find(Field input) {
        return input.getName().equals("id");
      }
    });
    setFindJavaClass(new JavaClassFinder() {

      @Override
      public String find(String id, RavenJObject doc, RavenJObject metadata) {
        return metadata.value(String.class, Constants.RAVEN_JAVA_CLASS);
      }
    });
    setFindJavaClassName(new JavaClassNameFinder() {
      @Override
      public String find(Class< ? > entityType) {
        return ReflectionUtil.getFullNameWithoutVersionInformation(entityType);
      }
    });

    setTransformTypeTagNameToDocumentKeyPrefix(new TypeTagNameToDocumentKeyPrefixTransformer() {
      @Override
      public String transform(String typeTagName) {
        return defaultTransformTypeTagNameToDocumentKeyPrefix(typeTagName);
      }
    });
    setFindFullDocumentKeyFromNonStringIdentifier(new DocumentKeyFinder() {
      @SuppressWarnings("boxing")
      @Override
      public String find(Object id, Class< ? > type, Boolean allowNull) {
        return defaultFindFullDocumentKeyFromNonStringIdentifier(id, type, allowNull);
      }
    });

    setFindIdentityPropertyNameFromEntityName(new IdentityPropertyNameFinder() {
      @Override
      public String find(String entityName) {
        return "id";
      }
    });

    setFindTypeTagName(new TypeTagNameFinder() {

      @Override
      public String find(Class< ? > clazz) {
        return defaultTypeTagName(clazz);
      }
    });

    setFindPropertyNameForIndex(new PropertyNameFinder() {
      @Override
      public String find(Class<?> indexedType, String indexedName, String path, String prop) {
        return (path + prop).replace(',', '_').replace('.', '_');
      }
    });

    setFindPropertyNameForDynamicIndex(new PropertyNameFinder() {
      @Override
      public String find(Class< ? > indexedType, String indexedName, String path, String prop) {
        return path + prop;
      }
    });

    setIdentityPartsSeparator("/");
    setJsonContractResolver(new DefaultRavenContractResolver());

    setMaxNumberOfRequestsPerSession(30);
    setReplicationInformerFactory(new ReplicationInformerFactory() {
      @Override
      public IDocumentStoreReplicationInformer create(String url, HttpJsonRequestFactory jsonRequestFactory) {
        return new ReplicationInformer(DocumentConvention.this, jsonRequestFactory);
      }
    });
    setFindIdValuePartForValueTypeConversion(new IdValuePartFinder() {
      @Override
      public String find(Object entity, String id) {
        String[] splits = id.split(identityPartsSeparator);
        for (int i = splits.length - 1; i >= 0; i--) {
          if (StringUtils.isNotEmpty(splits[i])) {
            return splits[i];
          }
        }
        return null;
      }
    });
    setShouldAggressiveCacheTrackChanges(true);
    setShouldSaveChangesForceAggressiveCacheCheck(true);
    setMaxLengthOfQueryUsingGetUrl(1024+512);
    setIndexAndTransformerReplicationMode(
      EnumSet.of(
        IndexAndTransformerReplicationMode.INDEXES,
        IndexAndTransformerReplicationMode.TRANSFORMERS));
    acceptGzipContent = true;
    jsonSerializer = new JsonSerializer(this);
  }

  public static String defaultTransformTypeTagNameToDocumentKeyPrefix(String typeTagName) {
    char[] charArray = typeTagName.toCharArray();
    int count = 0;
    for (int i = 0; i < charArray.length; i++) {
      if (Character.isUpperCase(charArray[i])) {
        count++;
      }
    }

    if (count <= 1) { //simple name, just lower case it
      return typeTagName.toLowerCase();
    }
    // multiple capital letters, so probably something that we want to preserve caps on.
    return typeTagName;
  }

  /**
   * Find the full document name assuming that we are using the standard conventions
   * for generating a document key
   * @param id
   * @param type
   * @param allowNull
   */
  public String defaultFindFullDocumentKeyFromNonStringIdentifier(Object id, Class<?> type, boolean allowNull) {
    ITypeConverter converter = null;
    for (ITypeConverter conv: getIdentityTypeConvertors()) {
      if (conv.canConvertFrom(id.getClass())) {
        converter = conv;
        break;
      }
    }
    String tag = getTypeTagName(type);
    if (tag != null) {
      tag = transformTypeTagNameToDocumentKeyPrefix.transform(tag);
      tag += identityPartsSeparator;
    }
    if (converter != null) {
      return converter.convertFrom(tag, id, allowNull);
    }
    return tag + id;
  }



  /**
   * Disable all profiling support
   */
  public boolean isDisableProfiling() {
    return disableProfiling;
  }

  /**
   * Disable all profiling support
   * @param b
   */
  public void setDisableProfiling(boolean b) {
    this.disableProfiling = b;
  }

  /**
   * A list of type converters that can be used to translate the document key (string)
   * to whatever type it is that is used on the entity, if the type isn't already a string
   */
  public List<ITypeConverter> getIdentityTypeConvertors() {
    return identityTypeConvertors;
  }

  public DeserializationProblemHandler getJsonContractResolver() {
    return jsonContractResolver;
  }

  public void setJsonContractResolver(DeserializationProblemHandler jsonContractResolver) {
    this.jsonContractResolver = jsonContractResolver;
  }

  /**
   * A list of type converters that can be used to translate the document key (string)
   * to whatever type it is that is used on the entity, if the type isn't already a string
   * @param identityTypeConvertors
   */
  public void setIdentityTypeConvertors(List<ITypeConverter> identityTypeConvertors) {
    this.identityTypeConvertors = identityTypeConvertors;
  }

  /**
   * Gets the identity parts separator used by the HiLo generators
   */
  public String getIdentityPartsSeparator() {
    return identityPartsSeparator;
  }

  /**
   * Sets the identity parts separator used by the HiLo generators
   * @param identityPartsSeparator
   */
  public void setIdentityPartsSeparator(String identityPartsSeparator) {
    this.identityPartsSeparator = identityPartsSeparator;
  }

  /**
   * Gets the default max number of requests per session.
   */
  public int getMaxNumberOfRequestsPerSession() {
    return maxNumberOfRequestsPerSession;
  }

  /**
   * Sets the default max number of requests per session.
   * @param maxNumberOfRequestsPerSession
   */
  public void setMaxNumberOfRequestsPerSession(int maxNumberOfRequestsPerSession) {
    this.maxNumberOfRequestsPerSession = maxNumberOfRequestsPerSession;
  }

  /**
   *  Whatever to allow queries on document id.
   *  By default, queries on id are disabled, because it is far more efficient
   *  to do a Load() than a Query() if you already know the id.
   *  This is NOT recommended and provided for backward compatibility purposes only.
   */
  public boolean isAllowQueriesOnId() {
    return allowQueriesOnId;
  }

  /**
   *  Whatever to allow queries on document id.
   *  By default, queries on id are disabled, because it is far more efficient
   *  to do a Load() than a Query() if you already know the id.
   *  This is NOT recommended and provided for backward compatibility purposes only.
   * @param allowQueriesOnId
   */
  public void setAllowQueriesOnId(boolean allowQueriesOnId) {
    this.allowQueriesOnId = allowQueriesOnId;
  }

  /**
   * The consistency options used when querying the database by default
   */
  public ConsistencyOptions getDefaultQueryingConsistency() {
    return defaultQueryingConsistency;
  }

  /**
   * The consistency options used when querying the database by default
   * @param defaultQueryingConsistency
   */
  public void setDefaultQueryingConsistency(ConsistencyOptions defaultQueryingConsistency) {
    this.defaultQueryingConsistency = defaultQueryingConsistency;
  }

  /**
   * Generates the document key using identity.
   * @param conventions
   * @param entity
   */
  public static String generateDocumentKeyUsingIdentity(DocumentConvention conventions, Object entity) {
    return conventions.getDynamicTagName(entity) + "/";
  }

  /**
   * Get the default tag name for the specified type.
   * @param t
   */
  public static String defaultTypeTagName(Class<?> t) {
    String result;

    if (CACHED_DEFAULT_TYPE_TAG_NAMES.containsKey(t)) {
      return CACHED_DEFAULT_TYPE_TAG_NAMES.get(t);
    }

    result = Inflector.pluralize(t.getSimpleName());
    CACHED_DEFAULT_TYPE_TAG_NAMES.put(t, result);

    return result;
  }

  /**
   *  Gets the name of the type tag.
   * @param type
   */
  public String getTypeTagName(Class<?> type) {
    String value = findTypeTagName.find(type);
    if (value != null) {
      return value;
    }
    return defaultTypeTagName(type);
  }

  /*
   *  If object is dynamic, try to load a tag name.
   */
  public String getDynamicTagName(Object entity) {
    if (entity == null) {
      return null;
    }
    return getTypeTagName(entity.getClass());
  }

  /**
   * Generates the document key.
   * @param dbName Name of the database
   * @param databaseCommands Low level database commands.
   * @param entity The entity.
   */
  public String generateDocumentKey(String dbName, IDatabaseCommands databaseCommands, Object entity) {
    Class<?> type = entity.getClass();
    for (Tuple<Class<?>, IdConvention> typeToRegisteredIdConvention : listOfRegisteredIdConventions) {
      if (typeToRegisteredIdConvention.getItem1().isAssignableFrom(type)) {
        return typeToRegisteredIdConvention.getItem2().findIdentifier(dbName, databaseCommands, entity);
      }
    }

    return documentKeyGenerator.generate(dbName, databaseCommands, entity);
  }

  /**
   *  Gets the function to find the java class of a document.
   */
  public JavaClassFinder getFindJavaClass() {
    return findJavaClass;
  }

  /**
   *  Sets the function to find the java class of a document.
   * @param findJavaClass
   */
  public void setFindJavaClass(JavaClassFinder findJavaClass) {
    this.findJavaClass = findJavaClass;
  }

  /**
   *  Gets the function to find the java class name from a java class
   */
  public JavaClassNameFinder getFindJavaClassName() {
    return findJavaClassName;
  }

  /**
   *  Sets the function to find the java class name from a java class
   * @param findJavaClassName
   */
  public void setFindJavaClassName(JavaClassNameFinder findJavaClassName) {
    this.findJavaClassName = findJavaClassName;
  }

  /**
   * Gets the function to find the full document key based on the type of a document
   * and the value type identifier (just the numeric part of the id).
   */
  public DocumentKeyFinder getFindFullDocumentKeyFromNonStringIdentifier() {
    return findFullDocumentKeyFromNonStringIdentifier;
  }

  /**
   * Sets the function to find the full document key based on the type of a document
   * and the value type identifier (just the numeric part of the id).
   * @param findFullDocumentKeyFromNonStringIdentifier
   */
  public void setFindFullDocumentKeyFromNonStringIdentifier(DocumentKeyFinder findFullDocumentKeyFromNonStringIdentifier) {
    this.findFullDocumentKeyFromNonStringIdentifier = findFullDocumentKeyFromNonStringIdentifier;
  }

  /**
   * Gets the function to find the type tag.
   */
  public TypeTagNameFinder getFindTypeTagName() {
    return findTypeTagName;
  }

  /**
   * Sets the function to find the type tag.
   * @param findTypeTagName
   */
  public void setFindTypeTagName(TypeTagNameFinder findTypeTagName) {
    this.findTypeTagName = findTypeTagName;
  }

  /**
   * Gets the function to find the indexed property name
   * given the indexed document type, the index name, the current path and the property path.
   */
  public PropertyNameFinder getFindPropertyNameForIndex() {
    return findPropertyNameForIndex;
  }

  /**
   * Sets the function to find the indexed property name
   * given the indexed document type, the index name, the current path and the property path.
   * @param findPropertyNameForIndex
   */
  public void setFindPropertyNameForIndex(PropertyNameFinder findPropertyNameForIndex) {
    this.findPropertyNameForIndex = findPropertyNameForIndex;
  }

  /**
   *  Gets the function to find the indexed property name
   *  given the indexed document type, the index name, the current path and the property path.
   */
  public PropertyNameFinder getFindPropertyNameForDynamicIndex() {
    return findPropertyNameForDynamicIndex;
  }

  /**
   * Sets the function to find the indexed property name
   *  given the indexed document type, the index name, the current path and the property path.
   * @param findPropertyNameForDynamicIndex
   */
  public void setFindPropertyNameForDynamicIndex(PropertyNameFinder findPropertyNameForDynamicIndex) {
    this.findPropertyNameForDynamicIndex = findPropertyNameForDynamicIndex;
  }

  /**
   * Get the function to get the identity property name from the entity name
   */
  public IdentityPropertyNameFinder getFindIdentityPropertyNameFromEntityName() {
    return findIdentityPropertyNameFromEntityName;
  }

  /**
   * Sets the function to get the identity property name from the entity name
   * @param findIdentityPropertyNameFromEntityName
   */
  public void setFindIdentityPropertyNameFromEntityName(IdentityPropertyNameFinder findIdentityPropertyNameFromEntityName) {
    this.findIdentityPropertyNameFromEntityName = findIdentityPropertyNameFromEntityName;
  }

  /**
   * Gets the document key generator.
   */
  public DocumentKeyGenerator getDocumentKeyGenerator() {
    return documentKeyGenerator;
  }

  /**
   * Sets the document key generator.
   * @param documentKeyGenerator
   */
  public void setDocumentKeyGenerator(DocumentKeyGenerator documentKeyGenerator) {
    this.documentKeyGenerator = documentKeyGenerator;
  }

  /**
   * Whatever or not RavenDB should in the aggressive cache mode use Changes API to track
   * changes and rebuild the cache. This will make that outdated data will be revalidated
   * to make the cache more updated, however it is still possible to get a state result because of the time
   * needed to receive the notification and forcing to check for cached data.
   */
  public boolean isShouldAggressiveCacheTrackChanges() {
    return shouldAggressiveCacheTrackChanges;
  }

  /**
   * Whatever or not RavenDB should in the aggressive cache mode use Changes API to track
   * changes and rebuild the cache. This will make that outdated data will be revalidated
   * to make the cache more updated, however it is still possible to get a state result because of the time
   * needed to receive the notification and forcing to check for cached data.
   * @param shouldAggressiveCacheTrackChanges
   */
  public void setShouldAggressiveCacheTrackChanges(boolean shouldAggressiveCacheTrackChanges) {
    this.shouldAggressiveCacheTrackChanges = shouldAggressiveCacheTrackChanges;
  }

  /**
   * Whatever or not RavenDB should in the aggressive cache mode should force the aggressive cache
   * to check with the server after we called SaveChanges() on a non empty data set.
   * This will make any outdated data revalidated, and will work nicely as long as you have just a
   * single client. For multiple clients, {@link DocumentConvention#shouldAggressiveCacheTrackChanges}
   */
  public boolean isShouldSaveChangesForceAggressiveCacheCheck() {
    return shouldSaveChangesForceAggressiveCacheCheck;
  }

  /**
   * Whatever or not RavenDB should in the aggressive cache mode should force the aggressive cache
   * to check with the server after we called SaveChanges() on a non empty data set.
   * This will make any outdated data revalidated, and will work nicely as long as you have just a
   * single client. For multiple clients, {@link DocumentConvention#shouldAggressiveCacheTrackChanges}
   * @param shouldSaveChangesForceAggressiveCacheCheck
   */
  public void setShouldSaveChangesForceAggressiveCacheCheck(boolean shouldSaveChangesForceAggressiveCacheCheck) {
    this.shouldSaveChangesForceAggressiveCacheCheck = shouldSaveChangesForceAggressiveCacheCheck;
  }

  /**
   *  Instruct RavenDB to parallel Multi Get processing
   * when handling lazy requests
   * @param useParallelMultiGet
   */
  public void setUseParallelMultiGet(boolean useParallelMultiGet) {
    this.useParallelMultiGet = useParallelMultiGet;
  }

  /**
   * Register an id convention for a single type (and all of its derived types.
   * Note that you can still fall back to the DocumentKeyGenerator if you want.
   */
  @SuppressWarnings("unchecked")
  public <TEntity> DocumentConvention registerIdConvention(Class<TEntity> type, IdConvention func) {
    for (Tuple<Class<?>, IdConvention> entry: listOfRegisteredIdConventions) {
      if (entry.getItem1().equals(type)) {
        listOfRegisteredIdConventions.remove(type);
        break;
      }
    }
    int index;
    for (index = 0; index < listOfRegisteredIdConventions.size(); index++) {
      Tuple<Class< ? >, IdConvention> entry = listOfRegisteredIdConventions.get(index);
      if (entry.getItem1().isAssignableFrom(type)) {
        break;
      }
    }
    Tuple<Class<?>, IdConvention> item =
        (Tuple<Class<?>, IdConvention>) (Object) Tuple.create(type, func);
    listOfRegisteredIdConventions.add(index, item);

    return this;
  }

  /**
   * Get the java class (if exists) from the document
   * @param id
   * @param document
   * @param metadata
   */
  public String getJavaClass(String id, RavenJObject document, RavenJObject metadata) {
    return findJavaClass.find(id, document, metadata);
  }

  /**
   * When RavenDB needs to convert between a string id to a value type like int or uuid, it calls
   * this to perform the actual work
   */
  public IdValuePartFinder getFindIdValuePartForValueTypeConversion() {
    return findIdValuePartForValueTypeConversion;
  }

  /**
   * When RavenDB needs to convert between a string id to a value type like int or uuid, it calls
   * this to perform the actual work
   * @param findIdValuePartForValueTypeConversion
   */
  public void setFindIdValuePartForValueTypeConversion(IdValuePartFinder findIdValuePartForValueTypeConversion) {
    this.findIdValuePartForValueTypeConversion = findIdValuePartForValueTypeConversion;
  }

  /**
   * Translate the type tag name to the document key prefix
   */
  public TypeTagNameToDocumentKeyPrefixTransformer getTransformTypeTagNameToDocumentKeyPrefix() {
    return transformTypeTagNameToDocumentKeyPrefix;
  }

  /**
   * Translate the type tag name to the document key prefix
   * @param transformTypeTagNameToDocumentKeyPrefix
   */
  public void setTransformTypeTagNameToDocumentKeyPrefix(TypeTagNameToDocumentKeyPrefixTransformer transformTypeTagNameToDocumentKeyPrefix) {
    this.transformTypeTagNameToDocumentKeyPrefix = transformTypeTagNameToDocumentKeyPrefix;
  }

  public void setReplicationInformerFactory(ReplicationInformerFactory replicationInformerFactory) {
    this.replicationInformerFactory = replicationInformerFactory;
  }

  /**
   * Get the java class name to be stored in the entity metadata
   */
  public String getJavaClassName(Class<?> entityType) {
    return findJavaClassName.find(entityType);
  }

  /**
   * Clone the current conventions to a new instance
   */
  @Override
  public DocumentConvention clone() {
    DocumentConvention cloned = new DocumentConvention();
    try {
      BeanUtils.copyProperties(cloned, this);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
    return cloned;
  }

  /**
   * Instruct RavenDB to parallel Multi Get processing
   * when handling lazy requests
   */
  public boolean isUseParallelMultiGet() {
    return useParallelMultiGet;
  }

  /**
   * This is called to provide replication behavior for the client. You can customize
   * this to inject your own replication / failover logic.
   */
  public ReplicationInformerFactory getReplicationInformerFactory() {
    return replicationInformerFactory;
  }

  public interface TryConvertValueForQueryDelegate<T> {
    public boolean tryConvertValue(String fieldName, T value, QueryValueConvertionType convertionType, Reference<String> strValue);
    public Class<T> getSupportedClass();
  }

  public <T> void registerQueryValueConverter(TryConvertValueForQueryDelegate<T> converter) {
    registerQueryValueConverter(converter, SortOptions.STRING, false);
  }

  public <T> void registerQueryValueConverter(TryConvertValueForQueryDelegate<T> converter, SortOptions defaultSortOption) {
    registerQueryValueConverter(converter, defaultSortOption, false);
  }

  public <T> void registerQueryValueConverter(final TryConvertValueForQueryDelegate<T> converter, SortOptions defaultSortOption, boolean usesRangeField) {

    int index;
    for (index = 0; index < listOfQueryValueConverters.size(); index++) {
      Tuple<Class<?>, TryConvertValueForQueryDelegate<?>> entry = listOfQueryValueConverters.get(index);
      if (entry.getItem1().isAssignableFrom(converter.getSupportedClass())) {
        break;
      }
    }

    listOfQueryValueConverters.add(index, Tuple.<Class<?>,TryConvertValueForQueryDelegate<?>> create(converter.getSupportedClass(), converter));

    if (defaultSortOption != SortOptions.STRING) {
      customDefaultSortOptions.put(converter.getSupportedClass().getName(), defaultSortOption);
    }
    if (usesRangeField) {
      customRangeTypes.add(converter.getSupportedClass());
    }
  }

  @SuppressWarnings("unchecked")
  public boolean tryConvertValueForQuery(String fieldName, Object value, QueryValueConvertionType convertionType, Reference<String> strValue) {
    for (Tuple<Class<?>, TryConvertValueForQueryDelegate<?>> queryValueConverterTuple : listOfQueryValueConverters) {
      if (queryValueConverterTuple.getItem1().isInstance(value)) {
        TryConvertValueForQueryDelegate< Object > valueForQueryDelegate = (TryConvertValueForQueryDelegate<Object>) queryValueConverterTuple.getItem2();
        return valueForQueryDelegate.tryConvertValue(fieldName, value, convertionType, strValue);
      }
    }
    strValue.value = null;
    return false;
  }

  public SortOptions getDefaultSortOption(String typeName) {

    switch (typeName) {
    case "java.lang.Short":
      return SortOptions.SHORT;
    case "java.lang.Integer":
      return SortOptions.INT;
    case "java.lang.Long":
      return SortOptions.LONG;
    case "java.lang.Double":
      return SortOptions.DOUBLE;
    case "java.lang.Float":
      return SortOptions.FLOAT;
    case "java.lang.String":
      return SortOptions.STRING;
    default:
      return customDefaultSortOptions.containsKey(typeName)? customDefaultSortOptions.get(typeName) : SortOptions.STRING;
    }
  }

  public SortOptions getDefaultSortOption(Class<?> clazz) {
    if (clazz == null) {
      return null;
    }
    return getDefaultSortOption(clazz.getName());
  }

  public boolean usesRangeType(Object o) {
    if (o == null) {
      return false;
    }
    Class<?> type = o.getClass();
    if (o instanceof Class) {
      type = (Class< ? >) o;
    }

    if (Integer.class.equals(type) || Long.class.equals(type) || Double.class.equals(type) || Float.class.equals(type)
      || int.class.equals(type) || long.class.equals(type) || double.class.equals(type) || float.class.equals(type)) {
      return true;
    }
    return customRangeTypes.contains(type);
  }

  public JsonSerializer createSerializer() {
    jsonSerializer.config();
    return jsonSerializer;

  }

  public int getMaxLengthOfQueryUsingGetUrl() {
    return maxLengthOfQueryUsingGetUrl;
  }

  public boolean isDefaultUseOptimisticConcurrency() {
    return defaultUseOptimisticConcurrency;
  }

  public void setDefaultUseOptimisticConcurrency(boolean defaultUseOptimisticConcurrency) {
    this.defaultUseOptimisticConcurrency = defaultUseOptimisticConcurrency;
  }

  public void setMaxLengthOfQueryUsingGetUrl(int maxLengthOfQueryUsingGetUrl) {
    this.maxLengthOfQueryUsingGetUrl = maxLengthOfQueryUsingGetUrl;
  }


  public EnumSet<IndexAndTransformerReplicationMode> getIndexAndTransformerReplicationMode() {
    return indexAndTransformerReplicationMode;
  }


  public void setIndexAndTransformerReplicationMode(
    EnumSet<IndexAndTransformerReplicationMode> indexAndTransformerReplicationMode) {
    this.indexAndTransformerReplicationMode = indexAndTransformerReplicationMode;
  }

  public boolean isPreserveDocumentPropertiesNotFoundOnModel() {
    return preserveDocumentPropertiesNotFoundOnModel;
  }

  /**
   * Controls whatever properties on the object that weren't de-serialized to object properties
   * will be preserved when saving the document again. If false, those properties will be removed
   * when the document will be saved.
   * @param preserveDocumentPropertiesNotFoundOnModel
   */
  public void setPreserveDocumentPropertiesNotFoundOnModel(boolean preserveDocumentPropertiesNotFoundOnModel) {
    this.preserveDocumentPropertiesNotFoundOnModel = preserveDocumentPropertiesNotFoundOnModel;
  }

  public boolean isAcceptGzipContent() {
    return acceptGzipContent;
  }


  public void setAcceptGzipContent(boolean acceptGzipContent) {
    this.acceptGzipContent = acceptGzipContent;
  }

}
