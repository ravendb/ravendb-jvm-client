package net.ravendb.client.documents.smuggler;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum DatabaseItemType {
    NONE,

    DOCUMENTS,
    REVISION_DOCUMENTS,
    INDEXES,
    IDENTITIES,
    TOMBSTONES,
    LEGACY_ATTACHMENTS,
    CONFLICTS,
    COMPARE_EXCHANGE,
    LEGACY_DOCUMENT_DELETIONS,
    LEGACY_ATTACHMENT_DELETIONS,
    DATABASE_RECORD,
    UNKNOWN,
    COUNTERS,
    ATTACHMENTS
}
