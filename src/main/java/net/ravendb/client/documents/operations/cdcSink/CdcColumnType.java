package net.ravendb.client.documents.operations.cdcSink;

import net.ravendb.client.primitives.UseSharpEnum;

/**
 * Controls how a CDC Sink column is stored in the target RavenDB document.
 */
@UseSharpEnum
public enum CdcColumnType {

    /**
     * Store as a document property with standard type conversion.
     * int/smallint/bigint to long, real/float/double to double, numeric/decimal to decimal,
     * boolean to bool, date to date, timestamp/timestamptz to date-time,
     * uuid to string, varchar/text to string, arrays to JSON arrays.
     * JSON/JSONB columns are stored as plain strings unless explicitly marked as Json.
     */
    DEFAULT,

    /**
     * Parse the string value as its native JSON type in the document.
     * Handles all JSON value types: objects, arrays, strings, numbers, booleans,
     * and null. Use for json/jsonb columns in PostgreSQL, or nvarchar(max) with
     * JSON content in SQL Server. Without this type, JSON values are stored as
     * escaped strings.
     */
    JSON,

    /**
     * Store as a RavenDB attachment instead of a document property.
     * The binary format depends on the source type at runtime:
     * byte[] to binary (application/octet-stream),
     * string to UTF-8 text (text/plain),
     * float[]/double[] to raw vector data (application/octet-stream).
     */
    ATTACHMENT
}
