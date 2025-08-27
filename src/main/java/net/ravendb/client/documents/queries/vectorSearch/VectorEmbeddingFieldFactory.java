package net.ravendb.client.documents.queries.vectorSearch;

import net.ravendb.client.documents.session.IVectorFieldFactory;
import net.ravendb.client.documents.queries.vectorSearch.fields.VectorEmbeddingField;
import net.ravendb.client.documents.queries.vectorSearch.fields.VectorEmbeddingTextField;
import net.ravendb.client.documents.queries.vectorSearch.fields.VectorField;
import net.ravendb.client.documents.session.IVectorEmbeddingField;
import net.ravendb.client.documents.session.IVectorEmbeddingTextField;
import net.ravendb.client.documents.session.IVectorField;

/**
 * Factory for creating vector fields
 * @param <T> The type of the field
 */
public class VectorEmbeddingFieldFactory<T> implements IVectorFieldFactory<T> {

    @Override
    public IVectorEmbeddingTextField withText(String fieldName) {
        return new VectorEmbeddingTextField<>(fieldName);
    }

    @Override
    public IVectorEmbeddingField withEmbedding(String fieldName, VectorEmbeddingType storedEmbeddingQuantization) {
        return new VectorEmbeddingField<>(fieldName, storedEmbeddingQuantization, false);
    }

    @Override
    public IVectorEmbeddingField withBase64(String fieldName, VectorEmbeddingType storedEmbeddingQuantization) {
        return new VectorEmbeddingField<>(fieldName, storedEmbeddingQuantization, true);
    }

    @Override
    public IVectorField withField(T fieldName, VectorEmbeddingType storedEmbeddingQuantization, VectorEmbeddingType destinationEmbeddingQuantization, String embeddingsGenerationTaskIdentifier) {
        return new VectorField<>(fieldName, storedEmbeddingQuantization, destinationEmbeddingQuantization, embeddingsGenerationTaskIdentifier);
    }

    @Override
    public IVectorField withField(String fieldName) {
        return new VectorField<>(fieldName);
    }
}