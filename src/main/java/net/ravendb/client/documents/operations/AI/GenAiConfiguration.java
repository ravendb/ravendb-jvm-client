package net.ravendb.client.documents.operations.AI;

import net.ravendb.client.documents.operations.AI.agents.AiAgentToolQuery;
import net.ravendb.client.documents.operations.etl.EtlType;
import net.ravendb.client.documents.operations.etl.Transformation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GenAiConfiguration extends AbstractAiIntegrationConfiguration {

    private String identifier;
    private String collection;

    private GenAiTransformation genAiTransformation;

    private String prompt;
    private String jsonSchema;
    private String sampleObject;
    private String updateScript;

    private int maxConcurrency = DefaultMaxConcurrency;

    private List<AiAgentToolQuery> queries = new ArrayList<>();

    private boolean enableTracing;

    private Integer expirationInSec;

    private List<Transformation> transforms;

    private static final int DefaultMaxConcurrency = 4;

    final String transformationName = "GenAi-transform-script";

    @Override
    public String getDestination() {
        return identifier;
    }

    @Override
    public String getDefaultTaskName() {
        return identifier;
    }

    @Override
    public EtlType getEtlType() {
        return EtlType.GEN_AI;
    }

    @Override
    public boolean usingEncryptedCommunicationChannel() {
        return getConnection() != null && getConnection().usingEncryptedCommunicationChannel();
    }

    public String generateIdentifier() {
        return EmbeddingsGenerationConfiguration.generateIdentifier(getName());
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public GenAiTransformation getGenAiTransformation() {
        return genAiTransformation;
    }

    public void setGenAiTransformation(GenAiTransformation genAiTransformation) {
        this.genAiTransformation = genAiTransformation;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getJsonSchema() {
        return jsonSchema;
    }

    public void setJsonSchema(String jsonSchema) {
        this.jsonSchema = jsonSchema;
    }

    public String getSampleObject() {
        return sampleObject;
    }

    public void setSampleObject(String sampleObject) {
        this.sampleObject = sampleObject;
    }

    public String getUpdateScript() {
        return updateScript;
    }

    public void setUpdateScript(String updateScript) {
        this.updateScript = updateScript;
    }

    public int getMaxConcurrency() {
        return maxConcurrency;
    }

    public void setMaxConcurrency(int maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
    }

    public List<AiAgentToolQuery> getQueries() {
        return queries;
    }

    public void setQueries(List<AiAgentToolQuery> queries) {
        this.queries = queries;
    }

    public boolean isEnableTracing() {
        return enableTracing;
    }

    public void setEnableTracing(boolean enableTracing) {
        this.enableTracing = enableTracing;
    }

    public Integer getExpirationInSec() {
        return expirationInSec;
    }

    public void setExpirationInSec(Integer expirationInSec) {
        this.expirationInSec = expirationInSec;
    }

    @Override
    @Deprecated
    public List<Transformation> getTransforms() {
        if (transforms == null) {
            transforms = new ArrayList<>();
            Transformation t = new Transformation();
            t.setName(transformationName);
            t.setCollections(Collections.singletonList(collection));
            t.setScript(genAiTransformation != null ? genAiTransformation.getScript() : null);
            transforms.add(t);
        }
        return transforms;
    }

    @Override
    @Deprecated
    public void setTransforms(List<Transformation> transforms) {
        throw new UnsupportedOperationException(
                "GenAiConfiguration doesn't support multiple transformations. " +
                        "Use GenAiTransformation property instead."
        );
    }

    @Override
    public boolean validate(List<String> errors,
                            boolean validateName,
                            boolean validateConnection,
                            boolean validateIdentifier) {

        if (validateConnection && ! isInitialized()) {
            throw new IllegalStateException("GenAi configuration must be initialized");
        }

        errors.clear();

        if (validateIdentifier &&
                !AiTaskIdentifierHelper.validateIdentifier(identifier, errors)) {
            // errors already populated
        }

        if (validateName && (getName() == null || getName().isEmpty())) {
            errors.add("Name of GenAi configuration cannot be empty");
        }

        if (!isTestMode() && (getConnectionStringName() == null || getConnectionStringName().isEmpty())) {
            errors.add("ConnectionStringName cannot be empty");
        }

        if (validateConnection && !isTestMode()) {
            getConnection().validate(errors);
        }

        if (validateConnection) {
            if (getConnection().getModelType() != AiModelType.Chat) {
                errors.add("ModelType of GenAI configuration must be Chat");
            }
        }

        if (collection == null || collection.isEmpty()) {
            errors.add("Collection must be provided");
        }

        if (genAiTransformation == null) {
            errors.add("GenAiTransformation must be specified");
        } else {
            StringBuilder error = new StringBuilder();
            if (!genAiTransformation.validateScript(error)) {
                errors.add(error.toString());
            }
        }

        if (!isTestMode()) {
            if (prompt == null || prompt.isEmpty()) {
                errors.add("Prompt must be provided");
            }

            if ((jsonSchema == null || jsonSchema.isEmpty()) &&
                    (sampleObject == null || sampleObject.isEmpty())) {
                errors.add("You must provide either a JSON schema or a sample object");
            }

            if (updateScript == null || updateScript.isEmpty()) {
                errors.add("You must provide an update function");
            }
        }

        return errors.isEmpty();
    }
}
