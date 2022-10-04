package net.ravendb.client.documents.operations.configuration;

public class StudioConfiguration {
    private boolean disabled;

    private boolean disableAutoIndexCreation;

    private StudioEnvironment environment;

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public boolean isDisableAutoIndexCreation() {
        return disableAutoIndexCreation;
    }

    public void setDisableAutoIndexCreation(boolean disableAutoIndexCreation) {
        this.disableAutoIndexCreation = disableAutoIndexCreation;
    }

    public StudioEnvironment getEnvironment() {
        return environment;
    }

    public void setEnvironment(StudioEnvironment environment) {
        this.environment = environment;
    }
}
