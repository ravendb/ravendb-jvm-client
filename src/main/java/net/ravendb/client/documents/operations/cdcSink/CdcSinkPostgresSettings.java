package net.ravendb.client.documents.operations.cdcSink;

/**
 * PostgreSQL-specific settings for a CDC Sink task.
 * These are optional on creation — if omitted, auto-generated names are used.
 * Once set (either by the user or auto-filled), these values are immutable.
 */
public class CdcSinkPostgresSettings {

    private String publicationName;
    private String slotName;

    /**
     * @return the PostgreSQL publication name used for logical replication. If null on creation, auto-filled
     *         with an auto-generated name ({@code rvn_cdc_p_{guid}}). Must be a valid PostgreSQL identifier
     *         (alphanumeric + underscore, max 63 chars).
     */
    public String getPublicationName() {
        return publicationName;
    }

    public void setPublicationName(String publicationName) {
        this.publicationName = publicationName;
    }

    /**
     * @return the PostgreSQL logical replication slot name. If null on creation, auto-filled with an
     *         auto-generated name ({@code rvn_cdc_s_{guid}}). Must be a valid PostgreSQL identifier
     *         (alphanumeric + underscore, max 63 chars).
     */
    public String getSlotName() {
        return slotName;
    }

    public void setSlotName(String slotName) {
        this.slotName = slotName;
    }
}
