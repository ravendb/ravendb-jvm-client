package net.ravendb.abstractions.cluster;

public enum ClusterBehavior {
    NONE,
    READ_FROM_LEADER_WRITE_TO_LEADER,
    READ_FROM_LEADER_WRITE_TO_LEADER_WITH_FAILOVERS,
    READ_FROM_ALL_WRITE_TO_LEADER,
    READ_FROM_ALL_WRITE_TO_LEADER_WITH_FAILOVERS;
}
