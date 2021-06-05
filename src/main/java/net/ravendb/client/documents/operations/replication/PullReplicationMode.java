package net.ravendb.client.documents.operations.replication;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum PullReplicationMode {
    NONE,
    HUB_TO_SINK,
    SINK_TO_HUB
}
