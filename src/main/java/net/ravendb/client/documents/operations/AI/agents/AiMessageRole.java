package net.ravendb.client.documents.operations.AI.agents;

import net.ravendb.client.primitives.UseSharpEnum;

/**
 * The role of the sender of a conversation message.
 */
@UseSharpEnum
public enum AiMessageRole {
    SYSTEM,
    USER,
    ASSISTANT,
    SUMMARY,
    INTERNAL
}
