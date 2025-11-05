package net.ravendb.client.documents.operations.AI;

import org.apache.commons.lang3.StringUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * The configuration for the Ollama API client.
 */
public final class OllamaSettings extends AbstractAiSettings {

    /**
     * The URI of the Ollama API.
     */
    private String uri;

    /**
     * The model that should be used.
     */
    private String model;

    /**
     * Controls whether thinking models engage their reasoning process before responding.
     * When true, thinking models will perform their internal reasoning process (uses more tokens, slower, better quality for complex tasks).
     * When false, thinking models skip the reasoning process and respond directly (fewer tokens, faster, may reduce quality for complex reasoning).
     * When null, the parameter is not sent (backwards compatible).
     */
    private Boolean think;

    /**
     * Controls randomness of the model output. Range typically [0.0, 2.0].
     * Higher values (e.g., 1.0+) make output more creative and diverse; lower values (e.g., 0.2) make it more deterministic.
     * When null, the parameter is not sent.
     */
    private Double temperature;

    public OllamaSettings(String uri, String model) {
        this.uri = uri;
        this.model = model;
    }
    public OllamaSettings() {
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Boolean getThink() {
        return think;
    }

    public void setThink(Boolean think) {
        this.think = think;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    @Override
    public void validateFields(List<String> errors) {
        if (StringUtils.isBlank(uri)) {
            errors.add("Value of 'uri' field cannot be empty.");
        }

        if (StringUtils.isBlank(model)) {
            errors.add("Value of 'model' field cannot be empty.");
        }

        if (temperature != null && temperature < 0) {
            errors.add("Value of 'temperature' field must be non-negative.");
        }
    }

    @Override
    public EnumSet<AiSettingsCompareDifferences> compare(AbstractAiSettings other) {
        if (!(other instanceof OllamaSettings)) {
            return EnumSet.of(AiSettingsCompareDifferences.All);
        }

        OllamaSettings otherSettings = (OllamaSettings) other;
        EnumSet<AiSettingsCompareDifferences> diff = EnumSet.of(AiSettingsCompareDifferences.None);

        if (!Objects.equals(this.model, otherSettings.model)) {
            diff.remove(AiSettingsCompareDifferences.None);
            diff.add(AiSettingsCompareDifferences.ModelArchitecture);
        }

        if (!Objects.equals(this.uri, otherSettings.uri)) {
            diff.remove(AiSettingsCompareDifferences.None);
            diff.add(AiSettingsCompareDifferences.EndpointConfiguration);
        }

        if (!Objects.equals(this.think, otherSettings.think)) {
            diff.remove(AiSettingsCompareDifferences.None);
            diff.add(AiSettingsCompareDifferences.EndpointConfiguration);
        }

        boolean hasTemp = this.temperature != null;
        boolean otherHasTemp = otherSettings.temperature != null;

        if (hasTemp != otherHasTemp ||
                (hasTemp && otherHasTemp &&
                        Math.abs(this.temperature - otherSettings.temperature) > 0.0001)) {
            diff.remove(AiSettingsCompareDifferences.None);
            diff.add(AiSettingsCompareDifferences.EndpointConfiguration);
        }

        return diff;
    }
}