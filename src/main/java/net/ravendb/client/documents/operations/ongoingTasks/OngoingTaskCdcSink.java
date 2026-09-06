package net.ravendb.client.documents.operations.ongoingTasks;

import net.ravendb.client.documents.operations.cdcSink.CdcSinkConfiguration;

import java.util.Date;

public class OngoingTaskCdcSink extends OngoingTask {

    private CdcSinkConfiguration configuration;
    private String connectionStringName;
    private String factoryName;
    private Date lastBatchTime;
    private String lastCheckpoint;
    private Double secondsSinceLastBatch;
    private Date lastActivityTime;
    private Double secondsSinceLastActivity;
    private String healthIssue;

    public OngoingTaskCdcSink() {
        setTaskType(OngoingTaskType.CDC_SINK);
    }

    public CdcSinkConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(CdcSinkConfiguration configuration) {
        this.configuration = configuration;
    }

    public String getConnectionStringName() {
        return connectionStringName;
    }

    public void setConnectionStringName(String connectionStringName) {
        this.connectionStringName = connectionStringName;
    }

    public String getFactoryName() {
        return factoryName;
    }

    public void setFactoryName(String factoryName) {
        this.factoryName = factoryName;
    }

    /**
     * @return the UTC time of the last successfully completed batch, or null if no batch has completed yet
     */
    public Date getLastBatchTime() {
        return lastBatchTime;
    }

    public void setLastBatchTime(Date lastBatchTime) {
        this.lastBatchTime = lastBatchTime;
    }

    /**
     * @return the last successfully persisted checkpoint (LSN/GTID)
     */
    public String getLastCheckpoint() {
        return lastCheckpoint;
    }

    public void setLastCheckpoint(String lastCheckpoint) {
        this.lastCheckpoint = lastCheckpoint;
    }

    /**
     * @return the seconds since the last successful batch, or null if no batch has completed yet.
     *         Provides a simple lag indicator for the dashboard.
     */
    public Double getSecondsSinceLastBatch() {
        return secondsSinceLastBatch;
    }

    public void setSecondsSinceLastBatch(Double secondsSinceLastBatch) {
        this.secondsSinceLastBatch = secondsSinceLastBatch;
    }

    /**
     * Gets the UTC time of the last activity from the source — poll iteration (SQL Server),
     * replication message (PostgreSQL), or binlog event (MySQL). Null before the first activity.
     *
     * <p>
     * When this is recent but {@link #getLastBatchTime()} is old, it means the source connection is alive but
     * there are no changes. When both are stale, the connection may be dead.
     * </p>
     * @return the UTC time of the last source activity, or null
     */
    public Date getLastActivityTime() {
        return lastActivityTime;
    }

    public void setLastActivityTime(Date lastActivityTime) {
        this.lastActivityTime = lastActivityTime;
    }

    /**
     * @return the seconds since the last source activity. When this exceeds the expected heartbeat/poll
     *         interval significantly, the connection may be dead.
     */
    public Double getSecondsSinceLastActivity() {
        return secondsSinceLastActivity;
    }

    public void setSecondsSinceLastActivity(Double secondsSinceLastActivity) {
        this.secondsSinceLastActivity = secondsSinceLastActivity;
    }

    /**
     * @return null when healthy; a diagnostic message when the process detects a problem
     *         (fallback mode, stale connection, etc.)
     */
    public String getHealthIssue() {
        return healthIssue;
    }

    public void setHealthIssue(String healthIssue) {
        this.healthIssue = healthIssue;
    }
}
