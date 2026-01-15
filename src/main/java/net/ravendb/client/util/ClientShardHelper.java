package net.ravendb.client.util;

import net.jpountz.xxhash.XXHashFactory;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.primitives.Tuple;
import net.ravendb.client.serverwide.sharding.PrefixedShardingSetting;
import net.ravendb.client.serverwide.sharding.ShardBucketRange;
import net.ravendb.client.serverwide.sharding.ShardingConfiguration;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ClientShardHelper {

    public static final int NUMBER_OF_BUCKETS = 1024 * 1024;
    public static String toShardName(String database, int shardNumber) {
        if (isShardName(database)) {
            throw new IllegalArgumentException("Expected a non shard name but got " + database);
        }

        if (shardNumber < 0) {
            throw new IllegalArgumentException("Shard number must be non-negative");
        }

        return database + "$" + shardNumber;
    }

    public static String toDatabaseName(String shardName) {
        int shardNumberPosition = shardName.indexOf("$");
        if (shardNumberPosition == -1) {
            return shardName;
        }

        return shardName.substring(0, shardNumberPosition);
    }

    public static boolean tryGetShardNumberAndDatabaseName(String databaseName, Reference<Tuple<String, Integer>> dbNameAndShardNumber) {
        int index = databaseName.indexOf("$");
        int shardNumber = -1;
        String shardedDatabaseName;

        if (index != -1) {
            String slice = databaseName.substring(index + 1);
            shardedDatabaseName = databaseName.substring(0, index);
            shardNumber = Integer.parseInt(slice, 10);

            dbNameAndShardNumber.value = Tuple.create(shardedDatabaseName, shardNumber);

            return true;
        }

        shardedDatabaseName = databaseName;
        dbNameAndShardNumber.value = Tuple.create(shardedDatabaseName, shardNumber);
        return false;
    }

    public static Integer getShardNumberFromDatabaseName(String databaseName) {
        Reference<Tuple<String, Integer>> tupleReference = new Reference<>();
        if (tryGetShardNumberAndDatabaseName(databaseName, tupleReference)) {
            return tupleReference.value.second;
        }

        return null;
    }

    public static boolean isShardName(String shardName) {
        return shardName.contains("$");
    }

    public static int getShardNumberFor(ShardingConfiguration configuration, String id) {
        int bucket = getBucketFor(configuration, id);
        return findBucketShard(configuration.getBucketRanges(), bucket);
    }

    public static int getBucketFor(ShardingConfiguration configuration, String id) {
        byte[] lowerId = id.toLowerCase().getBytes(StandardCharsets.UTF_8);
        return getBucketFor(configuration, lowerId);
    }

    public static int getBucketFor(ShardingConfiguration configuration, byte[] lowerId) {
        int bucket = getBucketFor(lowerId);

        if (configuration != null && configuration.getPrefixed() != null) {
            for (PrefixedShardingSetting setting : configuration.getPrefixed()) {
                if (startsWith(lowerId, setting.getPrefixBytesLowerCase())) {
                    bucket += setting.getBucketRangeStart();
                    break;
                }
            }
        }
        return bucket;
    }

    public static int findBucketShard(List<ShardBucketRange> ranges, int bucket) {
        int prefixRange = bucket >> 20;
        for (int i = 0; i < ranges.size() - 1; i++) {
            int bucketRangeStart = ranges.get(i).getBucketRangeStart();
            if ((bucketRangeStart >> 20) != prefixRange) continue;

            int nextBucketRangeStart = ranges.get(i + 1).getBucketRangeStart();
            if (bucket < nextBucketRangeStart) return ranges.get(i).getShardNumber();
        }

        return ranges.get(ranges.size() - 1).getShardNumber();
    }

    private static int getBucketFor(byte[] buffer) {
        int len = buffer.length;
        int start = 0;

        for (int i = len - 1; i > 0; i--) {
            if (buffer[i] != (byte) '$') continue;
            start = i + 1;
            break;
        }

        byte[] slice = new byte[len - start];
        System.arraycopy(buffer, start, slice, 0, len - start);

        XXHashFactory factory = XXHashFactory.fastestInstance();
        long hash = factory.hash64().hash(slice, 0, slice.length, 0);

        return (int) (hash % NUMBER_OF_BUCKETS);
    }

    private static boolean startsWith(byte[] array, byte[] prefix) {
        if (array.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (array[i] != prefix[i]) return false;
        }
        return true;
    }
}
