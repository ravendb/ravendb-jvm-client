package net.ravendb.client.documents.changes;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum IndexChangeTypes {
    NONE,

    BATCH_COMPLETED,

    INDEX_ADDED,
    INDEX_REMOVED,

    INDEX_DEMOTED_TO_IDLE,
    INDEX_PROMOTED_FROM_IDLE,

    INDEX_DEMOTED_TO_DISABLED,

    INDEX_MARKED_AS_ERRORED,

    SIDE_BY_SIDE_REPLACE,

    RENAMED,
    INDEX_PAUSED,
    LOCK_MODE_CHANGED,
    PRIORITY_CHANGED
}
