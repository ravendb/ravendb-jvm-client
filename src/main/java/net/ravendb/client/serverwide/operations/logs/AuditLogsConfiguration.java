package net.ravendb.client.serverwide.operations.logs;

public final class AuditLogsConfiguration {

    private String path;
    private LogLevel level;
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

    public LogLevel getLevel() {
        return level;
    }

    public void setLevel(LogLevel level) {
        this.level = level;
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
