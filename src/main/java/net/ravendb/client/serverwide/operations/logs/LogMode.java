package net.ravendb.client.serverwide.operations.logs;

import net.ravendb.client.primitives.UseSharpEnum;

/**
 * @deprecated Not supported by RavenDB 7.x servers; use {@link LogLevel}
 */
@Deprecated
@UseSharpEnum
public enum LogMode {
    NONE,
    OPERATIONS,
    INFORMATION
}
