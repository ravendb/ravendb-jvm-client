package net.ravendb.client.documents.operations.AI;

import net.ravendb.client.documents.operations.AI.agents.AiAgentToolQuery;
import net.ravendb.client.documents.operations.etl.EtlType;
import net.ravendb.client.documents.operations.etl.Transformation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GenAiConfiguration extends AbstractAiIntegrationConfiguration{

    private String identifier;
    private String collection;

    private GenAiTransformation genAiTransformation;

    private String prompt;
    private String jsonSchema;
    private String sampleObject;
    private String updateScript;

    private int maxConcurrency = DEFAULT_MAX_CONCURRENCY;

    private List<AiAgentToolQuery> queries = new ArrayList<>();

    private boolean enableTracing;

    private Integer expirationInSec;

    private List<Transformation> transforms;

    private static final int DEFAULT_MAX_CONCURRENCY = 4;

    final String transformationName = "GenAi-transform-script";

    public EtlType getEtlType() { return EtlType.GEN_AI; }

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
}
