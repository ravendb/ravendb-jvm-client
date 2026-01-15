package net.ravendb.client.serverwide.sharding;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class PrefixedShardingSetting {
    private String prefix;
    private List<Integer> shards;
    private byte[] prefixBytesLowerCase;
    private int bucketRangeStart;

    public int getBucketRangeStart() {
        return bucketRangeStart;
    }
    public byte[] getPrefixBytesLowerCase() {
        if (prefixBytesLowerCase == null) {
            prefixBytesLowerCase = prefix.toLowerCase().getBytes(StandardCharsets.UTF_8);
        }
        return prefixBytesLowerCase;
    }

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
