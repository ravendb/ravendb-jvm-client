package net.ravendb.client.documents.operations.backups.sharding;

import java.util.Map;

public class ShardedRestoreSettings {
    private Map<String, SingleShardRestoreSetting> shards;

    public Map<String, SingleShardRestoreSetting> getShards() {
        return shards;
    }

    public void setShards(Map<String, SingleShardRestoreSetting> shards) {
        this.shards = shards;
    }
}
