package net.ravendb.client.serverwide;

import java.util.HashMap;
import java.util.Map;

public class DatabaseRecord {
    private String databaseName;
    private boolean disabled;
    private String dataDirectory;
    private Map<String, String> settings = new HashMap<>();
    private ConflictSolver conflictSolverConfig;

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

    public String getDataDirectory() {
        return dataDirectory;
    }

    public void setDataDirectory(String dataDirectory) {
        this.dataDirectory = dataDirectory;
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
}
