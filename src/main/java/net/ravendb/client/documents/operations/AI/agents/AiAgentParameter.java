package net.ravendb.client.documents.operations.AI.agents;

public class AiAgentParameter {
    private String name;
    private String description;
    private Boolean sendToModel;

    public AiAgentParameter() {
    }

    public AiAgentParameter(String name) {
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("Name cannot be null or empty");
        this.name = name;
    }

    public AiAgentParameter(String name, String description) {
        this(name);
        if (description == null || description.isEmpty())
            throw new IllegalArgumentException("Description cannot be null or empty");
        this.description = description;
    }

    public AiAgentParameter(String name, String description, Boolean sendToModel) {
        this(name, description);
        this.sendToModel = sendToModel;
    }

    public Boolean getSendToModel() {
        return sendToModel;
    }

    public void setSendToModel(Boolean sendToModel) {
        this.sendToModel = sendToModel;
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

