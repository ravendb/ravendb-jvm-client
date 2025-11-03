package net.ravendb.client.documents.operations.AI;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * Settings for HuggingFace service.
 */
public class HuggingFaceSettings extends AbstractAiSettings {

    /**
     * The name of the Hugging Face model.
     */
    private String model;

    /**
     * The endpoint for the text embedding generation service. If not specified, the default endpoint will be used.
     */
    private String endpoint;

    /**
     * The API key required for accessing the Hugging Face service.
     */
    private String apiKey;

    public HuggingFaceSettings(String apiKey, String model, String endpoint) {
        this.apiKey = apiKey;
        this.model = model;
        this.endpoint = endpoint;
    }
    public HuggingFaceSettings() {
    }

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

        if (StringUtils.isBlank(apiKey)) {
            errors.add("Value of 'apiKey' field cannot be empty.");
        }
    }

    @Override
    public AiSettingsCompareDifferences compare(AbstractAiSettings other) {
        if (!(other instanceof HuggingFaceSettings)) {
            return AiSettingsCompareDifferences.All;
        }

        HuggingFaceSettings otherSettings = (HuggingFaceSettings) other;
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
