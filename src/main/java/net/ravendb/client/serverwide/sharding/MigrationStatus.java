package net.ravendb.client.serverwide.sharding;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum MigrationStatus {
    PENDING,
    /**
     * source is in progress of sending the bucket
     */
    MOVING,
    /**
     * the source has completed to send everything he has
     * and the destinations member nodes start confirm having all docs
     * at this stage writes will still go to the source shard
     */
    MOVED,
    /**
     * all member nodes confirmed receiving the bucket
     * the mapping is updated so any traffic will go now to the destination
     * the source will start the cleanup process
     */
    OWNERSHIP_TRANSFERRED
}
