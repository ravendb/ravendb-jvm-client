package net.ravendb.client.serverwide.operations.logs;

import java.time.Duration;

public class GetLogsConfigurationResult {
    private LogsConfiguration logs;
    private AuditLogsConfiguration auditLogs;
    private MicrosoftLogsConfiguration microsoftLogs;
    private AdminLogsConfiguration adminLogs;

    private LogMode currentMode;
    private LogMode mode;
    private String path;
    private boolean useUtcTime;
    private Duration retentionTime;
    private Long retentionSize;
    private boolean compress;

    public LogsConfiguration getLogs() {
        return logs;
    }

    public void setLogs(LogsConfiguration logs) {
        this.logs = logs;
    }

    public AuditLogsConfiguration getAuditLogs() {
        return auditLogs;
    }

    public void setAuditLogs(AuditLogsConfiguration auditLogs) {
        this.auditLogs = auditLogs;
    }

    public MicrosoftLogsConfiguration getMicrosoftLogs() {
        return microsoftLogs;
    }

    public void setMicrosoftLogs(MicrosoftLogsConfiguration microsoftLogs) {
        this.microsoftLogs = microsoftLogs;
    }

    public AdminLogsConfiguration getAdminLogs() {
        return adminLogs;
    }

    public void setAdminLogs(AdminLogsConfiguration adminLogs) {
        this.adminLogs = adminLogs;
    }

    /**
     * @return Current mode that is active
     * @deprecated Not supported by RavenDB 7.x servers; use {@link #getLogs()} and {@link LogsConfiguration#getCurrentMinLevel()}
     */
    @Deprecated
    public LogMode getCurrentMode() {
        return currentMode;
    }

    /**
     * @param currentMode Current mode that is active
     * @deprecated Not supported by RavenDB 7.x servers; use {@link #setLogs(LogsConfiguration)}
     */
    @Deprecated
    public void setCurrentMode(LogMode currentMode) {
        this.currentMode = currentMode;
    }

    /**
     * @return Mode that is written in the configuration file and which will be used after server restart
     * @deprecated Not supported by RavenDB 7.x servers; use {@link #getLogs()} and {@link LogsConfiguration#getMinLevel()}
     */
    @Deprecated
    public LogMode getMode() {
        return mode;
    }

    /**
     * @param mode Mode that is written in the configuration file and which will be used after server restart
     * @deprecated Not supported by RavenDB 7.x servers; use {@link #setLogs(LogsConfiguration)}
     */
    @Deprecated
    public void setMode(LogMode mode) {
        this.mode = mode;
    }

    /**
     * @return Path to which logs will be written
     * @deprecated Not supported by RavenDB 7.x servers; use {@link #getLogs()} and {@link LogsConfiguration#getPath()}
     */
    @Deprecated
    public String getPath() {
        return path;
    }

    /**
     * @param path Path to which logs will be written
     * @deprecated Not supported by RavenDB 7.x servers; use {@link #setLogs(LogsConfiguration)}
     */
    @Deprecated
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * @return Indicates if logs will be written in UTC or in server local time
     * @deprecated Not supported by RavenDB 7.x servers; there is no replacement
     */
    @Deprecated
    public boolean isUseUtcTime() {
        return useUtcTime;
    }

    /**
     * @param useUtcTime Indicates if logs will be written in UTC or in server local time
     * @deprecated Not supported by RavenDB 7.x servers; there is no replacement
     */
    @Deprecated
    public void setUseUtcTime(boolean useUtcTime) {
        this.useUtcTime = useUtcTime;
    }

    /**
     * @return Logs retention time
     * @deprecated Not supported by RavenDB 7.x servers; use {@link #getLogs()} and {@link LogsConfiguration#getMaxArchiveDays()}
     */
    @Deprecated
    public Duration getRetentionTime() {
        return retentionTime;
    }

    /**
     * @param retentionTime Logs retention time
     * @deprecated Not supported by RavenDB 7.x servers; use {@link #setLogs(LogsConfiguration)}
     */
    @Deprecated
    public void setRetentionTime(Duration retentionTime) {
        this.retentionTime = retentionTime;
    }

    /**
     * @return Logs retention size
     * @deprecated Not supported by RavenDB 7.x servers; use {@link #getLogs()} and {@link LogsConfiguration#getMaxArchiveFiles()}
     */
    @Deprecated
    public Long getRetentionSize() {
        return retentionSize;
    }

    /**
     * @param retentionSize Retention size
     * @deprecated Not supported by RavenDB 7.x servers; use {@link #setLogs(LogsConfiguration)}
     */
    @Deprecated
    public void setRetentionSize(Long retentionSize) {
        this.retentionSize = retentionSize;
    }

    /**
     * @return Are logs compressed
     * @deprecated Not supported by RavenDB 7.x servers; use {@link #getLogs()} and {@link LogsConfiguration#isEnableArchiveFileCompression()}
     */
    @Deprecated
    public boolean isCompress() {
        return compress;
    }

    /**
     * @param compress Are logs compressed
     * @deprecated Not supported by RavenDB 7.x servers; use {@link #setLogs(LogsConfiguration)}
     */
    @Deprecated
    public void setCompress(boolean compress) {
        this.compress = compress;
    }
}
