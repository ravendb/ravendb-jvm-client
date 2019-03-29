package net.ravendb.client.documents.operations.ongoingTasks;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum OngoingTaskConnectionStatus {
    NONE,
    ACTIVE,
    NOT_ACTIVE,
    RECONNECT,
    NOT_ON_THIS_NODE
}
