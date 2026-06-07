package net.ravendb.client.documents.operations.AI;

import org.apache.commons.lang3.StringUtils;

import java.util.EnumSet;
import java.util.Objects;

/**
 * Settings for Google AI service.
 */
public final class GoogleSettings extends OpenAiBaseSettings {

    private static final String GOOGLE_BASE_URI = "https://generativelanguage.googleapis.com/";

    /**
     * The version of Google AI to use.
     */
    private GoogleAIVersion aiVersion;

    public GoogleSettings(String model, String apiKey, String endpoint, GoogleAIVersion aiVersion, Integer dimensions, Double temperature) {
        super(apiKey, endpoint, model, dimensions, temperature);
        this.aiVersion = aiVersion;
    }

    public GoogleSettings(String model, String apiKey, GoogleAIVersion aiVersion, Integer dimensions) {
        this(model, apiKey, null, aiVersion, dimensions, null);
    }

    public GoogleSettings(String model, String apiKey) {
        this(model, apiKey, null, null, null, null);
    }

    public GoogleSettings(String model, String apiKey, GoogleAIVersion aiVersion) {
        this(model, apiKey, null, aiVersion, null, null);
    }

    public GoogleSettings(String model, String apiKey, Integer dimensions) {
        this(model, apiKey, null, null, dimensions, null);
    }

    public GoogleSettings() {
    }

    public GoogleAIVersion getAiVersion() {
        return aiVersion;
    }

    public void setAiVersion(GoogleAIVersion aiVersion) {
        this.aiVersion = aiVersion;
    }

    @Override
    public String getBaseEndpointUri() {
        if (StringUtils.isNotEmpty(getEndpoint())) {
            return super.getBaseEndpointUri();
        }

        return GOOGLE_BASE_URI;
    }

    @Override
    public EnumSet<AiSettingsCompareDifferences> compare(AbstractAiSettings other) {
        if (!(other instanceof GoogleSettings)) {
            return EnumSet.of(AiSettingsCompareDifferences.All);
        }

        GoogleSettings otherSettings = (GoogleSettings) other;
        EnumSet<AiSettingsCompareDifferences> diff = super.compare(other);

        if (!Objects.equals(this.aiVersion, otherSettings.aiVersion)) {
            diff.remove(AiSettingsCompareDifferences.None);
            diff.add(AiSettingsCompareDifferences.ModelArchitecture);
        }

        return diff;
    }
}
