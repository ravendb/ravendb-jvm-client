package net.ravendb.client.documents.operations.AI;

import org.apache.commons.lang3.StringUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * Settings for HuggingFace service.
 */
public final class HuggingFaceSettings extends AbstractAiSettings {

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

    public HuggingFaceSettings(String apiKey, String model) {
        this(apiKey, model, null);
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
    public void validateFields(List<String> errors) {
        if (StringUtils.isBlank(model)) {
            errors.add("Value of 'model' field cannot be empty.");
        }

        if (StringUtils.isBlank(apiKey)) {
            errors.add("Value of 'apiKey' field cannot be empty.");
        }
    }

    @Override
    public EnumSet<AiSettingsCompareDifferences> compare(AbstractAiSettings other) {
        if (!(other instanceof HuggingFaceSettings)) {
            return EnumSet.of(AiSettingsCompareDifferences.All);
        }

        HuggingFaceSettings otherSettings = (HuggingFaceSettings) other;
        EnumSet<AiSettingsCompareDifferences> diff = EnumSet.of(AiSettingsCompareDifferences.None);

        if (!Objects.equals(this.model, otherSettings.model)) {
            diff.remove(AiSettingsCompareDifferences.None);
            diff.add(AiSettingsCompareDifferences.ModelArchitecture);
        }

        if (!Objects.equals(this.endpoint, otherSettings.endpoint)) {
            diff.remove(AiSettingsCompareDifferences.None);
            diff.add(AiSettingsCompareDifferences.EndpointConfiguration);
        }

        if (!Objects.equals(this.apiKey, otherSettings.apiKey)) {
            diff.remove(AiSettingsCompareDifferences.None);
            diff.add(AiSettingsCompareDifferences.AuthenticationSettings);
        }

        return diff;
    }
}
