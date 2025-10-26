package net.ravendb.client.documents.operations.AI;

public class AiAnswer<TAnswer> {
    private TAnswer answer;
    private AiConversationResult status;

    public AiAnswer() {
    }

    public AiAnswer(TAnswer answer, AiConversationResult status) {
        this.answer = answer;
        this.status = status;
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

