package net.ravendb.client.http;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum LoadBalanceBehavior {
    NONE,
    USE_SESSION_CONTEXT
}
