package net.ravendb.client.documents.queries.vectorSearch;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.session.IVectorFieldFactory;
import net.ravendb.client.documents.queries.vectorSearch.fields.VectorEmbeddingField;
import net.ravendb.client.documents.queries.vectorSearch.fields.VectorEmbeddingTextField;
import net.ravendb.client.documents.queries.vectorSearch.fields.VectorField;
import net.ravendb.client.documents.session.IVectorEmbeddingField;
import net.ravendb.client.documents.session.IVectorEmbeddingTextField;
import net.ravendb.client.documents.session.IVectorField;
import net.ravendb.client.extensions.expressionExtension;
import net.ravendb.client.util.SerializableFunction;

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
    public IVectorEmbeddingField withEmbedding(SerializableFunction<T, ?> propertySelector, VectorEmbeddingType storedEmbeddingQuantization) {
        String name = expressionExtension.toPropertyPath(propertySelector, DocumentConventions.defaultConventions);
        return new VectorEmbeddingField<>(name, storedEmbeddingQuantization, false);
    }

    @Override
    public IVectorEmbeddingField withEmbedding(String fieldName, VectorEmbeddingType storedEmbeddingQuantization) {
        return new VectorEmbeddingField<>(fieldName, storedEmbeddingQuantization, false);
    }

    @Override
    public IVectorEmbeddingField withEmbedding(String fieldName) {
        return new VectorEmbeddingField<>(fieldName, null, false);
    }

    @Override
    public IVectorEmbeddingField withBase64(String fieldName, VectorEmbeddingType storedEmbeddingQuantization) {
        return new VectorEmbeddingField<>(fieldName, storedEmbeddingQuantization, true);
    }

    @Override
    public IVectorField withField(String fieldName) {
        return new VectorField<>(fieldName);
    }

    @Override
    public IVectorField withField(SerializableFunction<T, ?> propertySelector) {
        String name = expressionExtension.toPropertyPath(propertySelector, DocumentConventions.defaultConventions);

        return new VectorField<>(name);
    }
}