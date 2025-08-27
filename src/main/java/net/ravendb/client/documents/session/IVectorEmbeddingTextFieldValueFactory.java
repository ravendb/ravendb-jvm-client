package net.ravendb.client.documents.session;

public interface IVectorEmbeddingTextFieldValueFactory {
    /**
     * Defines queried text.
     * @param text Queried text
     */
    void byText(String text);

    /**
     * Defines queried texts.
     * @param texts Queried texts
     */
    void byTexts(String[] texts);

    /**
     * Query by the embedding(s) indexed from the specified document for the quried field.
     * @param documentId The unique identifier of the document to be processed.
     */
    void forDocument(String documentId);
}
