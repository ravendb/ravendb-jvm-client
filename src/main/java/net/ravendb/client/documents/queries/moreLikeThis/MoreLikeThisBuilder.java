package net.ravendb.client.documents.queries.moreLikeThis;

import net.ravendb.client.documents.session.IDocumentQuery;
import net.ravendb.client.documents.session.IFilterDocumentQueryBase;

import java.util.function.Consumer;

public class MoreLikeThisBuilder<T> implements IMoreLikeThisOperations<T>, IMoreLikeThisBuilderForDocumentQuery<T>, IMoreLikeThisBuilderBase<T> {

    private MoreLikeThisBase moreLikeThis;


    public MoreLikeThisBase getMoreLikeThis() {
        return moreLikeThis;
    }

    @Override
    public IMoreLikeThisOperations<T> usingAnyDocument() {
        moreLikeThis = new MoreLikeThisUsingAnyDocument();

        return this;
    }

    @Override
    public IMoreLikeThisOperations<T> usingDocument(String documentJson) {
        moreLikeThis = new MoreLikeThisUsingDocument(documentJson);

        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public IMoreLikeThisOperations<T> usingDocument(Consumer<IFilterDocumentQueryBase<T, IDocumentQuery<T>>> builder) {
        moreLikeThis = new MoreLikeThisUsingDocumentForDocumentQuery();
        ((MoreLikeThisUsingDocumentForDocumentQuery) moreLikeThis).setForDocumentQuery(builder);

        return this;
    }

    @Override
    public IMoreLikeThisOperations<T> withOptions(MoreLikeThisOptions options) {
        moreLikeThis.setOptions(options);

        return this;
    }
}
