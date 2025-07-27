package net.ravendb.client.documents.queries.vectorSearch.common;

import net.ravendb.client.documents.queries.vectorSearch.VectorEmbeddingType;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for vector field implementations
 * @param <T> The type of the field
 */
public abstract class VectorFieldBase<T> {
    private final T rawFieldName;
    private String fieldName;
    protected boolean byFieldMethodUsed = false;

    /**
     * Creates a new instance of VectorFieldBase
     * @param fieldName The field name
     */
    protected VectorFieldBase(T fieldName) {
        this.rawFieldName = fieldName;
        this.fieldName = fieldName.toString();
    }

    /**
     * Gets the raw field name
     * @return The raw field name
     */
    public T getRawFieldName() {
        return rawFieldName;
    }

    /**
     * Gets the formatted field name
     * @return The formatted field name
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Sets the formatted field name
     * @param fieldName The formatted field name
     */
    protected void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    /**
     * Gets the formatted field name based on source and destination embedding types
     * @param rawFieldName The raw field name
     * @param sourceType The source embedding type
     * @param destType The destination embedding type
     * @param taskIdentifier The task identifier (optional)
     * @return The formatted field name
     */
    protected String getFormattedFieldName(T rawFieldName, VectorEmbeddingType sourceType, 
                                          VectorEmbeddingType destType, String taskIdentifier) {
        // If using withField, return the field name as is
        if (byFieldMethodUsed) {
            return rawFieldName.toString();
        }

        Map<VectorEmbeddingType, Map<VectorEmbeddingType, String>> configurationMap = new HashMap<>();
        
        // Single source type mappings
        Map<VectorEmbeddingType, String> singleMap = new HashMap<>();
        singleMap.put(VectorEmbeddingType.SINGLE, "");
        singleMap.put(VectorEmbeddingType.INT8, "embedding.f32_i8");
        singleMap.put(VectorEmbeddingType.BINARY, "embedding.f32_i1");
        configurationMap.put(VectorEmbeddingType.SINGLE, singleMap);
        
        // Text source type mappings
        Map<VectorEmbeddingType, String> textMap = new HashMap<>();
        textMap.put(VectorEmbeddingType.SINGLE, "embedding.text");
        textMap.put(VectorEmbeddingType.INT8, "embedding.text_i8");
        textMap.put(VectorEmbeddingType.BINARY, "embedding.text_i1");
        configurationMap.put(VectorEmbeddingType.TEXT, textMap);
        
        // Int8 source type mappings
        Map<VectorEmbeddingType, String> int8Map = new HashMap<>();
        int8Map.put(VectorEmbeddingType.INT8, "embedding.i8");
        configurationMap.put(VectorEmbeddingType.INT8, int8Map);
        
        // Binary source type mappings
        Map<VectorEmbeddingType, String> binaryMap = new HashMap<>();
        binaryMap.put(VectorEmbeddingType.BINARY, "embedding.i1");
        configurationMap.put(VectorEmbeddingType.BINARY, binaryMap);

        // Get the embedding function for the source and destination types
        String embeddingFunction = null;
        if (configurationMap.containsKey(sourceType) && configurationMap.get(sourceType).containsKey(destType)) {
            embeddingFunction = configurationMap.get(sourceType).get(destType);
        }

        if (embeddingFunction == null) {
            return rawFieldName.toString();
        }

        // For text source type with task identifier, handle specially
        if (sourceType == VectorEmbeddingType.TEXT && taskIdentifier != null && !taskIdentifier.isEmpty()) {
            return embeddingFunction.isEmpty() 
                ? rawFieldName.toString() 
                : embeddingFunction + "(" + rawFieldName + ", ai.task('" + taskIdentifier + "'))";
        }

        // For empty embedding function (same source and destination for Single), return just the field name
        if (embeddingFunction.isEmpty()) {
            return rawFieldName.toString();
        }

        return embeddingFunction + "(" + rawFieldName + ")";
    }

    /**
     * Gets the formatted field name based on source and destination embedding types
     * @param rawFieldName The raw field name
     * @param sourceType The source embedding type
     * @param destType The destination embedding type
     * @return The formatted field name
     */
    protected String getFormattedFieldName(T rawFieldName, VectorEmbeddingType sourceType, VectorEmbeddingType destType) {
        return getFormattedFieldName(rawFieldName, sourceType, destType, null);
    }
}