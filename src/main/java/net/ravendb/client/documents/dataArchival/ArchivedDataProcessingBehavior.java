package net.ravendb.client.documents.dataArchival;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum ArchivedDataProcessingBehavior {
    EXCLUDE_ARCHIVED,
    INCLUDE_ARCHIVED,
    ARCHIVED_ONLY

}
