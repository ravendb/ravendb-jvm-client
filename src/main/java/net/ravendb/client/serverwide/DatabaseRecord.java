package net.ravendb.client.serverwide;

import net.ravendb.client.documents.indexes.AutoIndexDefinition;
import net.ravendb.client.documents.indexes.IndexDefinition;
import net.ravendb.client.documents.indexes.RollingIndex;
import net.ravendb.client.documents.indexes.RollingIndexDeployment;
import net.ravendb.client.documents.indexes.analysis.AnalyzerDefinition;
import net.ravendb.client.documents.operations.backups.PeriodicBackupConfiguration;
import net.ravendb.client.documents.operations.configuration.ClientConfiguration;
import net.ravendb.client.documents.operations.configuration.StudioConfiguration;
import net.ravendb.client.documents.operations.dataArchival.DataArchivalConfiguration;
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
import net.ravendb.client.documents.operations.queueSink.QueueSinkConfiguration;
import net.ravendb.client.documents.operations.refresh.RefreshConfiguration;
import net.ravendb.client.documents.operations.replication.ExternalReplication;
import net.ravendb.client.documents.operations.replication.PullReplicationAsSink;
import net.ravendb.client.documents.operations.replication.PullReplicationDefinition;
import net.ravendb.client.documents.operations.revisions.RevisionsCollectionConfiguration;
import net.ravendb.client.documents.operations.revisions.RevisionsConfiguration;
import net.ravendb.client.documents.operations.timeSeries.TimeSeriesConfiguration;
import net.ravendb.client.documents.queries.sorting.SorterDefinition;
import net.ravendb.client.documents.operations.etl.RavenConnectionString;
import net.ravendb.client.primitives.UseSharpEnum;
import net.ravendb.client.serverwide.operations.integrations.IntegrationConfigurations;
import net.ravendb.client.serverwide.sharding.ShardingConfiguration;

import java.util.*;

public class DatabaseRecord {
    private String databaseName;
    private boolean disabled;
    private boolean encrypted;
    private long etagForBackup;
    private Map<String, DeletionInProgressStatus> deletionInProgress;
    private Map<String, RollingIndex> rollingIndexes;
    private DatabaseStateStatus databaseState;
    private DatabaseLockMode lockMode;
    private DatabaseTopology topology;
    private ShardingConfiguration sharding;
    private ConflictSolver conflictSolverConfig;
    private DocumentsCompressionConfiguration documentsCompression;
    private Map<String, SorterDefinition> sorters = new HashMap<>();
    private Map<String, AnalyzerDefinition> analyzers = new HashMap<>();
    private Map<String, IndexDefinition> indexes;
    private Map<String, List<IndexHistoryEntry>> indexesHistory;
    private Map<String, AutoIndexDefinition> autoIndexes;
    private Map<String, String> settings = new HashMap<>();
    private RevisionsConfiguration revisions;
    private TimeSeriesConfiguration timeSeries;
    private RevisionsCollectionConfiguration revisionsForConflicts;
    private ExpirationConfiguration expiration;
    private RefreshConfiguration refresh;

    private DataArchivalConfiguration dataArchival;
    private IntegrationConfigurations integrations;
    private List<PeriodicBackupConfiguration> periodicBackups = new ArrayList<>();
    private List<ExternalReplication> externalReplications = new ArrayList<>();
    private List<PullReplicationAsSink> sinkPullReplications = new ArrayList<>();
    private List<PullReplicationDefinition> hubPullReplications = new ArrayList<>();
    private Map<String, RavenConnectionString> ravenConnectionStrings = new HashMap<>();
    private Map<String, SqlConnectionString> sqlConnectionStrings = new HashMap<>();
    private Map<String, OlapConnectionString> olapConnectionStrings = new HashMap<>();

    private Map<String, ElasticSearchConnectionString> elasticSearchConnectionStrings = new HashMap<>();
    private Map<String, QueueConnectionString> queueConnectionStrings = new HashMap<>();
    private List<RavenEtlConfiguration> ravenEtls = new ArrayList<>();
    private List<SqlEtlConfiguration> sqlEtls = new ArrayList<>();
    private List<ElasticSearchEtlConfiguration> elasticSearchEtls = new ArrayList<>();
    private List<OlapEtlConfiguration> olapEtls = new ArrayList<>();
    private List<QueueEtlConfiguration> queueEtls = new ArrayList<>();
    private List<QueueSinkConfiguration> queueSinks = new ArrayList<>();
    private ClientConfiguration client;
    private StudioConfiguration studio;
    private long truncatedClusterTransactionCommandsCount;
    private Set<String> unusedDatabaseIds = new HashSet<>();

    public DatabaseRecord() {
    }

