package net.ravendb.abstractions.connection;


import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class OperationCredentials {
  private String apiKey;

  public String getApiKey() {
    return apiKey;
  }

  public OperationCredentials() {
    super();
  }

  public OperationCredentials(OperationCredentials operationCredentials) {
    this.apiKey = operationCredentials.apiKey;
  }


  public OperationCredentials(String apiKey) {
    super();
    this.apiKey = apiKey;
  }


  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (o == null || getClass() != o.getClass()) return false;

    OperationCredentials that = (OperationCredentials) o;

    return new EqualsBuilder()
            .append(apiKey, that.apiKey)
            .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
            .append(apiKey)
            .toHashCode();
  }
}
