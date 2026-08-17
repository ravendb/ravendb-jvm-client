package net.ravendb.client.documents.operations.etl.queue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class AzureServiceBusConnectionSettings {

    private String connectionString;
    private AzureServiceBusEntraId entraId;
    private AzureServiceBusPasswordless passwordless;

    public String getConnectionString() {
        return connectionString;
    }

    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }

    public AzureServiceBusEntraId getEntraId() {
        return entraId;
    }

    public void setEntraId(AzureServiceBusEntraId entraId) {
        this.entraId = entraId;
    }

    public AzureServiceBusPasswordless getPasswordless() {
        return passwordless;
    }

    public void setPasswordless(AzureServiceBusPasswordless passwordless) {
        this.passwordless = passwordless;
    }

    /**
     * Verifies that exactly one authentication method is configured and that its required fields are populated.
     * Validation of the connection string itself is intentionally shallow (substring match for {@code sb://}) and is
     * deferred to the Azure Service Bus SDK, which produces the authoritative error at connect time.
     * @return true when exactly one authentication method is configured and usable
     */
    @JsonIgnore
    public boolean isValidConnection() {
        if (!isOnlyOneConnectionProvided()) {
            return false;
        }

        if (entraId != null && entraId.isValid()) {
            return true;
        }

        if (passwordless != null && passwordless.isValid()) {
            return true;
        }

        return extractEndpoint() != null;
    }

    private boolean isOnlyOneConnectionProvided() {
        int count = 0;

        if (entraId != null) {
            count++;
        }

        if (StringUtils.isNotBlank(connectionString)) {
            count++;
        }

        if (passwordless != null) {
            count++;
        }

        return count == 1;
    }

    @JsonIgnore
    public String getServiceBusUrl() {
        if (StringUtils.isNotBlank(connectionString)) {
            String endpoint = extractEndpoint();
            if (endpoint != null) {
                return endpoint;
            }

            throw new IllegalStateException("No endpoint provided");
        }

        return "sb://" + getNamespace() + "/";
    }

    /**
     * Returns the {@code sb://...} endpoint embedded in the connection string, or {@code null} when it is absent.
     */
    private String extractEndpoint() {
        if (StringUtils.isEmpty(connectionString)) {
            return null;
        }

        int start = StringUtils.indexOfIgnoreCase(connectionString, "sb://");
        if (start < 0) {
            return null;
        }

        int end = connectionString.indexOf(';', start);
        if (end < 0) {
            return connectionString.substring(start);
        }

        return connectionString.substring(start, end);
    }

    private String getNamespace() {
        if (entraId != null) {
            return entraId.getNamespace();
        }

        if (passwordless != null) {
            return passwordless.getNamespace();
        }

        throw new IllegalStateException("No namespace provided");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof AzureServiceBusConnectionSettings)) {
            return false;
        }

        AzureServiceBusConnectionSettings other = (AzureServiceBusConnectionSettings) o;
        return Objects.equals(entraId, other.entraId)
                && Objects.equals(connectionString, other.connectionString)
                && Objects.equals(passwordless, other.passwordless);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entraId, connectionString, passwordless);
    }
}
