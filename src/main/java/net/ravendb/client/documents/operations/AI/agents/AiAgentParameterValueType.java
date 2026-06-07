package net.ravendb.client.documents.operations.AI.agents;

import net.ravendb.client.primitives.UseSharpEnum;

/**
 * Defines the expected JSON value type of an agent parameter.
 * Used for validation before execution.
 * {@link #DEFAULT} disables type validation.
 */
@UseSharpEnum
public enum AiAgentParameterValueType {
    DEFAULT, // Don't care - for backward compatibility
    STRING,
    NUMBER,
    BOOLEAN,
    ARRAY_OF_STRING,
    ARRAY_OF_NUMBER,
    ARRAY_OF_BOOLEAN,
    NULL
}
