package net.ravendb.client.documents.operations.cdcSink;

/**
 * Controls how DELETE events are handled for a CDC Sink table (root or embedded).
 * When null (the default), DELETE events are processed normally — root documents are deleted,
 * embedded items are removed from the parent's array/map/value.
 */
public class CdcSinkOnDeleteConfig {

    private String patch;
    private boolean ignoreDeletes;

    /**
     * Gets the optional JavaScript patch that runs when a DELETE event is received.
     *
     * <p>
     * For root tables: {@code this} = the existing document, {@code $row} = raw CDC row (DELETE event data).
     * For embedded tables: {@code this} = the parent document, {@code $row} = the embedded row's DELETE event data.
     * </p>
     *
     * <p>
     * The patch runs before the delete is applied. Whether the delete proceeds afterward depends on
     * {@link #isIgnoreDeletes()}:
     * </p>
     * <ul>
     *   <li>{@code ignoreDeletes = false} (default): patch runs, then delete proceeds.</li>
     *   <li>{@code ignoreDeletes = true}: patch runs, delete is skipped.</li>
     * </ul>
     *
     * <p>
     * Typical uses are an audit trail (write a record to a separate audit document, then let the delete
     * proceed), the archive pattern (mark the document and prevent deletion), a conditional delete
     * (call {@code del(id(this))} explicitly despite {@code ignoreDeletes}), or snapshotting the last
     * known state onto the parent.
     * </p>
     * @return the JavaScript patch to run on DELETE, or null
     */
    public String getPatch() {
        return patch;
    }

    public void setPatch(String patch) {
        this.patch = patch;
    }

    /**
     * Gets a value indicating whether the DELETE operation is skipped — the document/item is kept.
     *
     * <p>
     * If a patch is also set, the patch runs first, then the delete is skipped. If no patch is set,
     * the DELETE event is silently discarded.
     * </p>
     *
     * <p>Use cases:</p>
     * <ul>
     *   <li>Archive pattern: set {@code ignoreDeletes = true} with a patch that marks the document as archived.</li>
     *   <li>Append-only data (e.g. audit logs) where rows should never be removed.</li>
     *   <li>When the embedded table's primary key doesn't include the join column to the parent and you don't
     *       want to set up REPLICA IDENTITY FULL (PostgreSQL-specific; SQL Server CDC always includes all
     *       tracked columns in change rows).</li>
     * </ul>
     *
     * <p>
     * When {@code ignoreDeletes} is true (without a patch), the CDC process does not need the join column to be
     * present in DELETE events, so for PostgreSQL the default REPLICA IDENTITY (primary key only) is sufficient
     * regardless of whether the PK includes the join column.
     * </p>
     * @return true when DELETE events do not remove the document/item
     */
    public boolean isIgnoreDeletes() {
        return ignoreDeletes;
    }

    public void setIgnoreDeletes(boolean ignoreDeletes) {
        this.ignoreDeletes = ignoreDeletes;
    }
}
