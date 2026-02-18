package net.ravendb.client.documents.operations.backups;

/**
 * S3 storage class options for Amazon S3 or S3‑compatible providers.
 */
public enum S3StorageClass {

    /**
     * S3 Glacier Deep Archive provides secure, durable object storage for long‑term archival.
     * Intended for data that is rarely, if ever, accessed and must be retained for long periods.
     */
    DEEP_ARCHIVE,

    /**
     * Glacier storage for archival objects with very infrequent access.
     * Durability 99.999999999%.
     */
    GLACIER,

    /**
     * Glacier Instant Retrieval storage class.
     */
    GLACIER_INSTANT_RETRIEVAL,

    /**
     * Intelligent‑Tiering automatically moves data between tiers based on access patterns,
     * reducing storage cost without lifecycle policies or manual transitions.
     */
    INTELLIGENT_TIERING,

    /**
     * One Zone‑Infrequent Access stores data in a single Availability Zone.
     * Durability 99.999999999%; Availability 99% yearly.
     */
    ONE_ZONE_INFREQUENT_ACCESS,

    /**
     * Reduced Redundancy provides the same availability as Standard but with lower durability.
     * Durability 99.99%; Availability 99.99% yearly.
     */
    REDUCED_REDUNDANCY,

    /**
     * Standard storage class, the default for S3.
     * Durability 99.999999999%; Availability 99.99% yearly.
     */
    STANDARD,

    /**
     * Standard‑Infrequent Access for long‑lived, infrequently accessed data such as backups.
     * Durability 99.999999999%; Availability 99.9% yearly.
     */
    STANDARD_INFREQUENT_ACCESS,

    /**
     * Express One Zone storage class for faster access.
     */
    EXPRESS_ONE_ZONE
}
