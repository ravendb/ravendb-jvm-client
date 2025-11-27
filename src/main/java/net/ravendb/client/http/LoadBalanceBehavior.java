package net.ravendb.client.http;

import net.ravendb.client.primitives.UseSharpEnum;
/**
 * Defines the behavior for load balancing client requests across multiple nodes.
 *
 * <p>Possible values:</p>
 * <ul>
 *   <li><b>NONE</b> – No load balancing is applied; all requests are sent to the preferred node.</li>
 *   <li><b>USE_SESSION_CONTEXT</b> – Requests are distributed based on the session context, balancing the load across available nodes.</li>
 * </ul>
 */
@UseSharpEnum
public enum LoadBalanceBehavior {
    NONE,
    USE_SESSION_CONTEXT
}
