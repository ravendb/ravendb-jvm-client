package net.ravendb.client.serverwide.operations;

import net.ravendb.client.documents.indexes.IndexDefinition;
import net.ravendb.client.documents.indexes.analysis.AnalyzerDefinition;
import net.ravendb.client.documents.operations.backups.PeriodicBackupConfiguration;
import net.ravendb.client.documents.operations.configuration.ClientConfiguration;
import net.ravendb.client.documents.operations.configuration.StudioConfiguration;
import net.ravendb.client.documents.operations.etl.RavenConnectionString;
import net.ravendb.client.documents.operations.etl.RavenEtlConfiguration;
import net.ravendb.client.documents.operations.etl.elasticSearch.ElasticSearchConnectionString;
import net.ravendb.client.documents.operations.etl.elasticSearch.ElasticSearchEtlConfiguration;
import net.ravendb.client.documents.operations.etl.olap.OlapConnectionString;
import net.ravendb.client.documents.operations.etl.olap.OlapEtlConfiguration;
import net.ravendb.client.documents.operations.etl.queue.QueueConnectionString;
import net.ravendb.client.documents.operations.etl.queue.QueueEtlConfiguration;
import net.ravendb.client.documents.operations.etl.sql.SqlConnectionString;
import net.ravendb.client.documents.operations.etl.sql.SqlEtlConfiguration;
import net.ravendb.client.documents.operations.expiration.ExpirationConfiguration;
import net.ravendb.client.documents.operations.refresh.RefreshConfiguration;
import net.ravendb.client.documents.operations.replication.ExternalReplication;
import net.ravendb.client.documents.operations.replication.PullReplicationAsSink;
import net.ravendb.client.documents.operations.replication.PullReplicationDefinition;
import net.ravendb.client.documents.operations.revisions.RevisionsConfiguration;
import net.ravendb.client.documents.operations.timeSeries.TimeSeriesConfiguration;
import net.ravendb.client.documents.queries.sorting.SorterDefinition;
import net.ravendb.client.serverwide.DatabaseRecord;
import net.ravendb.client.serverwide.DatabaseTopology;
import net.ravendb.client.serverwide.DocumentsCompressionConfiguration;
import net.ravendb.client.serverwide.OrchestratorTopology;
import net.ravendb.client.serverwide.operations.builder.*;
import net.ravendb.client.serverwide.operations.integrations.IntegrationConfigurations;
import net.ravendb.client.serverwide.operations.integrations.postgreSql.PostgreSqlConfiguration;
import net.ravendb.client.serverwide.sharding.OrchestratorConfiguration;
import net.ravendb.client.serverwide.sharding.ShardingConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class DatabaseRecordBuilder implements IDatabaseRecordBuilderInitializer, IDatabaseRecordBuilder,
        IEtlConfigurationBuilder,
        IConnectionStringConfigurationBuilder,
        IBackupConfigurationBuilder,
        IIntegrationConfigurationBuilder,
        IReplicationConfigurationBuilder,
        IShardedDatabaseRecordBuilder,
        IShardedTopologyConfigurationBuilder
{

    public static IDatabaseRecordBuilderInitializer create() {
        return new DatabaseRecordBuilder();
    }

    private DatabaseTopology _shardTopology;
    private final DatabaseRecord _databaseRecord;

    private DatabaseRecordBuilder() {
        _databaseRecord = new DatabaseRecord();
    }

    @Override
    public IBackupConfigurationBuilder addPeriodicBackup(PeriodicBackupConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot null");
        }

        if (_databaseRecord.getPeriodicBackups() == null) {
            _databaseRecord.setPeriodicBackups(new ArrayList<>());
        }

        _databaseRecord.getPeriodicBackups().add(configuration);

        return this;
    }

    @Override
    public IConnectionStringConfigurationBuilder addRavenConnectionString(RavenConnectionString connectionString) {
        if (connectionString == null) {
            throw new IllegalArgumentException("ConnectionString cannot be null");
        }

        if (_databaseRecord.getRavenConnectionStrings() == null) {
            _databaseRecord.setRavenConnectionStrings(new HashMap<>());
        }

        _databaseRecord.getRavenConnectionStrings().put(connectionString.getName(), connectionString);
        return this;
    }

    @Override
    public IConnectionStringConfigurationBuilder addSqlConnectionString(SqlConnectionString connectionString) {
        if (connectionString == null) {
            throw new IllegalArgumentException("ConnectionString cannot be null");
        }

        if (_databaseRecord.getSqlConnectionStrings() == null) {
            _databaseRecord.setSqlConnectionStrings(new HashMap<>());
        }

        _databaseRecord.getSqlConnectionStrings().put(connectionString.getName(), connectionString);
        return this;
    }

    @Override
    public IConnectionStringConfigurationBuilder addOlapConnectionString(OlapConnectionString connectionString) {
        if (connectionString == null) {
            throw new IllegalArgumentException("ConnectionString cannot be null");
        }

        if (_databaseRecord.getOlapConnectionStrings() == null) {
            _databaseRecord.setOlapConnectionStrings(new HashMap<>());
        }

        _databaseRecord.getOlapConnectionStrings().put(connectionString.getName(), connectionString);
        return this;
    }

    @Override
    public IConnectionStringConfigurationBuilder addElasticSearchConnectionString(ElasticSearchConnectionString connectionString) {
        if (connectionString == null) {
            throw new IllegalArgumentException("ConnectionString cannot be null");
        }

        if (_databaseRecord.getElasticSearchConnectionStrings() == null) {
            _databaseRecord.setElasticSearchConnectionStrings(new HashMap<>());
        }

        _databaseRecord.getElasticSearchConnectionStrings().put(connectionString.getName(), connectionString);
        return this;
    }

    @Override
    public IConnectionStringConfigurationBuilder addQueueConnectionString(QueueConnectionString connectionString) {
        if (connectionString == null) {
            throw new IllegalArgumentException("ConnectionString cannot be null");
        }

        if (_databaseRecord.getQueueConnectionStrings() == null) {
            _databaseRecord.setQueueConnectionStrings(new HashMap<>());
        }

        _databaseRecord.getQueueConnectionStrings().put(connectionString.getName(), connectionString);
        return this;
    }

    public IDatabaseRecordBuilder regular(String databaseName) {
        withName(databaseName);
        return this;
    }

    @Override
    public IShardedDatabaseRecordBuilder sharded(String databaseName, Consumer<IShardedTopologyConfigurationBuilder> builder) {
        if (builder == null) {
            throw new IllegalArgumentException("Builder cannot be null");
        }

        withName(databaseName);

        _databaseRecord.setSharding(new ShardingConfiguration());

        builder.accept(this);

        if (_databaseRecord.getSharding().getShards() == null || _databaseRecord.getSharding().getShards().isEmpty()) {
            throw new IllegalStateException("At least one shard is required. Use addShard to add a shard to the topology");
        }

        return this;
    }

    @Override
    public IEtlConfigurationBuilder addRavenEtl(RavenEtlConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }

        if (_databaseRecord.getRavenEtls() == null) {
            _databaseRecord.setRavenEtls(new ArrayList<>());
        }
        _databaseRecord.getRavenEtls().add(configuration);

        return this;
    }

    @Override
    public IEtlConfigurationBuilder addSqlEtl(SqlEtlConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }

        if (_databaseRecord.getSqlEtls() == null) {
            _databaseRecord.setSqlEtls(new ArrayList<>());
        }
        _databaseRecord.getSqlEtls().add(configuration);

        return this;
    }

    @Override
    public IEtlConfigurationBuilder addElasticSearchEtl(ElasticSearchEtlConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }

        if (_databaseRecord.getElasticSearchEtls() == null) {
            _databaseRecord.setElasticSearchEtls(new ArrayList<>());
        }

        _databaseRecord.getElasticSearchEtls().add(configuration);
        return this;
    }

    @Override
    public IEtlConfigurationBuilder addOlapEtl(OlapEtlConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }

        if (_databaseRecord.getOlapEtls() == null) {
            _databaseRecord.setOlapEtls(new ArrayList<>());
        }

        _databaseRecord.getOlapEtls().add(configuration);
        return this;
    }

    @Override
    public IEtlConfigurationBuilder addQueueEtl(QueueEtlConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }

        if (_databaseRecord.getQueueEtls() == null) {
            _databaseRecord.setQueueEtls(new ArrayList<>());
        }

        _databaseRecord.getQueueEtls().add(configuration);
        return this;
    }

    @Override
    public IIntegrationConfigurationBuilder configurePostgreSql(PostgreSqlConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }

        if (_databaseRecord.getIntegrations() == null) {
            _databaseRecord.setIntegrations(new IntegrationConfigurations());
        }

        _databaseRecord.getIntegrations().setPostgreSql(configuration);

        return this;
    }

    @Override
    public IReplicationConfigurationBuilder addExternalReplication(ExternalReplication configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }

        if (_databaseRecord.getExternalReplications() == null) {
            _databaseRecord.setExternalReplications(new ArrayList<>());
        }

        _databaseRecord.getExternalReplications().add(configuration);
        return this;
    }

    @Override
    public IReplicationConfigurationBuilder addPullReplicationSink(PullReplicationAsSink configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }

        if (_databaseRecord.getSinkPullReplications() == null) {
            _databaseRecord.setSinkPullReplications(new ArrayList<>());
        }

        _databaseRecord.getSinkPullReplications().add(configuration);
        return this;
    }

    @Override
    public IReplicationConfigurationBuilder addPullReplicationHub(PullReplicationDefinition configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }

        if (_databaseRecord.getHubPullReplications() == null) {
            _databaseRecord.setHubPullReplications(new ArrayList<>());
        }

        _databaseRecord.getHubPullReplications().add(configuration);
        return this;
    }

    @Override
    public IDatabaseRecordBuilderBase encrypted() {
        _databaseRecord.setEncrypted(true);
        return this;
    }

    @Override
    public IDatabaseRecordBuilderBase withLockMode(DatabaseRecord.DatabaseLockMode lockMode) {
        _databaseRecord.setLockMode(lockMode);
        return this;
    }

    @Override
    public IDatabaseRecordBuilderBase configureDocumentsCompression(DocumentsCompressionConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }

        _databaseRecord.setDocumentsCompression(configuration);
        return this;
    }

    @Override
    public IDatabaseRecordBuilderBase withSorters(SorterDefinition... sorterDefinitions) {
        if (sorterDefinitions == null || sorterDefinitions.length == 0) {
            return this;
        }

        if (_databaseRecord.getSorters() == null) {
            _databaseRecord.setSorters(new HashMap<>());
        }

        for (SorterDefinition sorterDefinition : sorterDefinitions) {
            _databaseRecord.getSorters().put(sorterDefinition.getName(), sorterDefinition);
        }

        return this;
    }

    @Override
    public IDatabaseRecordBuilderBase withAnalyzers(AnalyzerDefinition... analyzerDefinitions) {
        if (analyzerDefinitions == null || analyzerDefinitions.length == 0) {
            return this;
        }

        if (_databaseRecord.getAnalyzers() == null) {
            _databaseRecord.setAnalyzers(new HashMap<>());
        }

        for (AnalyzerDefinition analyzerDefinition : analyzerDefinitions) {
            _databaseRecord.getAnalyzers().put(analyzerDefinition.getName(), analyzerDefinition);
        }

        return this;
    }

    @Override
    public IDatabaseRecordBuilderBase withIndexes(IndexDefinition... indexDefinitions) {
        if (indexDefinitions == null || indexDefinitions.length == 0) {
            return this;
        }

        if (_databaseRecord.getIndexes() == null) {
            _databaseRecord.setIndexes(new HashMap<>());
        }

        for (IndexDefinition indexDefinition : indexDefinitions) {
            _databaseRecord.getIndexes().put(indexDefinition.getName(), indexDefinition);
        }

        return this;
    }

    @Override
    public IDatabaseRecordBuilderBase withSettings(Map<String, String> settings) {
        if (settings == null) {
            throw new IllegalArgumentException("Settings cannot be null");
        }

        _databaseRecord.setSettings(settings);
        return this;
    }

    @Override
    public IDatabaseRecordBuilderBase withSettings(Consumer<Map<String, String>> builder) {
        if (builder == null) {
            throw new IllegalArgumentException("Builder cannot be null");
        }

        _databaseRecord.setSettings(new HashMap<>());
        builder.accept(_databaseRecord.getSettings());

        return this;
    }

    @Override
    public IDatabaseRecordBuilderBase configureRevisions(RevisionsConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        _databaseRecord.setRevisions(configuration);
        return this;
    }

    @Override
    public IDatabaseRecordBuilderBase withEtls(Consumer<IEtlConfigurationBuilder> builder) {
        if (builder == null) {
            throw new IllegalArgumentException("Builder cannot be null");
        }

        builder.accept(this);
        return this;
    }

    @Override
    public IDatabaseRecordBuilderBase withBackups(Consumer<IBackupConfigurationBuilder> builder) {
        if (builder == null) {
            throw new IllegalArgumentException("Builder cannot be null");
        }

        builder.accept(this);
        return this;
    }

    @Override
    public IDatabaseRecordBuilderBase withReplication(Consumer<IReplicationConfigurationBuilder> builder) {
        if (builder == null) {
            throw new IllegalArgumentException("Builder cannot be null");
        }

        builder.accept(this);
        return this;
    }

    @Override
    public IDatabaseRecordBuilderBase withConnectionStrings(Consumer<IConnectionStringConfigurationBuilder> builder) {
        if (builder == null) {
            throw new IllegalArgumentException("Builder cannot be null");
        }

        builder.accept(this);

        return this;
    }

    @Override
    public IDatabaseRecordBuilderBase configureClient(ClientConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        _databaseRecord.setClient(configuration);
        return this;
    }

    @Override
    public IDatabaseRecordBuilderBase configureStudio(StudioConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        _databaseRecord.setStudio(configuration);
        return this;
    }

    @Override
    public IDatabaseRecordBuilderBase configureRefresh(RefreshConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }

        _databaseRecord.setRefresh(configuration);
        return this;
    }

    @Override
    public IDatabaseRecordBuilderBase configureExpiration(ExpirationConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }

        _databaseRecord.setExpiration(configuration);
        return this;
    }

    @Override
    public IDatabaseRecordBuilderBase configureTimeSeries(TimeSeriesConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }

        _databaseRecord.setTimeSeries(configuration);
        return this;
    }

    @Override
    public IDatabaseRecordBuilderBase withIntegrations(Consumer<IIntegrationConfigurationBuilder> builder) {
        if (builder == null) {
            throw new IllegalArgumentException("Builder cannot be null");
        }

        builder.accept(this);
        return this;
    }

    @Override
    public DatabaseRecord toDatabaseRecord() {
        return _databaseRecord;
    }

    @Override
    public IDatabaseRecordBuilderBase disabled() {
        _databaseRecord.setDisabled(true);
        return this;
    }


    @Override
    public IShardedTopologyConfigurationBuilder orchestrator(OrchestratorTopology topology) {
        if (topology == null) {
            throw new IllegalArgumentException("Topology cannot be null");
        }

        if (_databaseRecord.getSharding().getOrchestrator() == null) {
            _databaseRecord.getSharding().setOrchestrator(new OrchestratorConfiguration());
        }
        _databaseRecord.getSharding().getOrchestrator().setTopology(topology);
        return this;
    }

    @Override
    public IShardedTopologyConfigurationBuilder orchestrator(Consumer<IOrchestratorTopologyConfigurationBuilder> builder) {
        if (builder == null) {
            throw new IllegalArgumentException("Builder cannot be null");
        }

        builder.accept(new OrchestratorTopologyConfigurationBuilder());
        return this;
    }

    private class OrchestratorTopologyConfigurationBuilder implements IOrchestratorTopologyConfigurationBuilder {
        @Override
        public IOrchestratorTopologyConfigurationBuilder addNode(String nodeTag) {
            if (nodeTag == null) {
                throw new IllegalArgumentException("NodeTag cannot be null");
            }

            if (_databaseRecord.getSharding().getOrchestrator() == null) {
                _databaseRecord.getSharding().setOrchestrator(new OrchestratorConfiguration());
            }

            if (_databaseRecord.getSharding().getOrchestrator().getTopology() == null) {
                _databaseRecord.getSharding().getOrchestrator().setTopology(new OrchestratorTopology());
            }

            _databaseRecord.getSharding().getOrchestrator().getTopology().getMembers().add(nodeTag);

            return this;
        }


        @Override
        public IOrchestratorTopologyConfigurationBuilder enableDynamicNodesDistribution() {
            if (_databaseRecord.getSharding().getOrchestrator() == null) {
                _databaseRecord.getSharding().setOrchestrator(new OrchestratorConfiguration());
            }

            if (_databaseRecord.getSharding().getOrchestrator().getTopology() == null) {
                _databaseRecord.getSharding().getOrchestrator().setTopology(new OrchestratorTopology());
            }

            _databaseRecord.getSharding().getOrchestrator().getTopology().setDynamicNodesDistribution(true);

            return this;
        }
    }

    @Override
    public IShardedTopologyConfigurationBuilder addShard(int shardNumber, DatabaseTopology topology) {
        if (topology == null) {
            throw new IllegalArgumentException("Topology cannot be null");
        }

        if (_databaseRecord.getSharding().getShards() == null) {
            _databaseRecord.getSharding().setShards(new HashMap<>());
        }

        _databaseRecord.getSharding().getShards().put(shardNumber, topology);
        return this;
    }

    @Override
    public IShardedTopologyConfigurationBuilder addShard(int shardNumber, Consumer<IShardTopologyConfigurationBuilder> builder) {
        if (builder == null) {
            throw new IllegalArgumentException("Builder cannot be null");
        }

        _shardTopology = new DatabaseTopology();
        try {
            builder.accept(new ShardTopologyConfigurationBuilder());

            if (_databaseRecord.getSharding().getShards() == null) {
                _databaseRecord.getSharding().setShards(new HashMap<>());
            }
            _databaseRecord.getSharding().getShards().put(shardNumber, _shardTopology);
        } finally {
            _shardTopology = null;
        }

        return this;
    }

    private class ShardTopologyConfigurationBuilder implements IShardTopologyConfigurationBuilder {
        @Override
        public IShardTopologyConfigurationBuilder addNode(String nodeTag) {
            if (nodeTag == null) {
                throw new IllegalArgumentException("NodeTag cannot be null");
            }

            _shardTopology.getMembers().add(nodeTag);
            return this;
        }

        @Override
        public IShardTopologyConfigurationBuilder enableDynamicNodesDistribution() {
            _shardTopology.setDynamicNodesDistribution(true);
            return this;
        }
    }

    @Override
    public IDatabaseRecordBuilderBase withTopology(DatabaseTopology topology) {
        if (topology == null) {
            throw new IllegalArgumentException("Topology cannot be null");
        }

        _databaseRecord.setTopology(topology);
        return this;
    }

    @Override
    public IDatabaseRecordBuilderBase withTopology(Consumer<ITopologyConfigurationBuilder> builder) {
        if (builder == null) {
            throw new IllegalArgumentException("Builder cannot be null");
        }

        builder.accept(new TopologyConfigurationBuilder());
        return this;
    }

    private class TopologyConfigurationBuilder implements ITopologyConfigurationBuilder {
        @Override
        public ITopologyConfigurationBuilder addNode(String nodeTag) {
            if (_databaseRecord.getTopology() == null) {
                _databaseRecord.setTopology(new DatabaseTopology());
            }

            _databaseRecord.getTopology().getMembers().add(nodeTag);

            return this;
        }

        @Override
        public ITopologyConfigurationBuilder enableDynamicNodesDistribution() {
            if (_databaseRecord.getSharding().getOrchestrator() == null) {
                _databaseRecord.getSharding().setOrchestrator(new OrchestratorConfiguration());
            }

            if (_databaseRecord.getSharding().getOrchestrator().getTopology() == null) {
                _databaseRecord.getSharding().getOrchestrator().setTopology(new OrchestratorTopology());
            }

            _databaseRecord.getSharding().getOrchestrator().getTopology().setDynamicNodesDistribution(true);
            return this;
        }
    }

    @Override
    public IDatabaseRecordBuilderBase withReplicationFactor(int replicationFactor) {
        if (_databaseRecord.getTopology() == null) {
            _databaseRecord.setTopology(new DatabaseTopology());
        }

        _databaseRecord.getTopology().setReplicationFactor(replicationFactor);
        return this;
    }

    private void withName(String databaseName) {
        _databaseRecord.setDatabaseName(databaseName);
    }

}

