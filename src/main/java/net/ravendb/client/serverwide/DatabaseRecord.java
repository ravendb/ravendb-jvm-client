package net.ravendb.client.serverwide;

import net.ravendb.client.primitives.UseSharpEnum;

import java.util.*;

public class DatabaseRecord {
    private String databaseName;
    private boolean disabled;
    private boolean encrypted;
    private long etagForBackup;
    private Map<String, DeletionInProgressStatus> deletionInProgress;
    private DatabaseStateStatus databaseStatus;
    private DatabaseTopology topology;
    private ConflictSolver conflictSolverConfig;
    private DocumentsCompressionConfiguration documentsCompression;
    private Map<String, String> settings = new HashMap<>();

    public DatabaseRecord() {
    }

    public DatabaseRecord(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public Map<String, String> getSettings() {
        return settings;
    }

    public void setSettings(Map<String, String> settings) {
        this.settings = settings;
    }

    public ConflictSolver getConflictSolverConfig() {
        return conflictSolverConfig;
    }

    public void setConflictSolverConfig(ConflictSolver conflictSolverConfig) {
        this.conflictSolverConfig = conflictSolverConfig;
    }

    public DocumentsCompressionConfiguration getDocumentsCompression() {
        return documentsCompression;
    }

    public void setDocumentsCompression(DocumentsCompressionConfiguration documentsCompression) {
        this.documentsCompression = documentsCompression;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    public long getEtagForBackup() {
        return etagForBackup;
    }

    public void setEtagForBackup(long etagForBackup) {
        this.etagForBackup = etagForBackup;
    }

    public Map<String, DeletionInProgressStatus> getDeletionInProgress() {
        return deletionInProgress;
    }

    public void setDeletionInProgress(Map<String, DeletionInProgressStatus> deletionInProgress) {
        this.deletionInProgress = deletionInProgress;
    }

    public DatabaseTopology getTopology() {
        return topology;
    }

    public void setTopology(DatabaseTopology topology) {
        this.topology = topology;
    }


    public DatabaseStateStatus getDatabaseStatus() {
        return databaseStatus;
    }

    public void setDatabaseStatus(DatabaseStateStatus databaseStatus) {
        this.databaseStatus = databaseStatus;
    }





    @UseSharpEnum
    public enum DatabaseStateStatus {
        NORMAL,
        RESTORE_IN_PROGRESS
    }
}
