package net.ravendb.client.documents.queries.moreLikeThis;

/**
 * @{inheritDoc}
 * @see IMoreLikeThisOperations
 */
public interface IMoreLikeThisBuilderBase<T> {
    IMoreLikeThisOperations<T> usingAnyDocument();

    /**
     * @{inheritDoc}
     * @see IMoreLikeThisOperations
     * @param documentJson Inline JSON document that will be used as a base for operation.
     */
    IMoreLikeThisOperations<T> usingDocument(String documentJson);
}
