package net.ravendb.client.documents.operations.AI.ConnectionStrings.Settings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import net.ravendb.client.documents.operations.AI.ConnectionStrings.AiSettingsCompareDifferences;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * Base settings for OpenAI-compatible providers.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class OpenAiBaseSettings extends AbstractAiSettings implements IAiSettings {

    /**
     * The API key to use to authenticate with the service.
     */
    private String apiKey;

    /**
     * The service endpoint that the client will send requests to.
     */
    private String endpoint;

    /**
     * The model that should be used.
     */
    private String model;

    /**
     * The number of dimensions that the model should use.
     */
    private Integer dimensions;

    /**
     * Controls randomness of the model output. Range typically [0.0, 2.0].
     * Higher values (e.g., 1.0+) make output more creative and diverse;
     * lower values (e.g., 0.2) make it more deterministic.
     * When null, the parameter is not sent.
     */
    private Double temperature;

    protected OpenAiBaseSettings(String apiKey, String endpoint, String model,
                                 Integer dimensions, Double temperature) {
        this.apiKey = apiKey;
        this.endpoint = endpoint;
        this.model = model;
        this.dimensions = dimensions;
        this.temperature = temperature;
    }

    public OpenAiBaseSettings() {}

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getDimensions() {
        return dimensions;
    }

    public void setDimensions(Integer dimensions) {
        this.dimensions = dimensions;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    /**
     * Returns the base endpoint URI, ensuring it ends with a slash.
     */
    public String getBaseEndpointUri() {
        String uri = this.endpoint;
        if (uri != null && !uri.endsWith("/")) {
            uri += "/";
        }
        return uri;
    }

    @Override
    public void validate(List<String> errors) {
        if (StringUtils.isBlank(apiKey)) {
            errors.add("Value of 'apiKey' field cannot be empty.");
        }

        if (StringUtils.isBlank(endpoint)) {
            errors.add("Value of 'endpoint' field cannot be empty.");
        }

        if (StringUtils.isBlank(model)) {
            errors.add("Value of 'model' field cannot be empty.");
        }

        if (dimensions != null && dimensions <= 0) {
            errors.add("Value of 'dimensions' field must be positive.");
        }

        if (temperature != null && temperature < 0) {
            errors.add("Value of 'temperature' field must be non-negative.");
        }
    }

    @Override
    public AiSettingsCompareDifferences compare(AbstractAiSettings other) {
        if (!(other instanceof OpenAiBaseSettings)) {
            return AiSettingsCompareDifferences.All;
        }

        OpenAiBaseSettings otherSettings = (OpenAiBaseSettings) other;
        int diff = AiSettingsCompareDifferences.None.getValue();

        if (!Objects.equals(this.apiKey, otherSettings.apiKey)) {
            diff |= AiSettingsCompareDifferences.AuthenticationSettings.getValue();
        }

        if (!Objects.equals(this.endpoint, otherSettings.endpoint)) {
            diff |= AiSettingsCompareDifferences.EndpointConfiguration.getValue();
        }

        if (!Objects.equals(this.model, otherSettings.model)) {
            diff |= AiSettingsCompareDifferences.ModelArchitecture.getValue();
        }

        if (!Objects.equals(this.dimensions, otherSettings.dimensions)) {
            diff |= AiSettingsCompareDifferences.EmbeddingDimensions.getValue();
        }

        boolean hasTemp = this.temperature != null;
        boolean otherHasTemp = otherSettings.temperature != null;

        if (hasTemp != otherHasTemp ||
                (hasTemp && otherHasTemp &&
                        Math.abs(this.temperature - otherSettings.temperature) > 0.0001)) {
            diff |= AiSettingsCompareDifferences.EndpointConfiguration.getValue();
        }

        return AiSettingsCompareDifferences.fromValue(diff);
    }
}