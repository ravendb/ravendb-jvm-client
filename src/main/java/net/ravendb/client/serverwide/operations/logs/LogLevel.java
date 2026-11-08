package net.ravendb.client.serverwide.operations.logs;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum LogLevel {
    TRACE,
    DEBUG,
    INFO,
    WARN,
    ERROR,
    FATAL,
    OFF
}
