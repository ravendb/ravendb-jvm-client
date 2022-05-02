package net.ravendb.client.serverwide.operations.configuration;

import java.util.Map;

public class DatabaseSettings {
    private Map<String, String> settings;

    public Map<String, String> getSettings() {
        return settings;
    }

    public void setSettings(Map<String, String> settings) {
        this.settings = settings;
    }
}
