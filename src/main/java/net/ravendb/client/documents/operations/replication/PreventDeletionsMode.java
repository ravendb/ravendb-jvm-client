package net.ravendb.client.documents.operations.replication;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum PreventDeletionsMode {
    NONE,
    PREVENT_SINK_TO_HUB_DELETIONS
}
