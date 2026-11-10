package net.ravendb.client.serverwide.operations.logs;

public final class LogFilter {

    private LogLevel minLevel;
    private LogLevel maxLevel;
    private String condition;
    private LogFilterAction action;

    private LogFilter() {
        // for deserialization
    }

    public LogFilter(LogLevel minLevel, LogLevel maxLevel, String condition, LogFilterAction action) {
        if (condition == null) {
            throw new IllegalArgumentException("condition cannot be null");
        }
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.condition = condition;
        this.action = action;
    }

    public LogLevel getMinLevel() {
        return minLevel;
    }

    void setMinLevel(LogLevel minLevel) {
        this.minLevel = minLevel;
    }

    public LogLevel getMaxLevel() {
        return maxLevel;
    }

    void setMaxLevel(LogLevel maxLevel) {
        this.maxLevel = maxLevel;
    }

    public String getCondition() {
        return condition;
    }

    void setCondition(String condition) {
        this.condition = condition;
    }

    public LogFilterAction getAction() {
        return action;
    }

    void setAction(LogFilterAction action) {
        this.action = action;
    }
}
