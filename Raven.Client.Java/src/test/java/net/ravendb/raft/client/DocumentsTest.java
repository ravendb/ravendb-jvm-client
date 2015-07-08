package net.ravendb.raft.client;

import net.ravendb.abstractions.basic.EventHandler;
import net.ravendb.abstractions.closure.Action1;
import net.ravendb.abstractions.cluster.ClusterBehavior;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.client.connection.profiling.RequestResultArgs;
import net.ravendb.client.document.DocumentStore;
import net.ravendb.raft.RaftTestBase;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(Parameterized.class)
public class DocumentsTest extends RaftTestBase {
    private int numberOfNodes;

    public DocumentsTest(int numberOfNodes) {
        this.numberOfNodes = numberOfNodes;
    }

    @Test
    public void canReadFromMultipleServers1() throws IOException {
        canReadFromMultipleServersInternal(numberOfNodes, ClusterBehavior.READ_FROM_ALL_WRITE_TO_LEADER);
    }

    @Test
    public void canReadFromMultipleServers2() throws IOException {
        canReadFromMultipleServersInternal(numberOfNodes, ClusterBehavior.READ_FROM_ALL_WRITE_TO_LEADER_WITH_FAILOVERS);
    }

    private void canReadFromMultipleServersInternal(int numberOfNodes, final ClusterBehavior clusterBehavior) throws IOException {
        List<DocumentStore> clusterStores = createRaftCluster(numberOfNodes, "Replication", new Action1<DocumentStore>() {
            @Override
            public void apply(DocumentStore store) {
                store.getConventions().setClusterBehavior(clusterBehavior);
            }
        }, getDbName());

        setupClusterConfiguration(clusterStores);

        clusterStores.get(0).getDatabaseCommands().put("keys/1", null, new RavenJObject(), new RavenJObject());
        clusterStores.get(0).getDatabaseCommands().put("keys/2", null, new RavenJObject(), new RavenJObject());
        for (DocumentStore store : clusterStores) {
            waitForDocument(store.getDatabaseCommands().forDatabase(store.getDefaultDatabase(), ClusterBehavior.NONE), "keys/2");
        }

        final List<RequestResultArgs> events = new ArrayList<>();

        for (DocumentStore store : clusterStores) {
            store.initializeProfiling();
            store.getJsonRequestFactory().addLogRequestEventHandler(new EventHandler<RequestResultArgs>() {
                @Override
                public void handle(Object sender, RequestResultArgs event) {
                    events.add(event);
                }
            });
        }

        for (DocumentStore store : clusterStores) {
            store.getDatabaseCommands().get("keys/1");
            store.getDatabaseCommands().get("keys/2");
        }

        // verify if we have at least one request for each server
        Map<Integer, Integer> requestsPerPort = new HashMap<>();
        for (RequestResultArgs event : events) {
            URL url = new URL(event.getUrl());
            Integer currentValue = requestsPerPort.get(url.getPort());
            requestsPerPort.put(url.getPort(), currentValue == null ? 1 : currentValue + 1);
        }

        for (Map.Entry<Integer, Integer> requestCounts : requestsPerPort.entrySet()) {
            Assert.assertTrue(requestCounts.getValue() > 0);
        }
    }

    @Test
    public void putShouldBePropagated() throws IOException {
        List<DocumentStore> clusterStores = createRaftCluster(numberOfNodes, "Replication", new Action1<DocumentStore>() {
            @Override
            public void apply(DocumentStore store) {
                store.getConventions().setClusterBehavior(ClusterBehavior.READ_FROM_LEADER_WRITE_TO_LEADER);
            }
        }, getDbName());

        setupClusterConfiguration(clusterStores);

        for (int i = 0; i < clusterStores.size(); i++) {
            DocumentStore store = clusterStores.get(i);
            store.getDatabaseCommands().put("keys/" + i, null, new RavenJObject(), new RavenJObject());
        }

        for (int i = 0; i < clusterStores.size(); i++) {
            for (DocumentStore store : clusterStores) {
                waitForDocument(store.getDatabaseCommands().forDatabase(store.getDefaultDatabase(), ClusterBehavior.NONE), "keys/" + i);
            }
        }
    }

    @Test
    public void deleteShouldBePropagated() throws IOException {
        List<DocumentStore> clusterStores = createRaftCluster(numberOfNodes, "Replication", new Action1<DocumentStore>() {
            @Override
            public void apply(DocumentStore store) {
                store.getConventions().setClusterBehavior(ClusterBehavior.READ_FROM_LEADER_WRITE_TO_LEADER);
            }
        }, getDbName());

        setupClusterConfiguration(clusterStores);

        for (int i = 0; i < clusterStores.size(); i++) {
            DocumentStore store = clusterStores.get(i);
            store.getDatabaseCommands().put("keys/" + i, null, new RavenJObject(), new RavenJObject());
        }

        for (int i = 0; i < clusterStores.size(); i++) {
            for (DocumentStore store : clusterStores) {
                waitForDocument(store.getDatabaseCommands().forDatabase(store.getDefaultDatabase(), ClusterBehavior.NONE), "keys/" + i);
            }
        }

        for (int i = 0; i < clusterStores.size(); i++) {
            DocumentStore store = clusterStores.get(i);
            store.getDatabaseCommands().delete("keys/" + i, null);
        }

        for (int i = 0; i < clusterStores.size(); i++) {
            for (DocumentStore store : clusterStores) {
                waitForDelete(store.getDatabaseCommands().forDatabase(store.getDefaultDatabase(), ClusterBehavior.NONE), "keys/" + i);
            }
        }
    }
}
