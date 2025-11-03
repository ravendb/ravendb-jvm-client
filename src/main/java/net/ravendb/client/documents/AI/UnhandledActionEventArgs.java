package net.ravendb.client.documents.AI;

import net.ravendb.client.documents.operations.AI.agents.AiAgentActionRequest;

/**
 * Event arguments for an unhandled action in an AI agent conversation.
 */
public class UnhandledActionEventArgs {

    private AiConversation sender;
    private AiAgentActionRequest action;

    public UnhandledActionEventArgs(AiConversation sender, AiAgentActionRequest action) {
        this.action = action;
        this.sender = sender;
    }

    public AiConversation getSender() {
        return sender;
    }

    public void setSender(AiConversation sender) {
        this.sender = sender;
    }

    public AiAgentActionRequest getAction() {
        return action;
    }

    public void setAction(AiAgentActionRequest action) {
        this.action = action;
    }
}