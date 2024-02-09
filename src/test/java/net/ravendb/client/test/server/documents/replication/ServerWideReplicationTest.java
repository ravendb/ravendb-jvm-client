package net.ravendb.client.test.server.documents.replication;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.connectionStrings.PutConnectionStringOperation;
import net.ravendb.client.documents.operations.connectionStrings.PutConnectionStringResult;
import net.ravendb.client.documents.operations.etl.RavenConnectionString;
import net.ravendb.client.documents.operations.ongoingTasks.OngoingTaskType;
import net.ravendb.client.documents.operations.replication.ExternalReplication;
import net.ravendb.client.documents.operations.replication.UpdateExternalReplicationOperation;
import net.ravendb.client.infrastructure.DisabledOnPullRequest;
import net.ravendb.client.serverwide.DatabaseRecord;
import net.ravendb.client.serverwide.DatabaseRecordWithEtag;
import net.ravendb.client.serverwide.operations.CreateDatabaseOperation;
import net.ravendb.client.serverwide.operations.DeleteDatabasesOperation;
import net.ravendb.client.serverwide.operations.GetDatabaseRecordOperation;
import net.ravendb.client.serverwide.operations.ongoingTasks.*;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisabledOnPullRequest
public class ServerWideReplicationTest extends RemoteTestBase {

    @Test
    public void canStoreServerWideExternalReplication() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            ServerWideExternalReplication putConfiguration = new ServerWideExternalReplication();
            putConfiguration.setDisabled(true);
            putConfiguration.setTopologyDiscoveryUrls(store.getUrls());
            putConfiguration.setDelayReplicationFor(Duration.ofMinutes(3));
            putConfiguration.setMentorNode("A");

            ServerWideExternalReplicationResponse result = store.maintenance().server().send(new PutServerWideExternalReplicationOperation(putConfiguration));
            ServerWideExternalReplication serverWideConfiguration = store.maintenance().server().send(new GetServerWideExternalReplicationOperation(result.getName()));
            assertThat(serverWideConfiguration)
                    .isNotNull();

