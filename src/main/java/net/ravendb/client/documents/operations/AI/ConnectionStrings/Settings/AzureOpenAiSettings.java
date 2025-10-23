package net.ravendb.client.documents.operations.AI.ConnectionStrings.Settings;

import net.ravendb.client.documents.operations.AI.ConnectionStrings.AiSettingsCompareDifferences;
import java.util.List;
import java.util.Objects;

/**
 * Azure OpenAI settings.
 * Learn more: https://learn.microsoft.com/azure/cognitive-services/openai/how-to/create-resource
 */
public class AzureOpenAiSettings extends OpenAiBaseSettings {

    /**
     * Azure OpenAI deployment name.
     */
    private String deploymentName;

    public AzureOpenAiSettings(String apiKey, String endpoint, String model, String deploymentName,
                               Integer dimensions, Double temperature) {
        super(apiKey, endpoint, model, dimensions, temperature);
        this.deploymentName = deploymentName;
    }
    public AzureOpenAiSettings() {
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public void setDeploymentName(String deploymentName) {
        this.deploymentName = deploymentName;
    }

    @Override
    public void validate(List<String> errors) {
        super.validate(errors);

        if (deploymentName == null || deploymentName.trim().isEmpty()) {
            errors.add("Value for 'deploymentName' field cannot be empty.");
        }
    }

    @Override
    public AiSettingsCompareDifferences compare(AbstractAiSettings other) {
        if (!(other instanceof AzureOpenAiSettings)) {
            return AiSettingsCompareDifferences.All;
        }

        AiSettingsCompareDifferences differences = super.compare(other);

        AzureOpenAiSettings otherSettings = (AzureOpenAiSettings) other;

        if (!Objects.equals(this.deploymentName, otherSettings.deploymentName)) {
            differences = AiSettingsCompareDifferences.values()[differences.getValue()
                    | AiSettingsCompareDifferences.DeploymentConfiguration.getValue()];
        }

        return differences;
    }
}
