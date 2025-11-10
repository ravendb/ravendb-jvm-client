package net.ravendb.client.documents.AI;

import net.ravendb.client.documents.operations.AI.AiUsage;
import java.time.Duration;

public class AiAnswer<TAnswer> {
    /**
     * The answer content produced by the AI.
     */
    private TAnswer answer;

    /**
     * The status of the conversation.
     */
    private AiConversationResult status;

    /**
     * Token usage reported by the model for generating this answer (prompt/completion/total).
     */
    private AiUsage usage;

    /**
     * The total time elapsed to produce the answer(measured from the server's request to the LLM until the response was received).
     */
    private Duration elapsed;

    public AiAnswer() {
    }

    public AiUsage getUsage() {
        return usage;
    }

    public void setUsage(AiUsage usage) {
        this.usage = usage;
    }

    public Duration getElapsed() {
        return elapsed;
    }

    public void setElapsed(Duration elapsed) {
        this.elapsed = elapsed;
    }

    public TAnswer getAnswer() {
        return answer;
    }

    public void setAnswer(TAnswer answer) {
        this.answer = answer;
    }

    public AiConversationResult getStatus() {
        return status;
    }

    public void setStatus(AiConversationResult status) {
        this.status = status;
    }
}

