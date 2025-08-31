package net.ravendb.client.documents.session.tokens;

import net.ravendb.client.documents.queries.vectorSearch.VectorEmbeddingType;
import net.ravendb.client.documents.queries.vectorSearch.fields.VectorField;
import net.ravendb.client.documents.session.IVectorEmbeddingFieldFactoryAccessor;
import net.ravendb.client.documents.session.VectorEmbeddingFieldValueFactory;
import net.ravendb.client.util.VectorSearchUtil;

public class VectorSearchToken extends WhereToken {

    public static final String EMBEDDING_PREFIX = "embedding.";
    public static final String EMBEDDING_FOR_DOCUMENT = EMBEDDING_PREFIX + "forDoc";
    public static final String EMBEDDING_FOR_RAW = EMBEDDING_PREFIX + "Raw";
    public static final String EMBEDDING_TEXT = EMBEDDING_PREFIX + "text";
    public static final String EMBEDDING_TEXT_INT8 = EMBEDDING_PREFIX + "text_i8";
    public static final String EMBEDDING_TEXT_INT1 = EMBEDDING_PREFIX + "text_i1";
    public static final String EMBEDDING_SINGLE = EMBEDDING_PREFIX + "f32";
    public static final String EMBEDDING_SINGLE_INT8 = EMBEDDING_PREFIX + "f32_i8";
    public static final String EMBEDDING_SINGLE_INT1 = EMBEDDING_PREFIX + "f32_i1";
    public static final String EMBEDDING_INT8 = EMBEDDING_PREFIX + "i8";
    public static final String EMBEDDING_INT1 = EMBEDDING_PREFIX + "i1";

    public static final VectorEmbeddingType DEFAULT_EMBEDDING_TYPE = VectorEmbeddingType.SINGLE;
    public static final boolean DEFAULT_IS_EXACT = false;

    private static final String AI_TASK_METHOD_NAME = "ai.task";

    private final Float similarityThreshold;
    private final VectorEmbeddingType sourceQuantizationType;
    private final VectorEmbeddingType targetQuantizationType;
    private final Integer numberOfCandidatesForQuerying;
    private final boolean isDocumentId;
    private final String embeddingsGenerationTaskIdentifierByValue;
    private final String embeddingsGenerationTaskIdentifier;

    private final String fieldName;
    private final String parameterName;

    public VectorSearchToken(
            String fieldName,
            String parameterName,
            VectorEmbeddingType sourceQuantizationType,
            VectorEmbeddingType targetQuantizationType,
            Float similarityThreshold,
            Integer numberOfCandidatesForQuerying,
            boolean isExact,
            boolean isDocumentId,
            String embeddingsGenerationTaskIdentifier,
            String embeddingsGenerationTaskIdentifierByValue) {

        super();
        this.fieldName = fieldName;
        this.parameterName = parameterName;

        this.sourceQuantizationType = sourceQuantizationType;
        this.targetQuantizationType = targetQuantizationType;
        this.similarityThreshold = similarityThreshold;
        this.numberOfCandidatesForQuerying = numberOfCandidatesForQuerying;
        this.isDocumentId = isDocumentId;
        this.embeddingsGenerationTaskIdentifier = embeddingsGenerationTaskIdentifier;
        this.embeddingsGenerationTaskIdentifierByValue = embeddingsGenerationTaskIdentifierByValue;
        this.setOptions(new WhereOptions(isExact));
    }

    public static String getTaskIdentifier(Object value) {
        if (value instanceof VectorEmbeddingFieldValueFactory) {
            if (((VectorEmbeddingFieldValueFactory) value).getEmbeddingsGenerationTaskIdentifier() != null) {
                return ((VectorEmbeddingFieldValueFactory) value).getEmbeddingsGenerationTaskIdentifier();
            }
        } else if (value instanceof VectorField) {
            return ((VectorField) value).getEmbeddingsGenerationTaskIdentifier();
        }
        return null;
    }

    public static <T> VectorEmbeddingType getSourceQuantizationType(IVectorEmbeddingFieldFactoryAccessor<T> fieldAccessor) {
        if (fieldAccessor.getSourceQuantizationType() != null) {
            return fieldAccessor.getSourceQuantizationType();
        }
        return DEFAULT_EMBEDDING_TYPE;
    }

    public static <T> VectorEmbeddingType getTargetQuantizationType(IVectorEmbeddingFieldFactoryAccessor<T> fieldAccessor) {
        if (fieldAccessor.getDestinationQuantizationType() != null) {
            return fieldAccessor.getDestinationQuantizationType();
        }
        return DEFAULT_EMBEDDING_TYPE;
    }

    @Override
    public void writeTo(StringBuilder writer) {
        if (this.getOptions().getBoost() != null) {
            writer.append("boost(");
        }

        if (Boolean.TRUE.equals(this.getOptions().isExact())) {
            writer.append("exact(");
        }

        writer.append("vector.search(");
        if (this.sourceQuantizationType == VectorEmbeddingType.SINGLE
                && this.targetQuantizationType == VectorEmbeddingType.SINGLE) {
            writer.append(this.fieldName);
        } else {
            String methodName = VectorSearchUtil.vectorSearchConfigurationToMethodName(this.sourceQuantizationType, this.targetQuantizationType);

            if (this.sourceQuantizationType == VectorEmbeddingType.TEXT && embeddingsGenerationTaskIdentifier != null) {
                writer.append(methodName)
                        .append("(")
                        .append(fieldName)
                        .append(", ")
                        .append(AI_TASK_METHOD_NAME)
                        .append("('")
                        .append(embeddingsGenerationTaskIdentifier)
                        .append("'))");
            } else {
                writer.append(methodName)
                        .append("(")
                        .append(fieldName)
                        .append(")");
            }
        }

        writer.append(", ");

        if (isDocumentId) {
            writer.append(EMBEDDING_FOR_DOCUMENT)
                    .append("($")
                    .append(parameterName)
                    .append(")");
        } else if (embeddingsGenerationTaskIdentifierByValue != null){
            writer.append(EMBEDDING_TEXT)
                    .append("($")
                    .append(parameterName)
                    .append(", ")
                    .append(AI_TASK_METHOD_NAME)
                    .append("('")
                    .append(embeddingsGenerationTaskIdentifierByValue)
                    .append("'))");
        } else {
            writer.append("$").append(parameterName);
        }

        boolean parametersAreDefault = similarityThreshold == null &&
                numberOfCandidatesForQuerying == null;

        if (!parametersAreDefault) {
            writer.append(", ")
                    .append(similarityThreshold != null ? similarityThreshold.toString() : "null");
            writer.append(", ")
                    .append(numberOfCandidatesForQuerying != null ? numberOfCandidatesForQuerying.toString() : "null");
        }

        writer.append(")");

        if (Boolean.TRUE.equals(this.getOptions().isExact())) {
            writer.append(")");
        }

        if (this.getOptions().getBoost() != null) {
            writer.append(", ")
                    .append(this.getOptions().getBoost().toString())
                    .append(")");
        }
    }
}
