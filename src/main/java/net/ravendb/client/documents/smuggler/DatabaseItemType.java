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
    /**
     * @deprecated COUNTERS is not supported anymore. Will be removed in next major version of the product.
     */
    COUNTERS,
    ATTACHMENTS,
    COUNTER_GROUPS,
    SUBSCRIPTIONS,
    COMPARE_EXCHANGE_TOMBSTONES
}
