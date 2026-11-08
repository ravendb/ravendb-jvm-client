package net.ravendb.client.documents.session;

import net.ravendb.client.documents.RavenVector;

import java.util.Collection;
import java.util.Map;

public class VectorEmbeddingFieldValueFactory implements IVectorFieldValueFactory  {

    private Object embedding;
    private Number[][] embeddings;
    private String text;
    private String[] texts;
    private String byId;
    private String embeddingsGenerationTaskIdentifier;

    public String getEmbeddingsGenerationTaskIdentifier() {
        return embeddingsGenerationTaskIdentifier;
    }
    public String getById() {
        return byId;
    }
    public Object getEmbedding() {
        return embedding;
    }

    public Number[][] getEmbeddings() {
        return embeddings;
    }
    public String getText() {
        return text;
    }
    public String[] getTexts() {
        return texts;
    }


    @Override
    public <T extends Number> void byEmbedding(T[] embedding) {
        this.embedding = embedding;
    }

    @Override
    public <T extends Number> void byEmbedding(RavenVector<T> embedding) {
        this.embedding = embedding;
    }

    @Override
    @Deprecated
    public <T extends Number> void byEmbedding(Map<String, IRavenVector<T>> embedding) {
        this.embedding = embedding;
    }

    @Override
    public <T extends Number> void byEmbeddings(T[][] embeddings) {
        this.embeddings = embeddings;
    }

    @Override
    public void byBase64(String base64Embedding) {
        this.text = base64Embedding;
    }

    @Override
    public void byBase64(Collection<String> base64Embeddings) {
        this.texts = base64Embeddings != null ? base64Embeddings.toArray(new String[0]) : null;
    }

    @Override
    public <T extends Number> void byEmbedding(Collection<T> embedding) {
        this.embedding = embedding;
    }

    @Override
    public <T extends Number> void byEmbeddings(Collection<? extends Collection<T>> embeddings) {
        this.embedding = embeddings;
    }

    @Override
    public void byTexts(Collection<String> texts) {
        byTexts(texts != null ? texts.toArray(new String[0]) : null);
    }

    @Override
    public void byTexts(Collection<String> texts, String embeddingsGenerationTaskIdentifier) {
        byTexts(texts != null ? texts.toArray(new String[0]) : null, embeddingsGenerationTaskIdentifier);
    }

    @Override
    public void byText(String text) {
        this.text = text;
    }

    @Override
    public void byText(String text, String embeddingsGenerationTaskIdentifier) {
        this.text = text;
        this.embeddingsGenerationTaskIdentifier = embeddingsGenerationTaskIdentifier;
    }

    @Override
    public void byTexts(String[] texts) {
        this.texts = texts;
    }

    @Override
    public void byTexts(String[] texts, String embeddingsGenerationTaskIdentifier) {
        this.texts = texts;
        this.embeddingsGenerationTaskIdentifier = embeddingsGenerationTaskIdentifier;
    }

    @Override
    public void forDocument(String documentId) {
        this.byId = documentId;
    }
}
