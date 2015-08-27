package net.ravendb.abstractions.data;

import com.mysema.query.types.Expression;
import net.ravendb.abstractions.extensions.ExpressionExtensions;
import net.ravendb.abstractions.json.linq.RavenJToken;

import java.util.HashMap;
import java.util.Map;


public class SubscriptionCriteria {
  private String keyStartsWith;
  private String[] belongsToAnyCollection;
  private Map<String, RavenJToken> propertiesMatch;
  private Map<String, RavenJToken> propertiesNotMatch;
  private Etag startEtag;



  public SubscriptionCriteria() {
    super();
    propertiesMatch = new HashMap<>();
    propertiesNotMatch = new HashMap<>();
  }

  public String getKeyStartsWith() {
    return keyStartsWith;
  }

  public void setKeyStartsWith(String keyStartsWith) {
    this.keyStartsWith = keyStartsWith;
  }

  public String[] getBelongsToAnyCollection() {
    return belongsToAnyCollection;
  }

  public void setBelongsToAnyCollection(String... belongsToAnyCollection) {
    this.belongsToAnyCollection = belongsToAnyCollection;
  }

  public Map<String, RavenJToken> getPropertiesMatch() {
    return propertiesMatch;
  }

  public void setPropertiesMatch(Map<String, RavenJToken> propertiesMatch) {
    this.propertiesMatch = propertiesMatch;
  }

  public Map<String, RavenJToken> getPropertiesNotMatch() {
    return propertiesNotMatch;
  }

  public void setPropertiesNotMatch(Map<String, RavenJToken> propertiesNotMatch) {
    this.propertiesNotMatch = propertiesNotMatch;
  }

  public void propertyMatch(String field, RavenJToken indexing) {
    propertiesMatch.put(field, indexing);
  }

  public void propertyNotMatch(String field, RavenJToken indexing) {
    propertiesNotMatch.put(field, indexing);
  }

  public <TValue> void propertyMatch(Expression<TValue> expr, TValue value) {
    String field = ExpressionExtensions.toPropertyPath(expr);
    propertiesMatch.put(field, RavenJToken.fromObject(value));
  }

  public <TValue> void propertyNotMatch(Expression<TValue> expr, TValue value) {
    String field = ExpressionExtensions.toPropertyPath(expr);
    propertiesNotMatch.put(field, RavenJToken.fromObject(value));
  }

  public Etag getStartEtag() {
    return startEtag;
  }

  public void setStartEtag(Etag startEtag) {
    this.startEtag = startEtag;
  }
}
