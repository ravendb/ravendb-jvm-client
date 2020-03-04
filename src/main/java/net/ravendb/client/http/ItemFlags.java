package net.ravendb.client.http;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum ItemFlags {
    NONE,
    NOT_FOUND,
    AGGRESSIVELY_CACHED
}
