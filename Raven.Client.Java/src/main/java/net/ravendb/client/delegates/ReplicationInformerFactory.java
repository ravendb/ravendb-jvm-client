package net.ravendb.client.delegates;

import net.ravendb.client.connection.IDocumentStoreReplicationInformer;
import net.ravendb.client.connection.implementation.HttpJsonRequestFactory;

public interface ReplicationInformerFactory {
  public IDocumentStoreReplicationInformer create(String url, HttpJsonRequestFactory jsonRequestFactory);
}
