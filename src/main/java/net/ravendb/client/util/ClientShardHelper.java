package net.ravendb.client.util;

import net.ravendb.client.primitives.Reference;
import net.ravendb.client.primitives.Tuple;

public class ClientShardHelper {

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
}
