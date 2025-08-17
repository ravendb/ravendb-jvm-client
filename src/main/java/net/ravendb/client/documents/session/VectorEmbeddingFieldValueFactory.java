package net.ravendb.client.documents.session;

import java.util.Map;

public class VectorEmbeddingFieldValueFactory implements IVectorFieldValueFactory  {

    private Object embedding;
    private Integer[][] embeddings;
    private String text;
    private  String[] texts;

    public Object getEmbedding() {
        return embedding;
    }
    public Integer[][] getEmbeddings() {
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
        this.embedding = (Integer[])embedding;
    }

    @Override
    public <T extends Number> void byEmbedding(Map<String, IRavenVector<T>> embedding) {
        this.embedding = embedding;
    }

    @Override
    public <T extends Number> void byEmbeddings(T[][] embeddings) {
        this.embeddings = (Integer[][])embeddings;
    }

    @Override
    public void byBase64(String base64Embedding) {
        this.text = base64Embedding;
    }

    @Override
    public void byText(String text) {
        this.text = text;
    }

    @Override
    public void byTexts(String[] texts) {
        this.texts = texts;
    }
}
