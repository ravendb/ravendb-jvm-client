package net.ravendb.client.documents.operations.AI;

/**
 * Flags enum for detecting differences between AI settings configurations.
 * Uses bitwise operations for combining multiple differences.
 */
public enum AiSettingsCompareDifferences {
    None(0),
    AuthenticationSettings(1 << 0),      // 1
    EndpointConfiguration(1 << 1),       // 2
    ModelArchitecture(1 << 2),           // 4
    EmbeddingDimensions(1 << 3),         // 8
    DeploymentConfiguration(1 << 4),     // 16
    Identifier(1 << 5),                  // 32
    All((~(~0 << 6)));                   // 63 (all bits set)

    private final int value;

    AiSettingsCompareDifferences(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static AiSettingsCompareDifferences fromValue(int value) {
        for (AiSettingsCompareDifferences diff : values()) {
            if (diff.getValue() == value) {
                return diff;
            }
        }
        return None;
    }
}
