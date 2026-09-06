package net.ravendb.client.documents.operations.cdcSink;

import java.util.List;

/**
 * Per-table load state within a CDC Sink task state document.
 */
public class CdcSinkTableLoadState {

    private boolean initialLoadCompleted;
    private List<String> lastKeyValues;
    private List<String> keyColumns;

    /**
     * @return whether the initial full-table load has completed for this table
     */
    public boolean isInitialLoadCompleted() {
        return initialLoadCompleted;
    }

    public void setInitialLoadCompleted(boolean initialLoadCompleted) {
        this.initialLoadCompleted = initialLoadCompleted;
    }

    /**
     * @return the last primary key values loaded during the initial load, used to resume an interrupted
     *         initial load. Holds the string representations of the PK column values, in PK column order.
     */
    public List<String> getLastKeyValues() {
        return lastKeyValues;
    }

    public void setLastKeyValues(List<String> lastKeyValues) {
        this.lastKeyValues = lastKeyValues;
    }

    public List<String> getKeyColumns() {
        return keyColumns;
    }

    public void setKeyColumns(List<String> keyColumns) {
        this.keyColumns = keyColumns;
    }
}
