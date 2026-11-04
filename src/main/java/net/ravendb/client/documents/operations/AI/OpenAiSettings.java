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
     * The reasoning_effort to send to the provider, controlling the reasoning depth
     * of supported models (such as the GPT-5 family). Lower values reduce internal reasoning,
     * which may improve latency and reduce variability in responses.
     * <p>
     * Typical values are none, minimal, low, medium, high, xhigh and max. Model families accept
     * different subsets, for example minimal is rejected by gpt-5.1 and later, which take none instead.
     * When not set the field is omitted and the model applies its own default.
     * <p>
     * Sent as supplied apart from trimming, and not validated by RavenDB, so new provider values
     * can be used without waiting for a RavenDB release. Providers match it exactly: OpenAI accepts
     * lowercase values only. Legacy {@link OpenAiReasoningEffort} member names persisted by
     * older clients (e.g. High) are normalized to the provider form by the server.
     */
    private String reasoningEffort;

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
                          String reasoningEffort,
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

    public String getReasoningEffort() {
        return reasoningEffort;
    }

    public void setReasoningEffort(String reasoningEffort) {
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
