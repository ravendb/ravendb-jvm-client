package net.ravendb.client.documents.operations.AI;

import org.apache.commons.lang3.StringUtils;
import java.util.List;
import java.util.Objects;

/**
 * Settings for Google AI service.
 */
public class GoogleSettings extends AbstractAiSettings {

    /**
     * Represents the version of the Google AI API.
     */
    public enum GoogleAIVersion {
        V1,
        V1_Beta
    }

    /**
     * The model that should be used.
     */
    private String model;

    /**
     * The API key to use to authenticate with the service.
     */
    private String apiKey;

    /**
     * The version of Google AI to use.
     */
    private GoogleAIVersion aiVersion;

    /**
     * The number of dimensions that the model should use.
     */
    private Integer dimensions;

    public GoogleSettings(String model, String apiKey, GoogleAIVersion aiVersion, Integer dimensions) {
        this.model = model;
        this.apiKey = apiKey;
        this.aiVersion = aiVersion;
        this.dimensions = dimensions;
    }
    public GoogleSettings() {
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public GoogleAIVersion getAiVersion() {
        return aiVersion;
    }

    public void setAiVersion(GoogleAIVersion aiVersion) {
        this.aiVersion = aiVersion;
    }

    public Integer getDimensions() {
        return dimensions;
    }

    public void setDimensions(Integer dimensions) {
        this.dimensions = dimensions;
    }

    @Override
    public void validate(List<String> errors) {
        if (StringUtils.isBlank(model)) {
            errors.add("Value of 'model' field cannot be empty.");
        }

        if (StringUtils.isBlank(apiKey)) {
            errors.add("Value of 'apiKey' field cannot be empty.");
        }

        if (dimensions != null && dimensions <= 0) {
            errors.add("Value of 'dimensions' field must be positive.");
        }
    }

    @Override
    public AiSettingsCompareDifferences compare(AbstractAiSettings other) {
        if (!(other instanceof GoogleSettings)) {
            return AiSettingsCompareDifferences.All;
        }

        GoogleSettings otherSettings = (GoogleSettings) other;
        int diff = AiSettingsCompareDifferences.None.getValue();

        if (!Objects.equals(this.model, otherSettings.model) ||
                !Objects.equals(this.aiVersion, otherSettings.aiVersion)) {
            diff |= AiSettingsCompareDifferences.ModelArchitecture.getValue();
        }

        if (!Objects.equals(this.apiKey, otherSettings.apiKey)) {
            diff |= AiSettingsCompareDifferences.AuthenticationSettings.getValue();
        }

        if (!Objects.equals(this.dimensions, otherSettings.dimensions)) {
            diff |= AiSettingsCompareDifferences.EmbeddingDimensions.getValue();
        }

        return AiSettingsCompareDifferences.values()[diff];
    }
}