    public DatabaseRecord(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public Map<String, String> getSettings() {
        return settings;
    }

    public void setSettings(Map<String, String> settings) {
        this.settings = settings;
    }

    public ConflictSolver getConflictSolverConfig() {
        return conflictSolverConfig;
    }

    public void setConflictSolverConfig(ConflictSolver conflictSolverConfig) {
        this.conflictSolverConfig = conflictSolverConfig;
    }

    public DocumentsCompressionConfiguration getDocumentsCompression() {
        return documentsCompression;
    }

    public void setDocumentsCompression(DocumentsCompressionConfiguration documentsCompression) {
        this.documentsCompression = documentsCompression;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    public long getEtagForBackup() {
        return etagForBackup;
    }

    public void setEtagForBackup(long etagForBackup) {
        this.etagForBackup = etagForBackup;
    }

    public Map<String, DeletionInProgressStatus> getDeletionInProgress() {
        return deletionInProgress;
    }

    public void setDeletionInProgress(Map<String, DeletionInProgressStatus> deletionInProgress) {
        this.deletionInProgress = deletionInProgress;
    }

    public Map<String, RollingIndex> getRollingIndexes() {
        return rollingIndexes;
    }

    public void setRollingIndexes(Map<String, RollingIndex> rollingIndexes) {
        this.rollingIndexes = rollingIndexes;
    }

    public DatabaseTopology getTopology() {
        return topology;
    }

    public void setTopology(DatabaseTopology topology) {
        this.topology = topology;
    }

    public ShardingConfiguration getSharding() {
        return sharding;
    }

    public void setSharding(ShardingConfiguration sharding) {
        this.sharding = sharding;
    }

    public Map<String, SorterDefinition> getSorters() {
        return sorters;
    }

    public void setSorters(Map<String, SorterDefinition> sorters) {
        this.sorters = sorters;
    }

    public Map<String, AnalyzerDefinition> getAnalyzers() {
        return analyzers;
    }

    public void setAnalyzers(Map<String, AnalyzerDefinition> analyzers) {
        this.analyzers = analyzers;
    }

    public Map<String, IndexDefinition> getIndexes() {
        return indexes;
    }

    public void setIndexes(Map<String, IndexDefinition> indexes) {
        this.indexes = indexes;
    }

    public Map<String, AutoIndexDefinition> getAutoIndexes() {
        return autoIndexes;
    }

    public void setAutoIndexes(Map<String, AutoIndexDefinition> autoIndexes) {
        this.autoIndexes = autoIndexes;
    }

    public RevisionsConfiguration getRevisions() {
        return revisions;
    }

    public void setRevisions(RevisionsConfiguration revisions) {
        this.revisions = revisions;
    }

    public TimeSeriesConfiguration getTimeSeries() {
        return timeSeries;
    }

    public void setTimeSeries(TimeSeriesConfiguration timeSeries) {
        this.timeSeries = timeSeries;
    }

    public ExpirationConfiguration getExpiration() {
        return expiration;
    }

    public void setExpiration(ExpirationConfiguration expiration) {
        this.expiration = expiration;
    }

    public List<PeriodicBackupConfiguration> getPeriodicBackups() {
        return periodicBackups;
    }

    public void setPeriodicBackups(List<PeriodicBackupConfiguration> periodicBackups) {
        this.periodicBackups = periodicBackups;
    }

    public List<ExternalReplication> getExternalReplications() {
        return externalReplications;
    }

    public void setExternalReplications(List<ExternalReplication> externalReplications) {
        this.externalReplications = externalReplications;
    }

    public List<PullReplicationAsSink> getSinkPullReplications() {
        return sinkPullReplications;
    }

    public void setSinkPullReplications(List<PullReplicationAsSink> sinkPullReplications) {
        this.sinkPullReplications = sinkPullReplications;
    }

    public List<PullReplicationDefinition> getHubPullReplications() {
        return hubPullReplications;
    }

    public void setHubPullReplications(List<PullReplicationDefinition> hubPullReplications) {
        this.hubPullReplications = hubPullReplications;
    }

    public Map<String, RavenConnectionString> getRavenConnectionStrings() {
        return ravenConnectionStrings;
    }

    public void setRavenConnectionStrings(Map<String, RavenConnectionString> ravenConnectionStrings) {
        this.ravenConnectionStrings = ravenConnectionStrings;
    }

    public Map<String, SqlConnectionString> getSqlConnectionStrings() {
        return sqlConnectionStrings;
    }

    public void setSqlConnectionStrings(Map<String, SqlConnectionString> sqlConnectionStrings) {
        this.sqlConnectionStrings = sqlConnectionStrings;
    }

    public Map<String, OlapConnectionString> getOlapConnectionStrings() {
        return olapConnectionStrings;
    }

    public void setOlapConnectionStrings(Map<String, OlapConnectionString> olapConnectionStrings) {
        this.olapConnectionStrings = olapConnectionStrings;
    }

    public List<RavenEtlConfiguration> getRavenEtls() {
        return ravenEtls;
    }

    public void setRavenEtls(List<RavenEtlConfiguration> ravenEtls) {
        this.ravenEtls = ravenEtls;
    }

    public List<SqlEtlConfiguration> getSqlEtls() {
        return sqlEtls;
    }

    public void setSqlEtls(List<SqlEtlConfiguration> sqlEtls) {
        this.sqlEtls = sqlEtls;
    }

    public List<OlapEtlConfiguration> getOlapEtls() {
        return olapEtls;
    }

    public void setOlapEtls(List<OlapEtlConfiguration> olapEtls) {
        this.olapEtls = olapEtls;
    }

    public ClientConfiguration getClient() {
        return client;
    }

    public void setClient(ClientConfiguration client) {
        this.client = client;
    }

    public StudioConfiguration getStudio() {
        return studio;
    }

    public void setStudio(StudioConfiguration studio) {
        this.studio = studio;
    }

    public long getTruncatedClusterTransactionCommandsCount() {
        return truncatedClusterTransactionCommandsCount;
    }

    public void setTruncatedClusterTransactionCommandsCount(long truncatedClusterTransactionCommandsCount) {
        this.truncatedClusterTransactionCommandsCount = truncatedClusterTransactionCommandsCount;
    }

    public DatabaseStateStatus getDatabaseState() {
        return databaseState;
    }

    public void setDatabaseState(DatabaseStateStatus databaseState) {
        this.databaseState = databaseState;
    }

    public DatabaseLockMode getLockMode() {
        return lockMode;
    }

    public void setLockMode(DatabaseLockMode lockMode) {
        this.lockMode = lockMode;
    }

    public Map<String, List<IndexHistoryEntry>> getIndexesHistory() {
        return indexesHistory;
    }

    public void setIndexesHistory(Map<String, List<IndexHistoryEntry>> indexesHistory) {
        this.indexesHistory = indexesHistory;
    }

    public RevisionsCollectionConfiguration getRevisionsForConflicts() {
        return revisionsForConflicts;
    }

    public void setRevisionsForConflicts(RevisionsCollectionConfiguration revisionsForConflicts) {
        this.revisionsForConflicts = revisionsForConflicts;
    }

    public RefreshConfiguration getRefresh() {
        return refresh;
    }

    public void setRefresh(RefreshConfiguration refresh) {
        this.refresh = refresh;
    }

    public DataArchivalConfiguration getDataArchival() {
        return dataArchival;
    }

    public void setDataArchival(DataArchivalConfiguration dataArchival) {
        this.dataArchival = dataArchival;
    }

    public IntegrationConfigurations getIntegrations() {
        return integrations;
    }

    public void setIntegrations(IntegrationConfigurations integrations) {
        this.integrations = integrations;
    }

    public Set<String> getUnusedDatabaseIds() {
        return unusedDatabaseIds;
    }

    public Map<String, ElasticSearchConnectionString> getElasticSearchConnectionStrings() {
        return elasticSearchConnectionStrings;
    }

    public void setElasticSearchConnectionStrings(Map<String, ElasticSearchConnectionString> elasticSearchConnectionStrings) {
        this.elasticSearchConnectionStrings = elasticSearchConnectionStrings;
    }

    public Map<String, QueueConnectionString> getQueueConnectionStrings() {
        return queueConnectionStrings;
    }

    public void setQueueConnectionStrings(Map<String, QueueConnectionString> queueConnectionStrings) {
        this.queueConnectionStrings = queueConnectionStrings;
    }

    public List<ElasticSearchEtlConfiguration> getElasticSearchEtls() {
        return elasticSearchEtls;
    }

    public void setElasticSearchEtls(List<ElasticSearchEtlConfiguration> elasticSearchEtls) {
        this.elasticSearchEtls = elasticSearchEtls;
    }

    public List<QueueEtlConfiguration> getQueueEtls() {
        return queueEtls;
    }

    public void setQueueEtls(List<QueueEtlConfiguration> queueEtls) {
        this.queueEtls = queueEtls;
    }

    public List<QueueSinkConfiguration> getQueueSinks() {
        return queueSinks;
    }

    public void setQueueSinks(List<QueueSinkConfiguration> queueSinks) {
        this.queueSinks = queueSinks;
    }

    public void setUnusedDatabaseIds(Set<String> unusedDatabaseIds) {
        this.unusedDatabaseIds = unusedDatabaseIds;
    }

    public static class IndexHistoryEntry {
        private IndexDefinition definition;
        private String source;
        private Date createdAt;
        private Map<String, RollingIndexDeployment> rollingDeployment;

        public IndexDefinition getDefinition() {
            return definition;
        }

        public void setDefinition(IndexDefinition definition) {
            this.definition = definition;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public Date getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Date createdAt) {
            this.createdAt = createdAt;
        }

        public Map<String, RollingIndexDeployment> getRollingDeployment() {
            return rollingDeployment;
        }

        public void setRollingDeployment(Map<String, RollingIndexDeployment> rollingDeployment) {
            this.rollingDeployment = rollingDeployment;
        }
    }

    @UseSharpEnum
    public enum DatabaseLockMode {
        UNLOCK,
        PREVENT_DELETES_IGNORE,
        PREVENT_DELETES_ERROR
    }

    @UseSharpEnum
    public enum DatabaseStateStatus {
        NORMAL,
        RESTORE_IN_PROGRESS
    }
}
