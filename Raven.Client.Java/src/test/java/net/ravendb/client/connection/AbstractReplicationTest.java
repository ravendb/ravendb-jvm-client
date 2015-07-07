package net.ravendb.client.connection;

import net.ravendb.abstractions.closure.Functions;
import net.ravendb.abstractions.cluster.ClusterBehavior;
import net.ravendb.abstractions.connection.OperationCredentials;
import net.ravendb.abstractions.replication.ReplicationDestination;
import net.ravendb.abstractions.replication.ReplicationDestination.TransitiveReplicationOptions;
import net.ravendb.abstractions.replication.ReplicationDocument;
import net.ravendb.client.RavenDBAwareTests;
import net.ravendb.client.connection.request.IRequestExecuter;
import net.ravendb.client.connection.request.ReplicationAwareRequestExecuter;
import net.ravendb.client.listeners.IDocumentConflictListener;
import net.ravendb.client.metrics.RequestTimeMetric;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.util.UUID;



public abstract class AbstractReplicationTest extends RavenDBAwareTests{

  protected ServerClient serverClient2;
  protected static final String SOURCE = "source";
  protected static final String TARGET = "target";


  @SuppressWarnings("cast")
  @Before
  @Override
  public void init() {
    super.init();

    RequestTimeMetric requestTimeMetric = new RequestTimeMetric();
    ReplicationAwareRequestExecuter executer = new ReplicationAwareRequestExecuter(replicationInformer, requestTimeMetric);

    serverClient2 = new ServerClient(DEFAULT_SERVER_URL_2, convention, new OperationCredentials(),
      factory, UUID.randomUUID(),
            new Functions.StaticFunction4<ServerClient, String, ClusterBehavior, Boolean, IRequestExecuter>(executer),
            new Functions.StaticFunction1<String, RequestTimeMetric>(requestTimeMetric),
            null,  new IDocumentConflictListener[0], false, ClusterBehavior.NONE);

  }


  @BeforeClass
  public static void startServerBefore() {
    startServer(DEFAULT_SERVER_PORT_1, true);
    startServer(DEFAULT_SERVER_PORT_2, true);
  }

  @AfterClass
  public static void stopServerAfter() {
    stopServer(DEFAULT_SERVER_PORT_1);
    stopServer(DEFAULT_SERVER_PORT_2);
  }

  protected ReplicationDocument createReplicationDocument() {
    return createReplicationDocument(DEFAULT_SERVER_URL_2, TARGET);
  }

  protected ReplicationDocument createReplicationDocument(String url, String database) {
    ReplicationDestination rep = new ReplicationDestination();
    rep.setUrl(url);
    rep.setDatabase(database);
    rep.setTransitiveReplicationBehavior(TransitiveReplicationOptions.NONE);
    rep.setIgnoredClient(Boolean.FALSE);
    rep.setDisabled(Boolean.FALSE);
    ReplicationDocument repDoc = new ReplicationDocument();
    repDoc.getDestinations().add(rep);
    return repDoc;
  }

}
