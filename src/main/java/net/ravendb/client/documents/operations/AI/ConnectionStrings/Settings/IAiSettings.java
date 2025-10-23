package net.ravendb.client.documents.operations.AI.ConnectionStrings.Settings;

/**
 * Interface for AI settings with common properties across providers.
 */
public interface IAiSettings {

    /**
     * Gets the API key used to authenticate with the service.
     *
     * @return the API key, or null if not set.
     */
    String getApiKey();

    /**
     * Gets the model that should be used.
     *
     * @return the model name.
     */
    String getModel();

    /**
     * Gets the service endpoint that the client will send requests to.
     *
     * @return the endpoint URI, or null if not set.
     */
    String getEndpoint();

    /**
     * Returns the base endpoint URI, ensuring it ends with a slash.
     *
     * @return the normalized base endpoint URI.
     */
    String getBaseEndpointUri();
}
