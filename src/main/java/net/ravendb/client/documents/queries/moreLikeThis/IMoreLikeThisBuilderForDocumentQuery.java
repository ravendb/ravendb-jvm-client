package net.ravendb.client.documents.queries.moreLikeThis;

import net.ravendb.client.documents.session.IDocumentQuery;
import net.ravendb.client.documents.session.IFilterDocumentQueryBase;

import java.util.function.Consumer;

/**
 * {@inheritDoc}
 */
public interface IMoreLikeThisBuilderForDocumentQuery<T> extends IMoreLikeThisBuilderBase<T> {
    /**
     * {@inheritDoc}
     * @see IMoreLikeThisOperations
     * @param builder Filtering expression utilized to find a document that will be used as a base for operation.
     */
    IMoreLikeThisOperations<T> usingDocument(Consumer<IFilterDocumentQueryBase<T, IDocumentQuery<T>>> builder);
}
