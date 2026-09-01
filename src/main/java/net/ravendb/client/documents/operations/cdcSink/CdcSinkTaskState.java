package net.ravendb.client.documents.operations.cdcSink;

import java.util.Map;
import java.util.TreeMap;

/**
 * State document for a CDC Sink task, stored in the {@code @cdc-states} collection.
 * Tracks the last processed LSN and per-table initial load progress.
 */
public class CdcSinkTaskState {

    /**
     * Collection name for state documents.
     */
    public static final String COLLECTION_NAME = "@cdc-states";

    private String lastLsn;
    private Map<String, CdcSinkTableLoadState> tables = new TreeMap<>(String::compareToIgnoreCase);
    private String configurationName;

    /**
     * @return the last successfully processed Log Sequence Number (LSN) from the CDC stream,
     *         used to resume streaming after a restart
     */
    public String getLastLsn() {
        return lastLsn;
    }

    public void setLastLsn(String lastLsn) {
        this.lastLsn = lastLsn;
    }

    /**
     * @return the per-table initial load state, keyed by "schema.tableName" (case-insensitive)
     */
    public Map<String, CdcSinkTableLoadState> getTables() {
        return tables;
    }

    public void setTables(Map<String, CdcSinkTableLoadState> tables) {
        Map<String, CdcSinkTableLoadState> caseInsensitive = new TreeMap<>(String::compareToIgnoreCase);
        if (tables != null) {
            caseInsensitive.putAll(tables);
        }

        this.tables = caseInsensitive;
    }

    /**
     * @return the name of the CDC Sink configuration this state belongs to
     */
    public String getConfigurationName() {
        return configurationName;
    }

    public void setConfigurationName(String configurationName) {
        this.configurationName = configurationName;
    }

    /**
     * Generates the document ID for the state document. Configuration names are compared
     * case-insensitively, but the document ID preserves the original casing.
     * @param configurationName the CDC Sink configuration name
     * @return the state document ID
     */
    public static String getDocumentId(String configurationName) {
        return COLLECTION_NAME + "/" + configurationName;
    }
}
