package net.ravendb.client.documents.operations.cdcSink.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.ravendb.client.documents.operations.cdcSink.CdcColumnMapping;
import net.ravendb.client.documents.operations.cdcSink.CdcColumnType;

/**
 * One source-side column as the CDC schema-discovery endpoint sees it. Field names mirror
 * {@link CdcColumnMapping} so the response can be dropped straight into a mapping model.
 */
public class CdcSinkSourceColumn {

    private String name;
    private String nativeType;
    private CdcColumnType suggestedType;

    @JsonProperty("IsPrimaryKey")
    private boolean primaryKey;

    @JsonProperty("IsCdcCapturable")
    private boolean cdcCapturable;

    private String unsupportedReason;

    /**
     * @return the source column name. Matches {@link CdcColumnMapping#getColumn()}.
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the raw source-side type string (e.g. {@code "varchar"}, {@code "bigint"}, {@code "jsonb"}).
     *         For MySQL this is DATA_TYPE (without precision); the fuller COLUMN_TYPE form is not
     *         surfaced here today.
     */
    public String getNativeType() {
        return nativeType;
    }

    public void setNativeType(String nativeType) {
        this.nativeType = nativeType;
    }

    /**
     * @return what CDC will store this column as by default. {@link CdcColumnType#JSON} for PostgreSQL
     *         {@code jsonb}/{@code json} and MySQL {@code json}; {@link CdcColumnType#ATTACHMENT} for
     *         PostgreSQL {@code bytea} and SQL Server {@code varbinary}/{@code image};
     *         {@link CdcColumnType#DEFAULT} otherwise.
     */
    public CdcColumnType getSuggestedType() {
        return suggestedType;
    }

    public void setSuggestedType(CdcColumnType suggestedType) {
        this.suggestedType = suggestedType;
    }

    /**
     * @return true when this column participates in the source table's primary key
     */
    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(boolean primaryKey) {
        this.primaryKey = primaryKey;
    }

    /**
     * @return true when CDC can actually capture this column. False when the source-side type has no CDC
     *         mapping (PostgreSQL extension OIDs the streaming code would throw on) or when the column is
     *         not enrolled in the SQL Server CDC capture list.
     */
    public boolean isCdcCapturable() {
        return cdcCapturable;
    }

    public void setCdcCapturable(boolean cdcCapturable) {
        this.cdcCapturable = cdcCapturable;
    }

    /**
     * @return a human-readable reason set when {@link #isCdcCapturable()} is false; null on capturable columns
     */
    public String getUnsupportedReason() {
        return unsupportedReason;
    }

    public void setUnsupportedReason(String unsupportedReason) {
        this.unsupportedReason = unsupportedReason;
    }
}
