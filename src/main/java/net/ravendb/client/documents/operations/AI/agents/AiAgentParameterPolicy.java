package net.ravendb.client.documents.operations.AI.agents;

import net.ravendb.client.primitives.UseSharpEnum;

/**
 * Defines policy flags that control how a parameter behaves,
 * especially when used across parent and sub-agent boundaries.
 */
@UseSharpEnum
public enum AiAgentParameterPolicy {
    /**
     * No special behavior.
     */
    DEFAULT,

    /**
     * Prevents the model from generating a value for this parameter.
     * When used in a sub-agent, the value may only be inherited
     * from the parent agent's parameters.
     */
    FORBID_MODEL_GENERATION
}
