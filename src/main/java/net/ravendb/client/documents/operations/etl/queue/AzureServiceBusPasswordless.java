package net.ravendb.client.documents.operations.etl.queue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang3.StringUtils;

/**
 * Machine authentication (Managed Identity) for an Azure Service Bus namespace.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class AzureServiceBusPasswordless {

    private String namespace;

    /**
     * @return the fully qualified Service Bus namespace, e.g. {@code mynamespace.servicebus.windows.net}
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Sets the fully qualified Service Bus namespace, e.g. {@code mynamespace.servicebus.windows.net}.
     * @param namespace the fully qualified Service Bus namespace
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @JsonIgnore
    public boolean isValid() {
        return StringUtils.isNotBlank(namespace);
    }
}
