package net.ravendb.client.documents.queries.moreLikeThis;

import net.ravendb.client.documents.session.IDocumentQuery;
import net.ravendb.client.documents.session.IFilterDocumentQueryBase;

import java.util.function.Consumer;

public interface IMoreLikeThisBuilderForDocumentQuery<T> extends IMoreLikeThisBuilderBase<T> {
    IMoreLikeThisOperations<T> usingDocument(Consumer<IFilterDocumentQueryBase<T, IDocumentQuery<T>>> builder);
}
