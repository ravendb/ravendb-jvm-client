package net.ravendb.client.documents.operations.AI.agents;

public class AiAgentParameter {
    private String name;
    private String description; // Optional

    public AiAgentParameter() {
        // Default constructor
    }

    public AiAgentParameter(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

