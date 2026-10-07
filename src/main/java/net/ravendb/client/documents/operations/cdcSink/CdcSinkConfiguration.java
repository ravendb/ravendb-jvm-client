package net.ravendb.client.documents.operations.cdcSink;

import java.util.ArrayList;
import java.util.List;

public class CdcSinkConfiguration {

    private long taskId;
    private boolean disabled;
    private String name;
    private String mentorNode;
    private boolean pinToMentorNode;
    private String connectionStringName;
    private List<CdcSinkTableConfig> tables = new ArrayList<>();
    private CdcSinkPostgresSettings postgres;
    private boolean skipInitialLoad;

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMentorNode() {
        return mentorNode;
    }

    public void setMentorNode(String mentorNode) {
        this.mentorNode = mentorNode;
    }

    public boolean isPinToMentorNode() {
        return pinToMentorNode;
    }

    public void setPinToMentorNode(boolean pinToMentorNode) {
        this.pinToMentorNode = pinToMentorNode;
    }

    /**
     * @return the name of the SQL connection string pointing at the CDC source database
     */
    public String getConnectionStringName() {
        return connectionStringName;
    }

    public void setConnectionStringName(String connectionStringName) {
        this.connectionStringName = connectionStringName;
    }

    /**
     * @return the source tables mapped by this task
     */
    public List<CdcSinkTableConfig> getTables() {
        return tables;
    }

    public void setTables(List<CdcSinkTableConfig> tables) {
        this.tables = tables;
    }

    /**
     * @return the PostgreSQL-specific settings (publication name, slot name). Null for SQL Server
     *         configurations. Auto-filled on creation if omitted.
     */
    public CdcSinkPostgresSettings getPostgres() {
        return postgres;
    }

    public void setPostgres(CdcSinkPostgresSettings postgres) {
        this.postgres = postgres;
    }

    /**
     * @return true when the initial full-table load is skipped — tables are marked as loaded immediately and
     *         the task starts streaming CDC changes. Use this when the target RavenDB database is already
     *         populated (e.g. from a prior migration).
     */
    public boolean isSkipInitialLoad() {
        return skipInitialLoad;
    }

    public void setSkipInitialLoad(boolean skipInitialLoad) {
        this.skipInitialLoad = skipInitialLoad;
    }
}
