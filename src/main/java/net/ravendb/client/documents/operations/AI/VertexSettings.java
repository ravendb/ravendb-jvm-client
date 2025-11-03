package net.ravendb.client.documents.operations.AI;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang3.StringUtils;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Settings for Google Vertex AI service.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VertexSettings extends AbstractAiSettings {

    /**
     * Represents the version of the Vertex AI API.
     */
    public enum VertexAIVersion {
        V1,
        V1_Beta
    }

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

    /**
     * Extracts the project ID from the Google credentials JSON.
     */
    public String getProjectId() {
        try {
            Pattern pattern = Pattern.compile("\"project_id\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(googleCredentialsJson);
            if (matcher.find()) {
                String projectId = matcher.group(1);
                if (StringUtils.isBlank(projectId)) {
                    throw new IllegalArgumentException("Couldn't find project_id in the provided googleCredentialsJson.");
                }
                return projectId;
            } else {
                throw new IllegalArgumentException("Couldn't find project_id in the provided googleCredentialsJson.");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse googleCredentialsJson: " + e.getMessage(), e);
        }
    }

    @Override
    public void validate(List<String> errors) {
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
    public AiSettingsCompareDifferences compare(AbstractAiSettings other) {
        if (!(other instanceof VertexSettings)) {
            return AiSettingsCompareDifferences.All;
        }

        VertexSettings otherSettings = (VertexSettings) other;
        int diff = AiSettingsCompareDifferences.None.getValue();

        if (!Objects.equals(this.model, otherSettings.model) ||
                !Objects.equals(this.aiVersion, otherSettings.aiVersion)) {
            diff |= AiSettingsCompareDifferences.ModelArchitecture.getValue();
        }

        if (!Objects.equals(this.googleCredentialsJson, otherSettings.googleCredentialsJson)) {
            diff |= AiSettingsCompareDifferences.AuthenticationSettings.getValue();
        }

        if (!Objects.equals(this.location, otherSettings.location)) {
            diff |= AiSettingsCompareDifferences.DeploymentConfiguration.getValue();
        }

        return AiSettingsCompareDifferences.values()[diff];
    }
}
