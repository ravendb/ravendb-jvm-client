package net.ravendb.client.documents.operations.configuration;

public class StudioConfiguration {
    private boolean disabled;

    private StudioEnvironment environment;

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public StudioEnvironment getEnvironment() {
        return environment;
    }

    public void setEnvironment(StudioEnvironment environment) {
        this.environment = environment;
    }
}
