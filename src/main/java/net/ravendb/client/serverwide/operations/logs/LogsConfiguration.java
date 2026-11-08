package net.ravendb.client.serverwide.operations.logs;

import java.util.ArrayList;
import java.util.List;

public final class LogsConfiguration {

    private String path;
    private LogLevel currentMinLevel;
    private List<LogFilter> currentFilters = new ArrayList<>();
    private LogFilterAction currentLogFilterDefaultAction;
    private LogLevel minLevel;
    private long archiveAboveSizeInMb;
    private Integer maxArchiveDays;
    private Integer maxArchiveFiles;
    private boolean enableArchiveFileCompression;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public LogLevel getCurrentMinLevel() {
        return currentMinLevel;
    }

    public void setCurrentMinLevel(LogLevel currentMinLevel) {
        this.currentMinLevel = currentMinLevel;
    }

    public List<LogFilter> getCurrentFilters() {
        return currentFilters;
    }

    public void setCurrentFilters(List<LogFilter> currentFilters) {
        this.currentFilters = currentFilters;
    }

    public LogFilterAction getCurrentLogFilterDefaultAction() {
        return currentLogFilterDefaultAction;
    }

    public void setCurrentLogFilterDefaultAction(LogFilterAction currentLogFilterDefaultAction) {
        this.currentLogFilterDefaultAction = currentLogFilterDefaultAction;
    }

    public LogLevel getMinLevel() {
        return minLevel;
    }

    public void setMinLevel(LogLevel minLevel) {
        this.minLevel = minLevel;
    }

    public long getArchiveAboveSizeInMb() {
        return archiveAboveSizeInMb;
    }

    public void setArchiveAboveSizeInMb(long archiveAboveSizeInMb) {
        this.archiveAboveSizeInMb = archiveAboveSizeInMb;
    }

    public Integer getMaxArchiveDays() {
        return maxArchiveDays;
    }

    public void setMaxArchiveDays(Integer maxArchiveDays) {
        this.maxArchiveDays = maxArchiveDays;
    }

    public Integer getMaxArchiveFiles() {
        return maxArchiveFiles;
    }

    public void setMaxArchiveFiles(Integer maxArchiveFiles) {
        this.maxArchiveFiles = maxArchiveFiles;
    }

    public boolean isEnableArchiveFileCompression() {
        return enableArchiveFileCompression;
    }

    public void setEnableArchiveFileCompression(boolean enableArchiveFileCompression) {
        this.enableArchiveFileCompression = enableArchiveFileCompression;
    }
}
