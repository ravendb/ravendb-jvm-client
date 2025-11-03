package net.ravendb.client.documents.operations.AI;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * Settings for Mistral AI service.
 */
public class MistralAiSettings extends AbstractAiSettings {

    /**
     * The model ID for the Mistral AI service.
     */
    private String model;

    /**
     * The endpoint for the Mistral AI service.
     */
    private String endpoint;

    /**
     * The API key required for accessing the Mistral AI service.
     */
    private String apiKey;

    public MistralAiSettings(String model, String apiKey, String endpoint) {
        this.model = model;
        this.apiKey = apiKey;
        this.endpoint = endpoint;
    }
    public MistralAiSettings() {}

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public void validate(List<String> errors) {
        if (StringUtils.isBlank(model)) {
            errors.add("Value of 'model' field cannot be empty.");
        }

        if (StringUtils.isBlank(endpoint)) {
            errors.add("Value of 'endpoint' field cannot be empty.");
        }

        if (StringUtils.isBlank(apiKey)) {
            errors.add("Value of 'apiKey' field cannot be empty.");
        }
    }

    @Override
    public AiSettingsCompareDifferences compare(AbstractAiSettings other) {
        if (!(other instanceof MistralAiSettings)) {
            return AiSettingsCompareDifferences.All;
        }

        MistralAiSettings otherSettings = (MistralAiSettings) other;
        int diff = AiSettingsCompareDifferences.None.getValue();

        if (!Objects.equals(this.model, otherSettings.model)) {
            diff |= AiSettingsCompareDifferences.ModelArchitecture.getValue();
        }

        if (!Objects.equals(this.endpoint, otherSettings.endpoint)) {
            diff |= AiSettingsCompareDifferences.EndpointConfiguration.getValue();
        }

        if (!Objects.equals(this.apiKey, otherSettings.apiKey)) {
            diff |= AiSettingsCompareDifferences.AuthenticationSettings.getValue();
        }

        return AiSettingsCompareDifferences.values()[diff];
    }
}
