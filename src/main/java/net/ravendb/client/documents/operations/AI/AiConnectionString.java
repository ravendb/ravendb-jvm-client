package net.ravendb.client.documents.operations.AI;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import net.ravendb.client.documents.operations.connectionStrings.ConnectionString;
import net.ravendb.client.serverwide.ConnectionStringType;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * Represents an AI service connection string configuration.
 * Supports multiple AI providers (OpenAI, Azure OpenAI, Ollama, Google, HuggingFace, Mistral AI, Vertex AI, Embedded).
 * Only one provider can be configured per connection string.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class AiConnectionString extends ConnectionString {

    private String identifier;
    private OpenAiSettings openAiSettings;
    private AzureOpenAiSettings azureOpenAiSettings;
    private OllamaSettings ollamaSettings;
    private EmbeddedSettings embeddedSettings;
    private GoogleSettings googleSettings;
    private HuggingFaceSettings huggingFaceSettings;
    private MistralAiSettings mistralAiSettings;
    private VertexSettings vertexSettings;
    private AiModelType modelType;
    private final ConnectionStringType type = ConnectionStringType.AI;

    private static final String[] PROVIDER_KEYS = {
            "openAiSettings",
            "azureOpenAiSettings",
            "ollamaSettings",
            "embeddedSettings",
            "googleSettings",
            "huggingFaceSettings",
            "mistralAiSettings",
            "vertexSettings"
    };

    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        List<AbstractAiSettings> allSettings = new ArrayList<>();

        if (openAiSettings != null) allSettings.add(openAiSettings);
        if (azureOpenAiSettings != null) allSettings.add(azureOpenAiSettings);
        if (ollamaSettings != null) allSettings.add(ollamaSettings);
        if (embeddedSettings != null) allSettings.add(embeddedSettings);
        if (googleSettings != null) allSettings.add(googleSettings);
        if (huggingFaceSettings != null) allSettings.add(huggingFaceSettings);
        if (mistralAiSettings != null) allSettings.add(mistralAiSettings);
        if (vertexSettings != null) allSettings.add(vertexSettings);

        for (AbstractAiSettings setting : allSettings) {
            setting.validateFields(errors);
        }

        if (allSettings.isEmpty()) {
            errors.add("At least one of the following settings must be set: " + String.join(", ", PROVIDER_KEYS));
        } else if (allSettings.size() > 1) {
            List<String> configured = new ArrayList<>();
            if (openAiSettings != null) configured.add("openAiSettings");
            if (azureOpenAiSettings != null) configured.add("azureOpenAiSettings");
            if (ollamaSettings != null) configured.add("ollamaSettings");
            if (embeddedSettings != null) configured.add("embeddedSettings");
            if (googleSettings != null) configured.add("googleSettings");
            if (huggingFaceSettings != null) configured.add("huggingFaceSettings");
            if (mistralAiSettings != null) configured.add("mistralAiSettings");
            if (vertexSettings != null) configured.add("vertexSettings");

            errors.add("Only one of the following settings can be set: " + String.join(", ", configured));
        }

        return errors;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public VertexSettings getVertexSettings() { return vertexSettings; }
    public void setVertexSettings(VertexSettings vertexSettings) { this.vertexSettings = vertexSettings; }

    public HuggingFaceSettings getHuggingFaceSettings() { return huggingFaceSettings; }
    public void setHuggingFaceSettings(HuggingFaceSettings huggingFaceSettings) { this.huggingFaceSettings = huggingFaceSettings; }

    public MistralAiSettings getMistralAiSettings() { return mistralAiSettings; }
    public void setMistralAiSettings(MistralAiSettings mistralAiSettings) { this.mistralAiSettings = mistralAiSettings; }

    public GoogleSettings getGoogleSettings() { return googleSettings; }
    public void setGoogleSettings(GoogleSettings googleSettings) { this.googleSettings = googleSettings; }

    public OllamaSettings getOllamaSettings() { return ollamaSettings; }
    public void setOllamaSettings(OllamaSettings ollamaSettings) { this.ollamaSettings = ollamaSettings; }

    public EmbeddedSettings getEmbeddedSettings() { return embeddedSettings; }
    public void setEmbeddedSettings(EmbeddedSettings embeddedSettings) { this.embeddedSettings = embeddedSettings; }

    public AzureOpenAiSettings getAzureOpenAiSettings() { return azureOpenAiSettings; }
    public void setAzureOpenAiSettings(AzureOpenAiSettings azureOpenAiSettings) { this.azureOpenAiSettings = azureOpenAiSettings;}

    public OpenAiSettings getOpenAiSettings() { return openAiSettings; }
    public void setOpenAiSettings(OpenAiSettings openAiSettings) { this.openAiSettings = openAiSettings; }

    public AiModelType getModelType() { return modelType; }
    public void setModelType(AiModelType modelType) { this.modelType = modelType; }

    public AiConnectorType getActiveProvider() {
        if (openAiSettings != null) return AiConnectorType.OpenAi;
        if (azureOpenAiSettings != null) return AiConnectorType.AzureOpenAi;
        if (ollamaSettings != null) return AiConnectorType.Ollama;
        if (embeddedSettings != null) return AiConnectorType.Embedded;
        if (googleSettings != null) return AiConnectorType.Google;
        if (huggingFaceSettings != null) return AiConnectorType.HuggingFace;
        if (mistralAiSettings != null) return AiConnectorType.MistralAi;
        if (vertexSettings != null) return AiConnectorType.Vertex;
        return AiConnectorType.None;
    }

    AbstractAiSettings getActiveProviderInstance() {
        if (openAiSettings != null) return openAiSettings;
        if (azureOpenAiSettings != null) return azureOpenAiSettings;
        if (ollamaSettings != null) return ollamaSettings;
        if (embeddedSettings != null) return embeddedSettings;
        if (googleSettings != null) return googleSettings;
        if (huggingFaceSettings != null) return huggingFaceSettings;
        if (mistralAiSettings != null) return mistralAiSettings;
        if (vertexSettings != null) return vertexSettings;
        return null;
    }

    public EnumSet<AiSettingsCompareDifferences> compare(AiConnectionString other) {
        if (other == null) return EnumSet.of(AiSettingsCompareDifferences.All);

        EnumSet<AiSettingsCompareDifferences> result = EnumSet.of(AiSettingsCompareDifferences.None);

        if (!Objects.equals(this.identifier, other.identifier)) {
            result.remove(AiSettingsCompareDifferences.None);
            result.add(AiSettingsCompareDifferences.Identifier);
        }

        if (!Objects.equals(this.modelType, other.modelType)) {
            result.remove(AiSettingsCompareDifferences.None);
            result.add(AiSettingsCompareDifferences.ModelArchitecture);
        }

        AiConnectorType oldProvider = this.getActiveProvider();
        AiConnectorType newProvider = other.getActiveProvider();

        if (oldProvider != newProvider) {
            return EnumSet.of(AiSettingsCompareDifferences.All);
        }

        AbstractAiSettings oldInstance = this.getActiveProviderInstance();
        AbstractAiSettings newInstance = other.getActiveProviderInstance();

        if (oldInstance == null || newInstance == null) {
            return EnumSet.of(AiSettingsCompareDifferences.All);
        }

        EnumSet<AiSettingsCompareDifferences> providerDiffs;

        switch (oldProvider) {
            case OpenAi:
                providerDiffs = this.openAiSettings.compare(other.getOpenAiSettings());
                break;
            case AzureOpenAi:
                providerDiffs = this.azureOpenAiSettings.compare(other.getAzureOpenAiSettings());
                break;
            case Ollama:
                providerDiffs = this.ollamaSettings.compare(other.getOllamaSettings());
                break;
            case Embedded:
                providerDiffs = this.embeddedSettings.compare(other.getEmbeddedSettings());
                break;
            case Google:
                providerDiffs = this.googleSettings.compare(other.getGoogleSettings());
                break;
            case HuggingFace:
                providerDiffs = this.huggingFaceSettings.compare(other.getHuggingFaceSettings());
                break;
            case MistralAi:
                providerDiffs = this.mistralAiSettings.compare(other.getMistralAiSettings());
                break;
            case Vertex:
                providerDiffs = this.vertexSettings.compare(other.getVertexSettings());
                break;
            default:
                providerDiffs = EnumSet.of(AiSettingsCompareDifferences.All);
                break;
        }

        if (providerDiffs.size() != 0) {
            result.remove(AiSettingsCompareDifferences.None);
        }
        result.addAll(providerDiffs);
        return result;

    }

    public boolean isEqual(ConnectionString other) {
        if (!(other instanceof AiConnectionString)) return false;

        AiConnectionString otherAi = (AiConnectionString) other;

        if (!Objects.equals(this.getName(), otherAi.getName())) return false;
        if (!Objects.equals(this.identifier, otherAi.identifier)) return false;
        if (!Objects.equals(this.modelType, otherAi.modelType)) return false;

        EnumSet<AiSettingsCompareDifferences> diffs = this.compare(otherAi);
        return diffs.isEmpty() || (diffs.size() == 1 && diffs.contains(AiSettingsCompareDifferences.None));
    }

    boolean usingEncryptedCommunicationChannel() {
        AiConnectorType type = getActiveProvider();

        switch (type) {
            case Ollama:
                return ollamaSettings != null && ollamaSettings.getUri() != null &&
                        ollamaSettings.getUri().startsWith("https");
            case OpenAi:
                return openAiSettings != null && openAiSettings.getEndpoint() != null &&
                        openAiSettings.getEndpoint().startsWith("https");
            case AzureOpenAi:
                return azureOpenAiSettings != null && azureOpenAiSettings.getEndpoint() != null &&
                        azureOpenAiSettings.getEndpoint().startsWith("https");
            case MistralAi:
                return mistralAiSettings != null && mistralAiSettings.getEndpoint() != null &&
                        mistralAiSettings.getEndpoint().startsWith("https");
            case HuggingFace:
                return huggingFaceSettings == null ||
                        huggingFaceSettings.getEndpoint() == null ||
                        huggingFaceSettings.getEndpoint().startsWith("https");
            case Embedded:
            case Google:
            case Vertex:
                return true;
            default:
                throw new IllegalStateException("Unknown AI connector type: " + type);
        }
    }

    int getQueryEmbeddingsMaxConcurrentBatches(int globalDefault) {
        AbstractAiSettings provider = getActiveProviderInstance();
        return provider != null && provider.getEmbeddingsMaxConcurrentBatches() != null
                ? provider.getEmbeddingsMaxConcurrentBatches()
                : globalDefault;
    }

    @Override
    public ConnectionStringType getType() {
        return this.type;
    }
}