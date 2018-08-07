package net.ravendb.client.documents.session;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum TransactionMode {
    SINGLE_NODE,
    CLUSTER_WIDE
}
