package net.ravendb.client.documents.queries.moreLikeThis;

import net.ravendb.client.documents.session.IDocumentQuery;
import net.ravendb.client.documents.session.IFilterDocumentQueryBase;

import java.util.function.Consumer;

/**
 * {@inheritDoc}
 */
public class MoreLikeThisUsingDocumentForDocumentQuery<T> extends MoreLikeThisBase {
    /**
     * Specify a document for a MoreLikeThis query using {@link IDocumentQuery} operations.
     */
    private Consumer<IFilterDocumentQueryBase<T, IDocumentQuery<T>>> forDocumentQuery;

    //TODO: Add Action<IFilterDocumentQueryBase<T, IAsyncDocumentQuery<T>>> forAsyncDocumentQuery field

    public Consumer<IFilterDocumentQueryBase<T, IDocumentQuery<T>>> getForDocumentQuery() {
        return forDocumentQuery;
    }

    public void setForDocumentQuery(Consumer<IFilterDocumentQueryBase<T, IDocumentQuery<T>>> forDocumentQuery) {
        this.forDocumentQuery = forDocumentQuery;
    }
}
