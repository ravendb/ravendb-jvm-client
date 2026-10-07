package net.ravendb.client.serverwide.operations.certificates;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum SsoProvider {
    GITHUB,
    GOOGLE,
    MICROSOFT,
    WINDOWS
}
