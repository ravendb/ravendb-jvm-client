package net.ravendb.client.serverwide.operations.logs;

import java.util.ArrayList;
import java.util.List;

public final class AdminLogsConfiguration {

    private LogLevel currentMinLevel;
    private List<LogFilter> currentFilters = new ArrayList<>();
    private LogFilterAction currentLogFilterDefaultAction;

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
}
