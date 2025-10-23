package net.ravendb.client.documents.operations.AI.agents;

public class AiAgentToolQuery {
    private String name;
    private String description;
    private String query;
    private String parametersSampleObject; // Optional: JSON example of parameters
    private String parametersSchema;       // Optional: JSON schema for parameters
    private AiAgentToolQueryOptions options;

    public AiAgentToolQuery() {
        // Default constructor
    }

    public AiAgentToolQuery(String name, String description, String query,
                            String parametersSampleObject, String parametersSchema) {
        this.name = name;
        this.description = description;
        this.query = query;
        this.parametersSampleObject = parametersSampleObject;
        this.parametersSchema = parametersSchema;
    }

    public AiAgentToolQueryOptions getOptions() { return this.options; }
    public void setOptions(AiAgentToolQueryOptions options) { this.options = options; }

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

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
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
