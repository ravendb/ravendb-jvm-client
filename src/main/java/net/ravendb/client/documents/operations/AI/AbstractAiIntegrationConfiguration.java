package net.ravendb.client.documents.operations.AI;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.ravendb.client.documents.operations.etl.EtlConfiguration;

public abstract class AbstractAiIntegrationConfiguration extends EtlConfiguration<AiConnectionString> {
    @JsonIgnore
    public AiConnectorType getAiConnectorType() {
        AiConnectionString connection = getConnection();
        return (connection != null && connection.getActiveProvider() != null)
                ? connection.getActiveProvider()
                : AiConnectorType.None;
    }
}

