package net.ravendb.client.http;

import net.ravendb.client.primitives.UseSharpEnum;
/**
 * Defines the behavior for balancing read operations across multiple nodes.
 *
 * <p>Possible values:</p>
 * <ul>
 *   <li><b>NONE</b> – No read balancing is applied; all read operations are sent to the preferred node.</li>
 *   <li><b>ROUND_ROBIN</b> – Read operations are distributed across nodes in a round-robin fashion, balancing the load.</li>
 *   <li><b>FASTEST_NODE</b> – Read operations are directed to the fastest node available at the time of the request.</li>
 * </ul>
 */
@UseSharpEnum
public enum ReadBalanceBehavior {
    NONE,
    ROUND_ROBIN,
    FASTEST_NODE
}
