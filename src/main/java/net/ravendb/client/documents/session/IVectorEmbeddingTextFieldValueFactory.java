package net.ravendb.client.documents.session;

import java.util.Collection;

public interface IVectorEmbeddingTextFieldValueFactory {
    /**
     * Defines queried text.
     * @param text Queried text
     */
    void byText(String text);

    /**
     * Defines queried text.
     * @param text Queried text
     * @param embeddingsGenerationTaskIdentifier The embeddings generation task identifier
     */
    void byText(String text, String embeddingsGenerationTaskIdentifier);

    /**
     * Defines queried texts.
     * @param texts Queried texts
     */
    void byTexts(String[] texts);

    /**
     * Defines queried texts.
     * @param texts Queried texts
     * @param embeddingsGenerationTaskIdentifier The embeddings generation task identifier
     */
    void byTexts(String[] texts, String embeddingsGenerationTaskIdentifier);

    void byTexts(Collection<String> texts);

    void byTexts(Collection<String> texts, String embeddingsGenerationTaskIdentifier);

    /**
     * Query by the embedding(s) indexed from the specified document for the quried field.
     * @param documentId The unique identifier of the document to be processed.
     */
    void forDocument(String documentId);
}
