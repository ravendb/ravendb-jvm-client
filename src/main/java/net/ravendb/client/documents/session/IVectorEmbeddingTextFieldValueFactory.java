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
}
