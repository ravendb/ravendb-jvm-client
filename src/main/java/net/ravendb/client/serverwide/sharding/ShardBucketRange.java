package net.ravendb.client.serverwide.sharding;

public class ShardBucketRange {
    private int bucketRangeStart;
    private int shardNumber;

    public int getBucketRangeStart() {
        return bucketRangeStart;
    }

    public void setBucketRangeStart(int bucketRangeStart) {
        this.bucketRangeStart = bucketRangeStart;
    }

    public int getShardNumber() {
        return shardNumber;
    }

    public void setShardNumber(int shardNumber) {
        this.shardNumber = shardNumber;
    }
}
