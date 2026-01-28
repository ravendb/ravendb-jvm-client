package net.ravendb.client.documents.operations.AI;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.ravendb.client.documents.operations.etl.EtlType;
import net.ravendb.client.documents.operations.etl.Transformation;
import net.ravendb.client.documents.queries.vectorSearch.VectorEmbeddingType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class EmbeddingsGenerationConfiguration extends AbstractAiIntegrationConfiguration {

    private String identifier;
    private String collection;
    private List<EmbeddingPathConfiguration> embeddingsPathConfigurations;
    private EmbeddingsTransformation embeddingsTransformation;
    private VectorEmbeddingType quantization;
    private ChunkingOptions chunkingOptionsForQuerying;

    @JsonProperty("EmbeddingsCacheExpiration")
    private long embeddingsCacheExpiration = Duration.ofDays(90).toMillis();

    @JsonProperty("EmbeddingsCacheForQueryingExpiration")
    private long embeddingsCacheForQueryingExpiration = Duration.ofDays(14).toMillis();

    private static final String PATHS_TRANSFORMATION_NAME = "embeddings-from-paths";
    private static final String SCRIPT_TRANSFORMATION_NAME = "embeddings-transform-script";

    @Override
    public EtlType getEtlType() { return EtlType.EMBEDDINGS_GENERATION; }

    @JsonIgnore
    private List<Transformation> transforms;

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

    public List<EmbeddingPathConfiguration> getEmbeddingsPathConfigurations() {
        return embeddingsPathConfigurations;
    }

    public void setEmbeddingsPathConfigurations(List<EmbeddingPathConfiguration> embeddingsPathConfigurations) {
        this.embeddingsPathConfigurations = embeddingsPathConfigurations;
    }

    public EmbeddingsTransformation getEmbeddingsTransformation() {
        return embeddingsTransformation;
    }

    public void setEmbeddingsTransformation(EmbeddingsTransformation embeddingsTransformation) {
        this.embeddingsTransformation = embeddingsTransformation;
    }

    public VectorEmbeddingType getQuantization() {
        return quantization;
    }

    public void setQuantization(VectorEmbeddingType quantization) {
        this.quantization = quantization;
    }

    public ChunkingOptions getChunkingOptionsForQuerying() {
        return chunkingOptionsForQuerying;
    }

    public void setChunkingOptionsForQuerying(ChunkingOptions chunkingOptionsForQuerying) {
        this.chunkingOptionsForQuerying = chunkingOptionsForQuerying;
    }

    public long getEmbeddingsCacheExpiration() {
        return embeddingsCacheExpiration;
    }

    public void setEmbeddingsCacheExpiration(Duration embeddingsCacheExpiration) {
        this.embeddingsCacheExpiration = embeddingsCacheExpiration.toMillis();
    }

    public long getEmbeddingsCacheForQueryingExpiration() {
        return embeddingsCacheForQueryingExpiration;
    }

    public void setEmbeddingsCacheForQueryingExpiration(Duration embeddingsCacheForQueryingExpiration) {
        this.embeddingsCacheForQueryingExpiration = embeddingsCacheForQueryingExpiration.toMillis();
    }

    public String getTransformationName() {
        return embeddingsTransformation == null
                ? PATHS_TRANSFORMATION_NAME
                : SCRIPT_TRANSFORMATION_NAME;
    }

    @Override
    @Deprecated
    public List<Transformation> getTransforms() {
        if (embeddingsTransformation == null) {
            if (transforms == null) {
                Transformation t = new Transformation();
                t.setName(PATHS_TRANSFORMATION_NAME);
                t.setCollections(Collections.singletonList(collection));
                transforms = new ArrayList<>();
                transforms.add(t);
            }
            return transforms;
        }

        if (transforms == null) {
            Transformation t = new Transformation();
            t.setName(SCRIPT_TRANSFORMATION_NAME);
            t.setCollections(Collections.singletonList(collection));
            t.setScript(embeddingsTransformation.getScript());
            transforms = new ArrayList<>();
            transforms.add(t);
        }

        return transforms;
    }

    @Override
    @Deprecated
    public void setTransforms(List<Transformation> transforms) {
        throw new UnsupportedOperationException(
                "EmbeddingsGenerationConfiguration doesn't support multiple transformations. " +
                        "Use EmbeddingsTransformation property instead.");
    }
}
