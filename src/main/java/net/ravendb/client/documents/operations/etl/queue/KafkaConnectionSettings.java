package net.ravendb.client.documents.operations.etl.queue;

import java.util.Map;

public class KafkaConnectionSettings {

    private String bootstrapServers;

    private Map<String, String> connectionOptions;

    private boolean useRavenCertificate;

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public Map<String, String> getConnectionOptions() {
        return connectionOptions;
    }

    public void setConnectionOptions(Map<String, String> connectionOptions) {
        this.connectionOptions = connectionOptions;
    }

    public boolean isUseRavenCertificate() {
        return useRavenCertificate;
    }

    public void setUseRavenCertificate(boolean useRavenCertificate) {
        this.useRavenCertificate = useRavenCertificate;
    }
}
