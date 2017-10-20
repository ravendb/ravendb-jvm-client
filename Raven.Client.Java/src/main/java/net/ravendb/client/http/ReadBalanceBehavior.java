package net.ravendb.client.http;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum ReadBalanceBehavior {
    NONE,
    ROUND_ROBIN,
    FASTEST_NODE
}