            try {
                validateServerWideConfiguration(serverWideConfiguration, putConfiguration);

                // the configuration is applied to existing databases

                DatabaseRecordWithEtag record1 = store.maintenance().server().send(new GetDatabaseRecordOperation(store.getDatabase()));
                List<ExternalReplication> externalReplications1 = record1.getExternalReplications();
                assertThat(externalReplications1)
                        .hasSize(1);
                validateConfiguration(serverWideConfiguration, externalReplications1.get(0), store.getDatabase());

                // the configuration is applied to new databases
                String newDbName = store.getDatabase() + "-testDatabase";
                try {
                    store.maintenance().server().send(new CreateDatabaseOperation(new DatabaseRecord(newDbName)));
                    List<ExternalReplication> externalReplications = record1.getExternalReplications();
                    assertThat(externalReplications)
                            .hasSize(1);

                    DatabaseRecordWithEtag record2 = store.maintenance().server().send(new GetDatabaseRecordOperation(newDbName));
                    validateConfiguration(serverWideConfiguration, record2.getExternalReplications().get(0), newDbName);

                    // update the external replication configuration

                    putConfiguration.setTopologyDiscoveryUrls(new String[]{store.getUrls()[0], "http://localhost:8080"});
                    putConfiguration.setName(serverWideConfiguration.getName());

                    result = store.maintenance().server().send(new PutServerWideExternalReplicationOperation(putConfiguration));
                    serverWideConfiguration = store.maintenance().server().send(new GetServerWideExternalReplicationOperation(result.getName()));
                    validateServerWideConfiguration(serverWideConfiguration, putConfiguration);

                    record1 = store.maintenance().server().send(new GetDatabaseRecordOperation(store.getDatabase()));
                    assertThat(record1.getExternalReplications())
                            .hasSize(1);
                    validateConfiguration(serverWideConfiguration, record1.getExternalReplications().get(0), store.getDatabase());

                    record2 = store.maintenance().server().send(new GetDatabaseRecordOperation(newDbName));
                    assertThat(record2.getExternalReplications())
                            .hasSize(1);
                    validateConfiguration(serverWideConfiguration, record2.getExternalReplications().get(0), newDbName);
                } finally {
                    store.maintenance().server().send(new DeleteDatabasesOperation(newDbName, true));
                }
            } finally {
                store.maintenance().server().send(new DeleteServerWideTaskOperation(serverWideConfiguration.getName(), OngoingTaskType.REPLICATION));
            }
        }
    }

    @Test
    public void canExcludeDatabase() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            ServerWideExternalReplication serverWideExternalReplication = new ServerWideExternalReplication();
            serverWideExternalReplication.setDisabled(true);
            serverWideExternalReplication.setTopologyDiscoveryUrls(store.getUrls());

            ServerWideExternalReplicationResponse result = store.maintenance().server().send(new PutServerWideExternalReplicationOperation(serverWideExternalReplication));
            serverWideExternalReplication.setName(result.getName());

            try {
                DatabaseRecordWithEtag record = store.maintenance().server().send(new GetDatabaseRecordOperation(store.getDatabase()));
                assertThat(record.getExternalReplications())
                        .hasSize(1);
                assertThat(record.getRavenConnectionStrings())
                        .hasSize(1);

                String dbName = "db/" + UUID.randomUUID();
                String csName = "cs/" + UUID.randomUUID();

                RavenConnectionString connectionString = new RavenConnectionString();
                connectionString.setName(csName);
                connectionString.setDatabase(dbName);
                connectionString.setTopologyDiscoveryUrls(new String[] { "http://127.0.0.1:12345" });

                PutConnectionStringResult putConnectionStringResult = store.maintenance().send(new PutConnectionStringOperation<>(connectionString));
                assertThat(putConnectionStringResult.getRaftCommandIndex())
                        .isPositive();

                ExternalReplication externalReplication = new ExternalReplication(dbName, csName);
                externalReplication.setName("Regular Task");
                externalReplication.setDisabled(true);

                store.maintenance().send(new UpdateExternalReplicationOperation(externalReplication));

                record = store.maintenance().server().send(new GetDatabaseRecordOperation(store.getDatabase()));

                assertThat(record.getExternalReplications())
                        .hasSize(2);
                assertThat(record.getRavenConnectionStrings())
                        .hasSize(2);

                serverWideExternalReplication.setExcludedDatabases(new String[] { store.getDatabase() });
                store.maintenance().server().send(new PutServerWideExternalReplicationOperation(serverWideExternalReplication));

                record = store.maintenance().server().send(new GetDatabaseRecordOperation(store.getDatabase()));
                assertThat(record.getExternalReplications())
                        .hasSize(1);
                assertThat(record.getRavenConnectionStrings())
                        .hasSize(1);
                assertThat(record.getExternalReplications().get(0).getName())
                        .isEqualTo(externalReplication.getName());
            } finally {
                store.maintenance().server().send(new DeleteServerWideTaskOperation(serverWideExternalReplication.getName(), OngoingTaskType.REPLICATION));
            }
        }
    }

    private static void validateServerWideConfiguration(ServerWideExternalReplication serverWideConfiguration, ServerWideExternalReplication putConfiguration) {
        if (putConfiguration.getName() != null) {
            assertThat(putConfiguration.getName())
                    .isEqualTo(serverWideConfiguration.getName());
        }
        assertThat(putConfiguration.isDisabled())
                .isEqualTo(serverWideConfiguration.isDisabled());
        assertThat(putConfiguration.getMentorNode())
                .isEqualTo(serverWideConfiguration.getMentorNode());
        assertThat(putConfiguration.getDelayReplicationFor())
                .isEqualTo(serverWideConfiguration.getDelayReplicationFor());
        assertThat(putConfiguration.getTopologyDiscoveryUrls())
                .isEqualTo(serverWideConfiguration.getTopologyDiscoveryUrls());
    }

    private static void validateConfiguration(ServerWideExternalReplication serverWideConfiguration, ExternalReplication externalReplication, String databaseName) {
        assertThat(externalReplication.getName())
                .contains(serverWideConfiguration.getName());
        assertThat(externalReplication.isDisabled())
                .isEqualTo(serverWideConfiguration.isDisabled());
        assertThat(externalReplication.getMentorNode())
                .isEqualTo(serverWideConfiguration.getMentorNode());
        assertThat(externalReplication.getDelayReplicationFor())
                .isEqualTo(serverWideConfiguration.getDelayReplicationFor());
        assertThat(externalReplication.getDatabase())
                .isEqualTo(databaseName);
        assertThat(externalReplication.getConnectionStringName())
                .contains(serverWideConfiguration.getName());
    }
}
