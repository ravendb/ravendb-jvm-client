package net.ravendb.client.documents.AI;

import org.apache.commons.lang3.StringUtils;

/**
 * Controls the output format for a single AI conversation turn.
 * <p>
 * A default output schema must still be provided when creating the agent. AiOutputOptions lets you override
 * that default on a per-call basis - the options set here take precedence over the agent-level schema for the
 * duration of that turn only.
 */
public class AiOutputOptions {

    private Object sampleObject;
    private String outputSchema;
    private boolean noSchema;

    /**
     * Creates empty options. Set {@link #setNoSchema(boolean)} to true to disable structured output for the turn.
     */
    public AiOutputOptions() {
    }

    /**
     * Override the schema using a sample object. The server converts it to a JSON schema at request time.
     * The sample object should match the answer type expected from the conversation call.
     *
     * @param sampleObject the sample object
     */
    public AiOutputOptions(Object sampleObject) {
        if (sampleObject == null) {
            throw new IllegalArgumentException("sampleObject cannot be null");
        }
        // the answer type is erased at runtime, so a String sample object (only reachable through the generic
        // sample-object overloads) stands in for C#'s "sample object with a raw string answer type" check
        if (sampleObject instanceof String) {
            throw new IllegalStateException("When the answer type is a raw string you cannot set an output schema, because the model is expected to return just a string, with no structure. " +
                    "You can either set noSchema to true, or remove the output schema and sample object. " +
                    "To pass an explicit JSON schema, use AiOutputOptions(String outputSchema).");
        }
        this.sampleObject = sampleObject;
    }

    /**
     * Override the schema using an explicit JSON schema string.
     *
     * @param outputSchema the JSON schema
     */
    public AiOutputOptions(String outputSchema) {
        if (StringUtils.isBlank(outputSchema)) {
            throw new IllegalArgumentException("outputSchema cannot be null or whitespace");
        }
        this.outputSchema = outputSchema;
    }

    /**
     * @return A sample object used to generate a JSON schema for structured output.
     */
    public Object getSampleObject() {
        return sampleObject;
    }

    /**
     * @return An explicit JSON schema string for structured output.
     */
    public String getOutputSchema() {
        return outputSchema;
    }

    /**
     * When true, disables structured output entirely.
     * The LLM returns free-form text instead of JSON conforming to a schema, and the answer is a raw String.
     *
     * @return Whether structured output is disabled
     */
    public boolean isNoSchema() {
        return noSchema;
    }

    public void setNoSchema(boolean noSchema) {
        this.noSchema = noSchema;
    }
}
