package net.ravendb.client.documents.operations.AI.agents;

public class AiAgentToolSubAgent {

    private String identifier;
    private String description;

    public AiAgentToolSubAgent() {
    }

    public AiAgentToolSubAgent(String identifier, String description) {
        this.identifier = identifier;
        this.description = description;
    }

    /**
     * @return The identifier of the sub-agent that we can call
     */
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * @return The description for the sub-agent (which the model will use to decide when to call it)
     */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
