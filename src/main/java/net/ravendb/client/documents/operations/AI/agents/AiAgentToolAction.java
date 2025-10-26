package net.ravendb.client.documents.operations.AI.agents;

public class AiAgentToolAction {
    private String name;
    private String description;
    private String parametersSampleObject;
    private String parametersSchema;

    public AiAgentToolAction() {
    }

    public AiAgentToolAction(String name, String description, String parametersSampleObject, String parametersSchema) {
        this.name = name;
        this.description = description;
        this.parametersSampleObject = parametersSampleObject;
        this.parametersSchema = parametersSchema;
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

    public String getParametersSampleObject() {
        return parametersSampleObject;
    }

    public void setParametersSampleObject(String parametersSampleObject) {
        this.parametersSampleObject = parametersSampleObject;
    }

    public String getParametersSchema() {
        return parametersSchema;
    }

    public void setParametersSchema(String parametersSchema) {
        this.parametersSchema = parametersSchema;
    }
}
