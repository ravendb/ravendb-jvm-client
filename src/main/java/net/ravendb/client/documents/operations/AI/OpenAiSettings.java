package net.ravendb.client.documents.operations.AI;

import java.util.EnumSet;
import java.util.Objects;

/**
 * The configuration for the OpenAI API client.
 */
public final class OpenAiSettings extends OpenAiBaseSettings {

    /**
     * The value to use for the OpenAI-Organization request header.
     */
    private String organizationId;

    /**
     * The value to use for the OpenAI-Project request header.
     */
    private String projectId;

    private static final String OPENAI_BASE_URI = "https://api.openai.com/";

    public OpenAiSettings(String apiKey,
                          String endpoint,
                          String model,
                          String organizationId,
                          String projectId,
                          Integer dimensions,
                          Double temperature) {
        super(apiKey, endpoint, model, dimensions, temperature);
        this.organizationId = organizationId;
        this.projectId = projectId;
    }

    public OpenAiSettings(String apiKey,
                          String endpoint,
                          String model,
                          String organizationId,
                          String projectId,
                          Integer dimensions) {
        this(apiKey, endpoint, model, organizationId, projectId, dimensions, null);
    }

    public OpenAiSettings(String apiKey,
                          String endpoint,
                          String model,
                          String organizationId,
                          String projectId) {
        this(apiKey, endpoint, model, organizationId, projectId, null, null);
    }

    public OpenAiSettings(String apiKey,
                          String endpoint,
                          String model,
                          Integer dimensions,
                          Double temperature) {
        this(apiKey, endpoint, model, null, null, dimensions, temperature);
    }

    public OpenAiSettings(String apiKey,
                          String endpoint,
                          String model) {
        this(apiKey, endpoint, model, null, null, null, null);
    }

    public OpenAiSettings(){
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @Override
    public String getBaseEndpointUri() {
        String uri = super.getBaseEndpointUri();
        if (OPENAI_BASE_URI.equals(uri)) {
            return uri + "v1/";
        }
        return uri;
    }

    @Override
    public EnumSet<AiSettingsCompareDifferences> compare(AbstractAiSettings other) {
        if (!(other instanceof OpenAiSettings)) {
            return EnumSet.of(AiSettingsCompareDifferences.All);
        }

        OpenAiSettings otherSettings = (OpenAiSettings) other;
        EnumSet<AiSettingsCompareDifferences> diff = super.compare(other);

        if (!Objects.equals(this.organizationId, otherSettings.organizationId) ||
                !Objects.equals(this.projectId, otherSettings.projectId)) {
            diff.remove(AiSettingsCompareDifferences.None);
            diff.add(AiSettingsCompareDifferences.AuthenticationSettings);
        }

        return diff;
    }
}
