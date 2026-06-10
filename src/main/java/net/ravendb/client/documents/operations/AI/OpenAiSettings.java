package net.ravendb.client.documents.operations.AI;

import org.apache.commons.lang3.StringUtils;

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

    /**
     * Controls the reasoning depth used by supported models (such as GPT-5 family).
     * Lower values reduce the amount of internal reasoning performed by the model,
     * which may improve latency and reduce variability in responses.
     * <p>
     * Note that this setting reduces the likelihood of non-deterministic behavior,
     * but does not guarantee fully deterministic responses.
     */
    private OpenAiReasoningEffort reasoningEffort;

    /**
     * Optional seed used to make the model's sampling more reproducible across requests.
     * When provided, identical inputs and configuration may produce the same outputs
     * more consistently across runs.
     * <p>
     * This improves response stability (for example in automated tests),
     * but does not guarantee fully deterministic results due to internal model behavior.
     */
    private Integer seed;

    private static final String OPENAI_BASE_URI = "https://api.openai.com/";

    public OpenAiSettings(String apiKey,
                          String endpoint,
                          String model,
                          String organizationId,
                          String projectId,
                          Integer dimensions,
                          Double temperature,
                          OpenAiReasoningEffort reasoningEffort,
                          Integer seed) {
        super(apiKey, endpoint, model, dimensions, temperature);
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.reasoningEffort = reasoningEffort;
        this.seed = seed;
    }

    public OpenAiSettings(String apiKey,
                          String endpoint,
                          String model,
                          String organizationId,
                          String projectId,
                          Integer dimensions,
                          Double temperature) {
        this(apiKey, endpoint, model, organizationId, projectId, dimensions, temperature, null, null);
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

    public OpenAiReasoningEffort getReasoningEffort() {
        return reasoningEffort;
    }

    public void setReasoningEffort(OpenAiReasoningEffort reasoningEffort) {
        this.reasoningEffort = reasoningEffort;
    }

    public Integer getSeed() {
        return seed;
    }

    public void setSeed(Integer seed) {
        this.seed = seed;
    }

    @Override
    public String getBaseEndpointUri() {
        String uri = StringUtils.isEmpty(getEndpoint()) ? OPENAI_BASE_URI : super.getBaseEndpointUri();
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
