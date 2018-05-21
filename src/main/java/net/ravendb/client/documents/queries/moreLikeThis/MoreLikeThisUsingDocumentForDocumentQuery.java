package net.ravendb.client.documents.queries.moreLikeThis;

import net.ravendb.client.documents.session.IDocumentQuery;
import net.ravendb.client.documents.session.IFilterDocumentQueryBase;

import java.util.function.Consumer;

public class MoreLikeThisUsingDocumentForDocumentQuery<T> extends MoreLikeThisBase {

    private Consumer<IFilterDocumentQueryBase<T, IDocumentQuery<T>>> forDocumentQuery;

    public Consumer<IFilterDocumentQueryBase<T, IDocumentQuery<T>>> getForDocumentQuery() {
        return forDocumentQuery;
    }

    public void setForDocumentQuery(Consumer<IFilterDocumentQueryBase<T, IDocumentQuery<T>>> forDocumentQuery) {
        this.forDocumentQuery = forDocumentQuery;
    }
}
