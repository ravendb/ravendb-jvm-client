package net.ravendb.client.documents.operations.etl.queue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang3.StringUtils;

/**
 * Microsoft Entra ID (client credentials) authentication for an Azure Service Bus namespace.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class AzureServiceBusEntraId {

    private String namespace;
    private String tenantId;
    private String clientId;
    private String clientSecret;

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

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    @JsonIgnore
    public boolean isValid() {
        return StringUtils.isNotBlank(namespace)
                && StringUtils.isNotBlank(tenantId)
                && StringUtils.isNotBlank(clientId)
                && StringUtils.isNotBlank(clientSecret);
    }
}
