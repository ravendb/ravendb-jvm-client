package net.ravendb.client.documents.operations.AI;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * Azure OpenAI settings.
 * Learn more: https://learn.microsoft.com/azure/cognitive-services/openai/how-to/create-resource
 */
public final class AzureOpenAiSettings extends OpenAiBaseSettings {

    /**
     * Azure OpenAI deployment name.
     */
    private String deploymentName;

    public AzureOpenAiSettings(String apiKey, String endpoint, String model, String deploymentName,
                               Integer dimensions, Double temperature) {
        super(apiKey, endpoint, model, dimensions, temperature);
        if (deploymentName == null || deploymentName.trim().isEmpty()) {
            throw new IllegalArgumentException("deploymentName cannot be null or empty");
        }
        this.deploymentName = deploymentName;
    }

    public AzureOpenAiSettings() {
    }

    public AzureOpenAiSettings(String apiKey, String endpoint, String model, String deploymentName, Integer dimensions) {
        this(apiKey, endpoint, model, deploymentName, dimensions, null);
    }

    public AzureOpenAiSettings(String apiKey, String endpoint, String model, String deploymentName, Double temperature) {
        this(apiKey, endpoint, model, deploymentName, null, temperature);
    }

    public AzureOpenAiSettings(String apiKey, String endpoint, String model, String deploymentName) {
        this(apiKey, endpoint, model, deploymentName, null, null);
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public void setDeploymentName(String deploymentName) {
        this.deploymentName = deploymentName;
    }

    @Override
    public void validateFields(List<String> errors) {
        super.validateFields(errors);

        if (deploymentName == null || deploymentName.trim().isEmpty()) {
            errors.add("Value for 'deploymentName' field cannot be empty.");
        }

        if (getEndpoint() == null || getEndpoint().trim().isEmpty()) {
            errors.add("Value of 'endpoint' field cannot be empty.");
        }
    }

    @Override
    public EnumSet<AiSettingsCompareDifferences> compare(AbstractAiSettings other) {
        if (!(other instanceof AzureOpenAiSettings)) {
            return EnumSet.of(AiSettingsCompareDifferences.All);
        }

        EnumSet<AiSettingsCompareDifferences> differences = super.compare(other);

        AzureOpenAiSettings otherSettings = (AzureOpenAiSettings) other;

        if (!Objects.equals(this.deploymentName, otherSettings.deploymentName)) {
            differences.remove(AiSettingsCompareDifferences.None);
            differences.add(AiSettingsCompareDifferences.DeploymentConfiguration);
        }

        return differences;
    }
}
