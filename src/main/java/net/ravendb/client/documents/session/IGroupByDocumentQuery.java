package net.ravendb.client.documents.session;

import net.ravendb.client.documents.queries.IFilterFactory;

import java.util.function.Consumer;

public interface IGroupByDocumentQuery<T> {

    IGroupByDocumentQuery<T> selectKey();
    IGroupByDocumentQuery<T> selectKey(String fieldName);
    IGroupByDocumentQuery<T> selectKey(String fieldName, String projectedName);

    IDocumentQuery<T> selectSum(GroupByField field, GroupByField... fields);

    IDocumentQuery<T> selectCount();
    IDocumentQuery<T> selectCount(String projectedName);

    IGroupByDocumentQuery<T> filter(Consumer<IFilterFactory<T>> builder);
    IGroupByDocumentQuery<T> filter(Consumer<IFilterFactory<T>> builder, int limit);
}
