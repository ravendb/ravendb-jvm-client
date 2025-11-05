package net.ravendb.client.documents.operations.AI;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import java.io.IOException;
import java.util.*;

/**
 * Settings for Google Vertex AI service.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class VertexSettings extends AbstractAiSettings {
    /**
     * The model ID for the Vertex AI service.
     */
    private String model;

    /**
     * The Google Cloud service account credentials in JSON format.
     */
    private String googleCredentialsJson;

    /**
     * The version of Vertex AI to use.
     */
    private VertexAIVersion aiVersion;

    /**
     * The Google Cloud region/location for the Vertex AI service.
     */
    private String location;

    public VertexSettings(String model, String googleCredentialsJson, String location, VertexAIVersion aiVersion) {
        this.model = model;
        this.googleCredentialsJson = googleCredentialsJson;
        this.location = location;
        this.aiVersion = aiVersion;
    }

    public VertexSettings(String model, String googleCredentialsJson, String location) {
        this(model, googleCredentialsJson, location, null);
    }

    public VertexSettings() {
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getGoogleCredentialsJson() {
        return googleCredentialsJson;
    }

    public void setGoogleCredentialsJson(String googleCredentialsJson) {
        this.googleCredentialsJson = googleCredentialsJson;
    }

    public VertexAIVersion getAiVersion() {
        return aiVersion;
    }

    public void setAiVersion(VertexAIVersion aiVersion) {
        this.aiVersion = aiVersion;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    final String PROJECT_ID_KEY = "project_id";

    /**
     * Extracts the project ID from the Google credentials JSON.
     */
    public String getProjectId() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(googleCredentialsJson);

            Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                if (entry.getKey().equalsIgnoreCase(PROJECT_ID_KEY)) {
                    String projectId = entry.getValue().asText();
                    if (projectId == null || projectId.trim().isEmpty()) {
                        throw new IllegalArgumentException(
                                "Couldn't find " + PROJECT_ID_KEY + " in the provided googleCredentialsJson.");
                    }
                    return projectId;
                }
            }

            throw new IllegalArgumentException(
                    "Couldn't find " + PROJECT_ID_KEY + " in the provided googleCredentialsJson.");

        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to parse googleCredentialsJson: " + e.getMessage(), e);
        }
    }

    @Override
    public void validateFields(List<String> errors) {
        if (StringUtils.isBlank(model)) {
            errors.add("Value of 'model' field cannot be empty.");
        }

        if (StringUtils.isBlank(googleCredentialsJson)) {
            errors.add("Value of 'googleCredentialsJson' field cannot be empty.");
        } else {
            try {
                if (StringUtils.isBlank(getProjectId())) {
                    errors.add("Value of 'project_id' field in 'googleCredentialsJson' cannot be empty.");
                }
            } catch (Exception e) {
                errors.add("Invalid 'googleCredentialsJson': " + e.getMessage());
            }
        }

        if (StringUtils.isBlank(location)) {
            errors.add("Value of 'location' field cannot be empty.");
        }
    }

    @Override
    public EnumSet<AiSettingsCompareDifferences> compare(AbstractAiSettings other) {
        if (!(other instanceof VertexSettings)) {
            return EnumSet.of(AiSettingsCompareDifferences.All);
        }

        VertexSettings otherSettings = (VertexSettings) other;
        EnumSet<AiSettingsCompareDifferences> diff = EnumSet.of(AiSettingsCompareDifferences.None);

        if (!Objects.equals(this.model, otherSettings.model) ||
                !Objects.equals(this.aiVersion, otherSettings.aiVersion)) {
            diff.remove(AiSettingsCompareDifferences.None);
            diff.add(AiSettingsCompareDifferences.ModelArchitecture);
        }

        if (!Objects.equals(this.googleCredentialsJson, otherSettings.googleCredentialsJson)) {
            diff.remove(AiSettingsCompareDifferences.None);
            diff.add(AiSettingsCompareDifferences.AuthenticationSettings);
        }

        if (!Objects.equals(this.location, otherSettings.location)) {
            diff.remove(AiSettingsCompareDifferences.None);
            diff.add(AiSettingsCompareDifferences.DeploymentConfiguration);
        }

        return diff;
    }
}