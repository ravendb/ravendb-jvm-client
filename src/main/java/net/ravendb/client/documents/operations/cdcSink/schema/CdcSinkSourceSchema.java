package net.ravendb.client.documents.operations.cdcSink.schema;

import java.util.ArrayList;
import java.util.List;

/**
 * Top-level response of {@code POST /admin/cdc-sink/schema}. Carries the source-side schema annotated
 * with CDC-specific capturability hints so a CDC mapping can be driven without a second round-trip.
 */
public class CdcSinkSourceSchema {

    private String catalogName;
    private List<CdcSinkSourceTable> tables = new ArrayList<>();
    private List<String> errors = new ArrayList<>();
    private boolean hasPermissionToSetup;
    private List<String> warnings = new ArrayList<>();

    /**
     * @return the source database / catalog name
     */
    public String getCatalogName() {
        return catalogName;
    }

    public void setCatalogName(String catalogName) {
        this.catalogName = catalogName;
    }

    /**
     * @return the discovered source tables, each annotated with CDC capturability hints
     */
    public List<CdcSinkSourceTable> getTables() {
        return tables;
    }

    public void setTables(List<CdcSinkSourceTable> tables) {
        this.tables = tables;
    }

    /**
     * Gets the whole-request failures (validation, missing connection string, source DB unreachable) and
     * connection-level verification blockers (e.g. PostgreSQL {@code wal_level} not {@code logical}, the
     * connecting user lacking the privilege to provision CDC with no infrastructure in place).
     *
     * <p>
     * Per-table issues live on {@link CdcSinkSourceTable#getUnsupportedReason()} /
     * {@link CdcSinkSourceTable#getWarnings()} / {@link CdcSinkSourceColumn#getUnsupportedReason()} instead.
     * </p>
     * @return the whole-request errors
     */
    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    /**
     * @return whether the connecting user has sufficient privileges to provision the CDC infrastructure
     *         (PostgreSQL replication slot/publication, SQL Server {@code sp_cdc_enable_*}). When false an
     *         administrator must set CDC up out-of-band. Distinct from per-table
     *         {@link CdcSinkSourceTable#isCdcEnabled()}, which reports whether CDC is already active.
     */
    public boolean isHasPermissionToSetup() {
        return hasPermissionToSetup;
    }

    public void setHasPermissionToSetup(boolean hasPermissionToSetup) {
        this.hasPermissionToSetup = hasPermissionToSetup;
    }

    /**
     * @return non-fatal connection-level verification findings (e.g. SQL Server Agent not running, CDC
     *         infrastructure already present under a reduced-privilege account). Per-table warnings live
     *         on {@link CdcSinkSourceTable#getWarnings()}.
     */
    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    /**
     * @return true when nothing blocks setting up CDC against this source — i.e. there are no
     *         {@link #getErrors()}. Warnings do not affect success.
     */
    public boolean isSuccess() {
        return errors == null || errors.isEmpty();
    }
}
