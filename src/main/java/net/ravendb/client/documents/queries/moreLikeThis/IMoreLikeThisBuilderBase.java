package net.ravendb.client.documents.queries.moreLikeThis;

public interface IMoreLikeThisBuilderBase<T> {
    IMoreLikeThisOperations<T> usingAnyDocument();

    IMoreLikeThisOperations<T> usingDocument(String documentJson);
}
