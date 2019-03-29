package net.ravendb.client.documents.operations.ongoingTasks;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum  OngoingTaskState {
    ENABLED,
    DISABLED,
    PARTIALLY_ENABLED
}
