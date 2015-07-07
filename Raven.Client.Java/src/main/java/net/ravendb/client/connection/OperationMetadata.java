package net.ravendb.client.connection;

import net.ravendb.abstractions.cluster.ClusterInformation;
import net.ravendb.abstractions.connection.OperationCredentials;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;


public class OperationMetadata {

  private String url;
  private OperationCredentials credentials;
  private ClusterInformation clusterInformation = ClusterInformation.NOT_IN_CLUSTER;

  public String getUrl() {
    return url;
  }

  public OperationCredentials getCredentials() {
    return credentials;
  }

  public ClusterInformation getClusterInformation() {
    return clusterInformation;
  }

  public OperationMetadata(String url) {
    super();
    this.url = url;
  }

  public OperationMetadata(String url, OperationCredentials credentials, ClusterInformation clusterInformation) {
    this.url = url;
    this.credentials = credentials != null ? new OperationCredentials(credentials.getApiKey()) : new OperationCredentials();
    this.clusterInformation = clusterInformation != null ? new ClusterInformation(clusterInformation.isInCluster(), clusterInformation.isLeader()) : ClusterInformation.NOT_IN_CLUSTER;
  }

  public OperationMetadata(OperationMetadata opMeta) {
    this.url = opMeta.getUrl();
    this.credentials = new OperationCredentials(opMeta.getCredentials());
    this.clusterInformation = new ClusterInformation(opMeta.getClusterInformation().isInCluster(), opMeta.getClusterInformation().isLeader());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (o == null || getClass() != o.getClass()) return false;

    OperationMetadata that = (OperationMetadata) o;

    return new EqualsBuilder()
            .append(url, that.url)
            .append(credentials, that.credentials)
            .append(clusterInformation, that.clusterInformation)
            .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
            .append(url)
            .append(credentials)
            .append(clusterInformation)
            .toHashCode();
  }
}
