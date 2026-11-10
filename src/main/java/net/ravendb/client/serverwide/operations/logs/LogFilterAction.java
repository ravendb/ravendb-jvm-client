package net.ravendb.client.serverwide.operations.logs;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum LogFilterAction {
    /**
     * The filter doesn't want to decide whether to log or discard the message.
     */
    NEUTRAL,

    /**
     * The message should be logged.
     */
    LOG,

    /**
     * The message should not be logged.
     */
    IGNORE,

    /**
     * The message should be logged and processing should be finished.
     */
    LOG_FINAL,

    /**
     * The message should not be logged and processing should be finished.
     */
    IGNORE_FINAL
}
