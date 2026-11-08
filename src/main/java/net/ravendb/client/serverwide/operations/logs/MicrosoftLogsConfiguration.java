package net.ravendb.client.serverwide.operations.logs;

public final class MicrosoftLogsConfiguration {

    private LogLevel currentMinLevel;
    private LogLevel minLevel;

    public LogLevel getCurrentMinLevel() {
        return currentMinLevel;
    }

    public void setCurrentMinLevel(LogLevel currentMinLevel) {
        this.currentMinLevel = currentMinLevel;
    }

    public LogLevel getMinLevel() {
        return minLevel;
    }

    public void setMinLevel(LogLevel minLevel) {
        this.minLevel = minLevel;
    }
}
