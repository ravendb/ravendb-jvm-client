package net.ravendb.client.serverwide.sharding;

import net.ravendb.client.serverwide.DatabaseTopology;

import java.util.List;
import java.util.Map;

public class ShardingConfiguration {
    private OrchestratorConfiguration orchestrator;
    private Map<Integer, DatabaseTopology> shards;
    private List<ShardBucketRange> bucketRanges;
    private List<PrefixedShardingSetting> prefixed;
    private Map<Integer, ShardBucketMigration> bucketMigrations;

    private long migrationCutOffIndex;
    private String databaseId;

    public OrchestratorConfiguration getOrchestrator() {
        return orchestrator;
    }

    public void setOrchestrator(OrchestratorConfiguration orchestrator) {
        this.orchestrator = orchestrator;
    }

    public Map<Integer, DatabaseTopology> getShards() {
        return shards;
    }

    public void setShards(Map<Integer, DatabaseTopology> shards) {
        this.shards = shards;
    }

    public List<ShardBucketRange> getBucketRanges() {
        return bucketRanges;
    }

    public void setBucketRanges(List<ShardBucketRange> bucketRanges) {
        this.bucketRanges = bucketRanges;
    }

    public List<PrefixedShardingSetting> getPrefixed() {
        return prefixed;
    }

    public void setPrefixed(List<PrefixedShardingSetting> prefixed) {
        this.prefixed = prefixed;
    }

    public Map<Integer, ShardBucketMigration> getBucketMigrations() {
        return bucketMigrations;
    }

    public void setBucketMigrations(Map<Integer, ShardBucketMigration> bucketMigrations) {
        this.bucketMigrations = bucketMigrations;
    }

    public long getMigrationCutOffIndex() {
        return migrationCutOffIndex;
    }

    public void setMigrationCutOffIndex(long migrationCutOffIndex) {
        this.migrationCutOffIndex = migrationCutOffIndex;
    }

    public String getDatabaseId() {
        return databaseId;
    }

    public void setDatabaseId(String databaseId) {
        this.databaseId = databaseId;
    }
}
