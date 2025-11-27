package net.ravendb.client.documents.session;

import net.ravendb.client.primitives.UseSharpEnum;
import net.ravendb.client.DocumentationUrls;

@UseSharpEnum
public enum TransactionMode {
    /**
     * Calling {@link DocumentSession#saveChanges()} will persist all modification to a node.
     * <p>This is the default mode to favor performance and availability</p>
     * <p>For more details visit: {@link DocumentationUrls.Session.Transactions#TransactionSupport}</p>
     */
    SINGLE_NODE,
    /**
     * Calling {@link DocumentSession#saveChanges()} will persist all modifications consistently across the entire cluster.
     * <p>This mode uses RAFT to ensure consistency in the cluster and require the majority of the nodes to be available</p>
     * <p>For more details visit: {@link DocumentationUrls.Session.Transactions#TransactionSupport}</p>
     */
    CLUSTER_WIDE
}
