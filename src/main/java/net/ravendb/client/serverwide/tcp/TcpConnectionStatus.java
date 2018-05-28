package net.ravendb.client.serverwide.tcp;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum TcpConnectionStatus {
    OK,
    AUTHORIZATION_FAILED,
    TCP_VERSION_MISMATCH
}

