package net.ravendb.client.serverwide.sharding;

import java.util.List;

public class PrefixedShardingSetting {
    private String prefix;
    private List<Integer> shards;

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public List<Integer> getShards() {
        return shards;
    }

    public void setShards(List<Integer> shards) {
        this.shards = shards;
    }
}
