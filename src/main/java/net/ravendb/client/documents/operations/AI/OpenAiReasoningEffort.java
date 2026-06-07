package net.ravendb.client.documents.operations.AI;

import net.ravendb.client.primitives.UseSharpEnum;

/**
 * Specifies the reasoning effort level used by supported models.
 * Controls how much internal reasoning the model performs,
 * affecting latency and response variability.
 */
@UseSharpEnum
public enum OpenAiReasoningEffort {
    MINIMAL,
    LOW,
    MEDIUM,
    HIGH
}
