package net.ravendb.client.documents.operations.AI.agents;

import net.ravendb.client.primitives.UseSharpEnum;

/**
 * Controls the level of detail when reading conversation messages.
 */
@UseSharpEnum
public enum AiConversationDetailLevel {

    /**
     * User messages and assistant messages that have content only.
     * System prompts, tool calls, summaries, and internal messages are excluded.
     */
    SIMPLE,

    /**
     * Includes system messages, tool calls with results, and per-message usage.
     * Summaries and internal messages are excluded.
     */
    DETAILED,

    /**
     * No filtering. Includes all messages: system, tool calls, summaries, internal.
     * Intended for debugging and future-proofing.
     */
    FULL
}
