package net.ravendb.client.documents.operations.backups;

public class GetBackupConfigurationScript {
    private String exec;
    private String arguments;
    private int timeoutInMs;

    public GetBackupConfigurationScript() {
        timeoutInMs = 10_000;
    }

    public String getExec() {
        return exec;
    }

    public void setExec(String exec) {
        this.exec = exec;
    }

    public String getArguments() {
        return arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
    }

    public int getTimeoutInMs() {
        return timeoutInMs;
    }

    public void setTimeoutInMs(int timeoutInMs) {
        this.timeoutInMs = timeoutInMs;
    }
}
