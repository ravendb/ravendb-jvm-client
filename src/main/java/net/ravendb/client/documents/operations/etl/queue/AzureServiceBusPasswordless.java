package net.ravendb.client.documents.operations.etl.queue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof AzureServiceBusPasswordless)) {
            return false;
        }

        return Objects.equals(namespace, ((AzureServiceBusPasswordless) o).namespace);
    }

    @Override
    public int hashCode() {
        return namespace != null ? namespace.hashCode() : 0;
    }
}
