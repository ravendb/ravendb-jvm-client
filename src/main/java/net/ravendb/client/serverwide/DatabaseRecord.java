package net.ravendb.client.serverwide;

import net.ravendb.client.documents.indexes.AutoIndexDefinition;
import net.ravendb.client.documents.indexes.IndexDefinition;
import net.ravendb.client.documents.operations.backups.PeriodicBackupConfiguration;
import net.ravendb.client.documents.operations.configuration.ClientConfiguration;
import net.ravendb.client.documents.operations.configuration.StudioConfiguration;
import net.ravendb.client.documents.operations.etl.RavenEtlConfiguration;
import net.ravendb.client.documents.operations.etl.sql.SqlConnectionString;
import net.ravendb.client.documents.operations.etl.sql.SqlEtlConfiguration;
import net.ravendb.client.documents.operations.expiration.ExpirationConfiguration;
import net.ravendb.client.documents.operations.replication.ExternalReplication;
import net.ravendb.client.documents.operations.replication.PullReplicationAsSink;
import net.ravendb.client.documents.operations.replication.PullReplicationDefinition;
import net.ravendb.client.documents.operations.revisions.RevisionsConfiguration;
import net.ravendb.client.documents.queries.sorting.SorterDefinition;
import net.ravendb.client.documents.operations.etl.RavenConnectionString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseRecord {
    private String databaseName;
    private boolean disabled;
    private boolean encrypted;
    private long etagForBackup;
    private Map<String, DeletionInProgressStatus> deletionInProgress;
    private DatabaseTopology topology;
    private ConflictSolver conflictSolverConfig;
    private Map<String, SorterDefinition> sorters = new HashMap<>();
    private Map<String, IndexDefinition> indexes;
    private Map<String, AutoIndexDefinition> autoIndexes;
    private Map<String, String> settings = new HashMap<>();
    private RevisionsConfiguration revisions;
    private ExpirationConfiguration expiration;
    private List<PeriodicBackupConfiguration> periodicBackups = new ArrayList<>();
    private List<ExternalReplication> externalReplications = new ArrayList<>();
    private List<PullReplicationAsSink> sinkPullReplications = new ArrayList<>();
    private List<PullReplicationDefinition> hubPullReplications = new ArrayList<>();
    private Map<String, RavenConnectionString> ravenConnectionStrings = new HashMap<>();
    private Map<String, SqlConnectionString> sqlConnectionStrings = new HashMap<>();
    private List<RavenEtlConfiguration> ravenEtls = new ArrayList<>();
    private List<SqlEtlConfiguration> sqlEtls = new ArrayList<>();
    private ClientConfiguration client;
    private StudioConfiguration studio;
    private long truncatedClusterTransactionCommandsCount;

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

    public DatabaseTopology getTopology() {
        return topology;
    }

    public void setTopology(DatabaseTopology topology) {
        this.topology = topology;
    }

    public Map<String, SorterDefinition> getSorters() {
        return sorters;
    }

    public void setSorters(Map<String, SorterDefinition> sorters) {
        this.sorters = sorters;
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
}
