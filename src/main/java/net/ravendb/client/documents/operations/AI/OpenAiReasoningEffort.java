package net.ravendb.client.documents.operations.AI;

import net.ravendb.client.primitives.UseSharpEnum;

import java.util.Locale;

/**
 * Specifies the reasoning effort level used by supported models.
 * Kept for migration only; use the string-based {@link OpenAiSettings#setReasoningEffort(String)} instead.
 *
 * @deprecated Use the string-based OpenAiSettings reasoningEffort instead, e.g. {@code OpenAiReasoningEffort.HIGH.toReasoningEffort()}.
 */
@Deprecated
@UseSharpEnum
public enum OpenAiReasoningEffort {
    MINIMAL,
    LOW,
    MEDIUM,
    HIGH;

    /**
     * Converts a legacy value to the lowercase form expected by the provider, e.g. HIGH to "high".
     * @return reasoning effort value for {@link OpenAiSettings#setReasoningEffort(String)}
     */
    public String toReasoningEffort() {
        return name().toLowerCase(Locale.ROOT);
    }
}
