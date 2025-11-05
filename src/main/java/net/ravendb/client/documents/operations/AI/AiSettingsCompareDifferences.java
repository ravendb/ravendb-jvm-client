package net.ravendb.client.documents.operations.AI;

/**
 * Flags enum for detecting differences between AI settings configurations.
 * Uses bitwise operations for combining multiple differences.
 */
import java.util.EnumSet;

public enum AiSettingsCompareDifferences {
    None(0),
    Identifier(1 << 0),
    EmbeddingDimensions(1 << 1),
    ModelArchitecture(1 << 2),
    EndpointConfiguration(1 << 3),
    AuthenticationSettings(1 << 4),
    DeploymentConfiguration(1 << 5),
    // Combinations
    EmbeddingStructure(Identifier.value | EmbeddingDimensions.value | ModelArchitecture.value),
    ConnectionConfig(EndpointConfiguration.value | AuthenticationSettings.value),
    RequiresEmbeddingsRegeneration(EmbeddingStructure.value | DeploymentConfiguration.value),
    All(RequiresEmbeddingsRegeneration.value | ConnectionConfig.value);

    private final int value;

    AiSettingsCompareDifferences(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
