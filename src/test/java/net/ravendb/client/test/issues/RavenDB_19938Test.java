package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.indexes.IndexDefinition;
import net.ravendb.client.documents.indexes.analysis.AnalyzerDefinition;
import net.ravendb.client.documents.operations.backups.PeriodicBackupConfiguration;
import net.ravendb.client.documents.operations.configuration.ClientConfiguration;
import net.ravendb.client.documents.operations.configuration.StudioConfiguration;
import net.ravendb.client.documents.operations.configuration.StudioEnvironment;
import net.ravendb.client.documents.operations.expiration.ExpirationConfiguration;
import net.ravendb.client.documents.operations.refresh.RefreshConfiguration;
import net.ravendb.client.documents.operations.revisions.RevisionsCollectionConfiguration;
import net.ravendb.client.documents.operations.revisions.RevisionsConfiguration;
import net.ravendb.client.documents.operations.timeSeries.TimeSeriesConfiguration;
import net.ravendb.client.documents.queries.sorting.SorterDefinition;
import net.ravendb.client.serverwide.*;
import net.ravendb.client.serverwide.operations.CreateDatabaseOperation;
import net.ravendb.client.serverwide.operations.DatabaseRecordBuilder;
import net.ravendb.client.serverwide.operations.DeleteDatabasesOperation;
import net.ravendb.client.serverwide.operations.GetDatabaseRecordOperation;
import net.ravendb.client.serverwide.operations.builder.IDatabaseRecordBuilderInitializer;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RavenDB_19938Test extends RemoteTestBase {

    @Test
    public void can_Create_Database_Via_Builder() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            String database = store.getDatabase() + "test";
            store.maintenance().server().send(new CreateDatabaseOperation(builder -> builder.regular(database)));

            try {
                DatabaseRecordWithEtag databaseRecord = store.maintenance().server().send(new GetDatabaseRecordOperation(database));
                assertThat(databaseRecord)
                        .isNotNull();
            } finally {
                store.maintenance().server().send(new DeleteDatabasesOperation(database, true));
            }
        }
    }

    @Test
    public void can_Create_Sharded_Database_Via_Builder() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            String database = store.getDatabase() + "test";
            store.maintenance().server().send(new CreateDatabaseOperation(builder -> builder.sharded(database, s -> s.addShard(0, b -> b.addNode("A")))));

            try {
                DatabaseRecordWithEtag databaseRecord = store.maintenance().server().send(new GetDatabaseRecordOperation(database));
                assertThat(databaseRecord)
                        .isNotNull();
            } finally {
                store.maintenance().server().send(new DeleteDatabasesOperation(database, true));
            }
        }
    }

    @Test
    public void regular() throws Exception {
        DatabaseRecord record = createDatabaseRecord(builder -> builder.regular("DB1"));
        assertThat(record.getDatabaseName())
                .isEqualTo("DB1");

        DatabaseTopology databaseTopology = new DatabaseTopology();
        databaseTopology.setMembers(Collections.singletonList("A"));

        record = createDatabaseRecord(builder -> builder.regular("DB1").withTopology(databaseTopology));

        assertThat(record.getTopology().getMembers())
                .hasSize(1)
                .contains("A");

        record = createDatabaseRecord(builder -> builder.regular("DB1").withTopology(topology -> topology.addNode("B").addNode("C")));

        assertThat(record.getTopology().getMembers())
                .hasSize(2)
                .contains("B")
                .contains("C");

        record = createDatabaseRecord(builder -> builder.regular("DB1").withReplicationFactor(3));

        assertThat(record.getTopology().getMembers())
                .hasSize(0);
        assertThat(record.getTopology().getReplicationFactor())
                .isEqualTo(3);

        record = createDatabaseRecord(builder -> builder.regular("DB1").disabled());

        assertThat(record.isDisabled())
                .isTrue();

        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setIdentityPartsSeparator('z');
        record = createDatabaseRecord(builder -> builder.regular("DB1").configureClient(clientConfiguration));

        assertThat(record.getClient().getIdentityPartsSeparator())
                .isEqualTo('z');

        DocumentsCompressionConfiguration documentsCompressionConfiguration = new DocumentsCompressionConfiguration();
        documentsCompressionConfiguration.setCollections(new String[]{ "Orders" });
        record = createDatabaseRecord(builder -> builder.regular("DB1").configureDocumentsCompression(documentsCompressionConfiguration));

        assertThat(record.getDocumentsCompression().getCollections())
                .hasSize(1)
                .contains("Orders");

        ExpirationConfiguration expirationConfiguration = new ExpirationConfiguration();
        expirationConfiguration.setDeleteFrequencyInSec(777L);
        record = createDatabaseRecord(builder -> builder.regular("DB1").configureExpiration(expirationConfiguration));

        assertThat(record.getExpiration().getDeleteFrequencyInSec())
                .isEqualTo(777L);

        RefreshConfiguration refreshConfiguration = new RefreshConfiguration();
        refreshConfiguration.setRefreshFrequencyInSec(333L);
        record = createDatabaseRecord(builder -> builder.regular("DB1").configureRefresh(refreshConfiguration));

        assertThat(record.getRefresh().getRefreshFrequencyInSec())
                .isEqualTo(333);

        RevisionsCollectionConfiguration revisionsCollectionConfiguration = new RevisionsCollectionConfiguration();
        revisionsCollectionConfiguration.setDisabled(true);

        RevisionsConfiguration revisionsConfiguration = new RevisionsConfiguration();
        revisionsConfiguration.setDefaultConfig(revisionsCollectionConfiguration);
        record = createDatabaseRecord(builder -> builder.regular("DB1").configureRevisions(revisionsConfiguration));

        assertThat(record.getRevisions().getDefaultConfig().isDisabled())
                .isTrue();

        StudioConfiguration studioConfiguration = new StudioConfiguration();
        studioConfiguration.setEnvironment(StudioEnvironment.PRODUCTION);
        record = createDatabaseRecord(builder -> builder.regular("DB1").configureStudio(studioConfiguration));

        assertThat(record.getStudio().getEnvironment())
                .isEqualTo(StudioEnvironment.PRODUCTION);

        TimeSeriesConfiguration timeSeriesConfiguration = new TimeSeriesConfiguration();
        timeSeriesConfiguration.setPolicyCheckFrequency(Duration.ofSeconds(555));

        record = createDatabaseRecord(builder -> builder.regular("DB1").configureTimeSeries(timeSeriesConfiguration));
        assertThat(record.getTimeSeries().getPolicyCheckFrequency().getSeconds())
                .isEqualTo(555);

        AnalyzerDefinition analyzer1 = new AnalyzerDefinition();
        analyzer1.setName("A1");

        AnalyzerDefinition analyzer2 = new AnalyzerDefinition();
        analyzer2.setName("A2");

        record = createDatabaseRecord(builder -> builder.regular("DB1").withAnalyzers(analyzer1).withAnalyzers(analyzer2));

        assertThat(record.getAnalyzers())
                .hasSize(2);

        SorterDefinition sorter1 = new SorterDefinition();
        sorter1.setName("S1");

        SorterDefinition sorter2 = new SorterDefinition();
        sorter2.setName("A2");

        record = createDatabaseRecord(builder -> builder.regular("DB1").withSorters(sorter1).withSorters(sorter2));

        assertThat(record.getSorters())
                .hasSize(2);

        record = createDatabaseRecord(builder -> builder.regular("DB1").encrypted());

        assertThat(record.isEncrypted())
                .isTrue();


        PeriodicBackupConfiguration backup1 = new PeriodicBackupConfiguration();
        backup1.setDisabled(true);
        record = createDatabaseRecord(builder -> builder.regular("DB1").withBackups(b -> b.addPeriodicBackup(backup1)));
        assertThat(record.getPeriodicBackups())
                .hasSize(1);
    }

    @Test
    public void sharded() throws Exception {

        DatabaseTopology t1 = new DatabaseTopology();
        t1.setMembers(Arrays.asList("B", "C"));
        DatabaseRecord record = createDatabaseRecord(
                builder -> builder.sharded("DB1",
                        topology -> topology
                                .addShard(0, shard -> shard.addNode("A"))
                                .addShard(1, t1)
                                .addShard(2, shard -> shard.addNode("C").addNode("A"))));

        assertThat(record.getSharding().getShards().get(0).getMembers())
                .hasSize(1)
                .contains("A");
        assertThat(record.getSharding().getShards().get(1).getMembers())
                .hasSize(2)
                .contains("B")
                .contains("C");
        assertThat(record.getSharding().getShards().get(2).getMembers())
                .hasSize(2)
                .contains("C")
                .contains("A");

        OrchestratorTopology orchestratorTopology = new OrchestratorTopology();
        orchestratorTopology.setMembers(Arrays.asList("A"));

        assertThatThrownBy(() -> createDatabaseRecord(builder -> builder.sharded("DB1", topology -> topology.orchestrator(orchestratorTopology))))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessageContaining("At least one shard is required. Use addShard to add a shard to the topology");

        OrchestratorTopology ot2 = new OrchestratorTopology();
        ot2.setMembers(Arrays.asList("A"));

        record = createDatabaseRecord(builder -> builder.sharded("DB1", topology -> topology.orchestrator(ot2).addShard(1, new DatabaseTopology())));

        assertThat(record.getSharding().getOrchestrator().getTopology().getMembers())
                .hasSize(1)
                .contains("A");

        record = createDatabaseRecord(builder -> builder.sharded("DB1",
                topology -> topology.orchestrator(
                        orchestrator -> orchestrator.addNode("B").addNode("C")).addShard(1, new DatabaseTopology())));

        assertThat(record.getSharding().getOrchestrator().getTopology().getMembers())
                .hasSize(2)
                .contains("B")
                .contains("C");
    }

    @Test
    public void common() throws Exception {
        DatabaseRecord record  = createDatabaseRecord(builder -> builder.regular("DB1").disabled());

        assertThat(record.isDisabled())
                .isTrue();

        record = createDatabaseRecord(builder -> builder.regular("DB1").encrypted());
        assertThat(record.isEncrypted())
                .isTrue();

        AnalyzerDefinition analyzerDefinition = new AnalyzerDefinition();
        analyzerDefinition.setName("A1");

        record = createDatabaseRecord(builder -> builder.regular("DB1").withAnalyzers(analyzerDefinition));
        assertThat(record.getAnalyzers())
                .containsKey("A1");

        IndexDefinition indexDefinition = new IndexDefinition();
        indexDefinition.setName("I1");
        record = createDatabaseRecord(builder -> builder.regular("DB1").withIndexes(indexDefinition));

        assertThat(record.getIndexes())
                .containsKey("I1");

        SorterDefinition sorterDefinition = new SorterDefinition();
        sorterDefinition.setName("S1");
        record = createDatabaseRecord(builder -> builder.regular("DB1").withSorters(sorterDefinition));

        assertThat(record.getSorters())
                .containsKey("S1");
    }

    private static DatabaseRecord createDatabaseRecord(Consumer<IDatabaseRecordBuilderInitializer> builder) {
        if (builder == null) {
            throw new IllegalArgumentException("Builder cannot be null");
        }

        IDatabaseRecordBuilderInitializer instance = DatabaseRecordBuilder.create();
        builder.accept(instance);

        return instance.toDatabaseRecord();
    }
}
