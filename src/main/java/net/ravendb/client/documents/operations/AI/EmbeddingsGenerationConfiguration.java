package net.ravendb.client.documents.operations.AI;

import net.ravendb.client.documents.operations.etl.EtlType;
import net.ravendb.client.documents.operations.etl.Transformation;
import net.ravendb.client.documents.queries.vectorSearch.VectorEmbeddingType;
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

    private long embeddingsCacheExpiration = java.time.Duration.ofDays(90).toMillis();
    private long embeddingsCacheForQueryingExpiration = java.time.Duration.ofDays(14).toMillis();

    private static final String PATHS_TRANSFORMATION_NAME = "embeddings-from-paths";
    private static final String SCRIPT_TRANSFORMATION_NAME = "embeddings-transform-script";

    private List<Transformation> transforms;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

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
        return EtlType.EMBEDDINGS_GENERATION;
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

    public void setEmbeddingsCacheExpiration(long embeddingsCacheExpiration) {
        this.embeddingsCacheExpiration = embeddingsCacheExpiration;
    }

    public long getEmbeddingsCacheForQueryingExpiration() {
        return embeddingsCacheForQueryingExpiration;
    }

    public void setEmbeddingsCacheForQueryingExpiration(long embeddingsCacheForQueryingExpiration) {
        this.embeddingsCacheForQueryingExpiration = embeddingsCacheForQueryingExpiration;
    }

    String getTransformationName() {
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

    @Override
    public boolean validate(List<String> errors, boolean validateName, boolean validateConnection, boolean validateIdentifier) {

        if (validateConnection && !isInitialized()) {
            throw new IllegalStateException("Embeddings Generation configuration must be initialized");
        }

        errors.clear();

        if (validateIdentifier) {
            List<String> idErrors = new ArrayList<>();
            if (!AiTaskIdentifierHelper.validateIdentifier(identifier, idErrors)) {
                errors.addAll(idErrors);
            }
        }

        if (validateName && (getName() == null || getName().isEmpty())) {
            errors.add("Name of Embeddings Generation configuration cannot be empty");
        }

        if (!isTestMode() && (getConnectionStringName() == null || getConnectionStringName().isEmpty())) {
            errors.add("ConnectionStringName cannot be empty");
        }

        if (validateConnection && !isTestMode()) {
            getConnection().validate(errors);
        }

        if (validateConnection) {
            if (getConnection().getModelType() != AiModelType.TextEmbeddings) {
                errors.add("Connection.ModelType of Embeddings Generation configuration must be TextEmbeddings");
            }
        }

        if (collection == null || collection.isEmpty()) {
            errors.add("Collection must be provided");
        }

        boolean noPaths = embeddingsPathConfigurations == null || embeddingsPathConfigurations.isEmpty();
        boolean noScript = embeddingsTransformation == null ||
                embeddingsTransformation.getScript() == null ||
                embeddingsTransformation.getScript().isEmpty();

        if (noPaths && noScript) {
            errors.add("Configuration must have either EmbeddingsPathConfigurations or EmbeddingsTransformation script specified");
        }

        if (embeddingsPathConfigurations != null) {
            for (EmbeddingPathConfiguration pathConfig : embeddingsPathConfigurations) {
                if (pathConfig.getChunkingOptions() != null) {
                    pathConfig.getChunkingOptions().validate(pathConfig.getPath(), errors);
                } else {
                    errors.add("Path '" + pathConfig.getPath() + "': ChunkingOptions must be provided.");
                }
            }
        }

        if (embeddingsTransformation != null) {
            embeddingsTransformation.validate(errors);
        }

        if (quantization == VectorEmbeddingType.TEXT) {
            errors.add("Quantization cannot be Text");
        }

        if (chunkingOptionsForQuerying == null) {
            errors.add("ChunkingOptionsForQuerying must be provided.");
        } else {
            if (chunkingOptionsForQuerying.getMaxTokensPerChunk() <= 0) {
                errors.add("ChunkingOptionsForQuerying must have MaxTokensPerChunk > 0.");
            }
            if (chunkingOptionsForQuerying.getOverlapTokens() < 0) {
                errors.add("ChunkingOptionsForQuerying must have OverlapTokens >= 0.");
            }
        }

        return errors.isEmpty();
    }

    @Override
    public boolean usingEncryptedCommunicationChannel() {
        return getConnection() != null && getConnection().usingEncryptedCommunicationChannel();
    }

    String generateIdentifier() {
        return generateIdentifier(getName());
    }

    boolean validateIdentifier(List<String> errors) {
        return AiTaskIdentifierHelper.validateIdentifier(identifier, errors);
    }

    static String generateIdentifier(String input) {
        return AiTaskIdentifierHelper.generateIdentifier(input);
    }
}
