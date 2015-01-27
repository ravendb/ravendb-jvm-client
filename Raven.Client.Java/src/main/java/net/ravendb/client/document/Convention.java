package net.ravendb.client.document;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.reflect.FieldUtils;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

import com.mysema.query.types.Expression;

import net.ravendb.abstractions.closure.Action1;
import net.ravendb.abstractions.connection.OperationCredentials;
import net.ravendb.abstractions.replication.ReplicationClientConfiguration;
import net.ravendb.client.delegates.HttpResponseHandler;
import net.ravendb.client.delegates.HttpResponseWithMetaHandler;
import net.ravendb.client.delegates.IdentityPropertyFinder;
import net.ravendb.client.delegates.RequestCachePolicy;
import net.ravendb.client.indexes.AbstractIndexCreationTask;
import net.ravendb.client.linq.LinqPathProvider;


@SuppressWarnings("unused")
public class Convention {

  private Map<Class<?>, Field> idPropertyCache = new HashMap<>();

  private FailoverBehaviorSet failoverBehavior = new FailoverBehaviorSet();

  private IdentityPropertyFinder findIdentityProperty;

  protected String identityPartsSeparator;

  private RequestCachePolicy shouldCacheRequest;

  private HttpResponseHandler handleForbiddenResponse;

  private HttpResponseWithMetaHandler handleUnauthorizedResponse;

  private boolean saveEnumsAsIntegers;

  public void updateFrom(ReplicationClientConfiguration configuration) {
    if (configuration == null) {
      return;
    }
    if (configuration.getFailoverBehavior() != null) {
      this.failoverBehavior = FailoverBehaviorSet.of(configuration.getFailoverBehavior());
    }
  }

  /**
   * How should we behave in a replicated environment when we can't
   *  reach the primary node and need to failover to secondary node(s).
   * @return the failoverBehavior
   */
  public FailoverBehaviorSet getFailoverBehavior() {
    return failoverBehavior;
  }

  /**
   * How should we behave in a replicated environment when we can't
   *  reach the primary node and need to failover to secondary node(s).
   * @param failoverBehavior the failoverBehavior to set
   */
  public void setFailoverBehavior(FailoverBehaviorSet failoverBehavior) {
    this.failoverBehavior = failoverBehavior;
  }

  public FailoverBehaviorSet getFailoverBehaviorWithoutFlags() {
    FailoverBehaviorSet result = this.failoverBehavior.clone();
    result.remove(FailoverBehavior.READ_FROM_ALL_SERVERS);
    return result;
  }

  /**
   * Gets the identity property.
   * @param type
   */
  @SuppressWarnings("boxing")
  public Field getIdentityProperty(Class<?> type) {
    if (idPropertyCache.containsKey(type)) {
      return idPropertyCache.get(type);
    }
    // we want to ignore nested entities from index creation tasks
    if (type.isMemberClass() && type.getDeclaringClass() != null && AbstractIndexCreationTask.class.isAssignableFrom(type.getDeclaringClass())) {
      idPropertyCache.put(type, null);
      return null;
    }

    Field identityProperty = null;
    for (Field f : getPropertiesForType(type)) {
      if (findIdentityProperty.find(f)) {
        identityProperty = f;
        break;
      }
    }

    if (identityProperty != null && !identityProperty.getDeclaringClass().equals(type)) {
      Field propertyInfo = FieldUtils.getField(identityProperty.getDeclaringClass(), identityProperty.getName());
      if (propertyInfo != null) {
        identityProperty = propertyInfo;
      }
    }

    idPropertyCache.put(type, identityProperty);
    return identityProperty;
  }

  private static Iterable<Field> getPropertiesForType(Class<?> type) {
    List<Field> result = new ArrayList<>();
    do {
      Field[] fields = type.getDeclaredFields();
      for (Field field : fields) {
        if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())) {
          continue;
        }
        result.add(field);
      }
      type = type.getSuperclass();
    } while (type != null && !Object.class.equals(type));

    return result;
  }
  /**
   * Whatever or not RavenDB should cache the request to the specified url.
   * @return the shouldCacheRequest
   */
  public RequestCachePolicy getShouldCacheRequest() {
    return shouldCacheRequest;
  }

  /**
   * Gets the function to find the identity property.
   */
  public IdentityPropertyFinder getFindIdentityProperty() {
    return findIdentityProperty;
  }

  /**
   * Sets the function to find the identity property.
   * @param findIdentityProperty
   */
  public void setFindIdentityProperty(IdentityPropertyFinder findIdentityProperty) {
    this.findIdentityProperty = findIdentityProperty;
  }

  /**
   * Whatever or not RavenDB should cache the request to the specified url.
   * @param url
   */
  public Boolean shouldCacheRequest(String url) {
    return shouldCacheRequest.shouldCacheRequest(url);
  }

  /**
   * @param shouldCacheRequest the shouldCacheRequest to set
   */
  public void setShouldCacheRequest(RequestCachePolicy shouldCacheRequest) {
    this.shouldCacheRequest = shouldCacheRequest;
  }

  /**
   *  Handles unauthenticated responses, usually by authenticating against the oauth server
   * @return the handleUnauthorizedResponse
   */
  public HttpResponseWithMetaHandler getHandleUnauthorizedResponse() {
    return handleUnauthorizedResponse;
  }

  /**
   *  Handles unauthenticated responses, usually by authenticating against the oauth server
   * @param handleUnauthorizedResponse the handleUnauthorizedResponse to set
   */
  public void setHandleUnauthorizedResponse(HttpResponseWithMetaHandler handleUnauthorizedResponse) {
    this.handleUnauthorizedResponse = handleUnauthorizedResponse;
  }

  /**
   * Handles forbidden responses
   * @return the handleForbiddenResponse
   */
  public HttpResponseHandler getHandleForbiddenResponse() {
    return handleForbiddenResponse;
  }

  /**
   * Handles forbidden responses
   * @param handleForbiddenResponse the handleForbiddenResponse to set
   */
  public void setHandleForbiddenResponse(HttpResponseHandler handleForbiddenResponse) {
    this.handleForbiddenResponse = handleForbiddenResponse;
  }

  public void handleForbiddenResponse(HttpResponse forbiddenResponse) {
    handleForbiddenResponse.handle(forbiddenResponse);
  }

  public Action1<HttpRequest> handleUnauthorizedResponse(HttpResponse unauthorizedResponse, OperationCredentials credentials) {
    return handleUnauthorizedResponse.handle(unauthorizedResponse, credentials);
  }

  private List<CustomQueryExpressionTranslator> customQueryTranslators = new ArrayList<>();

  public void registerCustomQueryTranslator(CustomQueryExpressionTranslator translator) {
    customQueryTranslators.add(translator);
  }

  public LinqPathProvider.Result translateCustomQueryExpression(LinqPathProvider provider, Expression<?> expression) {
    for (CustomQueryExpressionTranslator translator: customQueryTranslators) {
      if (translator.canTransform(expression)) {
        return translator.translate(expression);
      }
    }
    return null;
  }


  /**
   * Saves Enums as integers and instruct the Linq provider to query enums as integer values.
   */
  public boolean isSaveEnumsAsIntegers() {
    return saveEnumsAsIntegers;
  }

  /**
   * Saves Enums as integers and instruct the Linq provider to query enums as integer values.
   * @param saveEnumsAsIntegers
   */
  public void setSaveEnumsAsIntegers(boolean saveEnumsAsIntegers) {
    this.saveEnumsAsIntegers = saveEnumsAsIntegers;
  }
}
