package net.ravendb.client.serverwide;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum DeletionInProgressStatus {
    NO,
    SOFT_DELETE,
    HARD_DELETE
}
