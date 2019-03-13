package net.ravendb.client.serverwide.operations.certificates;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum SecurityClearance {
    UNAUTHENTICATED_CLIENTS,
    CLUSTER_ADMIN,
    CLUSTER_NODE,
    OPERATOR,
    VALID_USER
}
