package net.ravendb.client.serverwide.operations.logs;

import java.time.Duration;

//TODO: make sure we have test with get/setter

public class GetLogsConfigurationResult {
    private LogMode currentMode;
    private LogMode mode;
    private String path;
    private boolean useUtcTime;
    private Duration retentionTime;
    private Long retentionSize;
    private boolean compress;

    /**
     * @return Current mode that is active
     */
    public LogMode getCurrentMode() {
        return currentMode;
    }

    /**
     * @param currentMode Current mode that is active
     */
    public void setCurrentMode(LogMode currentMode) {
        this.currentMode = currentMode;
    }

    /**
     * @return Mode that is written in the configuration file and which will be used after server restart
     */
    public LogMode getMode() {
        return mode;
    }

    /**
     * @param mode Mode that is written in the configuration file and which will be used after server restart
     */
    public void setMode(LogMode mode) {
        this.mode = mode;
    }

    /**
     * @return Path to which logs will be written
     */
    public String getPath() {
        return path;
    }

    /**
     * @param path Path to which logs will be written
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * @return Indicates if logs will be written in UTC or in server local time
     */
    public boolean isUseUtcTime() {
        return useUtcTime;
    }

    /**
     * @param useUtcTime Indicates if logs will be written in UTC or in server local time
     */
    public void setUseUtcTime(boolean useUtcTime) {
        this.useUtcTime = useUtcTime;
    }

    public Duration getRetentionTime() {
        return retentionTime;
    }

    public void setRetentionTime(Duration retentionTime) {
        this.retentionTime = retentionTime;
    }

    /**
     * @return Logs retention size
     */
    public Long getRetentionSize() {
        return retentionSize;
    }

    /**
     * @param retentionSize Retention size
     */
    public void setRetentionSize(Long retentionSize) {
        this.retentionSize = retentionSize;
    }

    /**
     * @return Are logs compressed
     */
    public boolean isCompress() {
        return compress;
    }

    /**
     * @param compress Are logs compressed
     */
    public void setCompress(boolean compress) {
        this.compress = compress;
    }
}
