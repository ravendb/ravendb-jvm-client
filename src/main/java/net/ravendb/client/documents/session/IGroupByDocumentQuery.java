package net.ravendb.client.documents.session;

public interface IGroupByDocumentQuery<T> {

    IGroupByDocumentQuery<T> selectKey();
    IGroupByDocumentQuery<T> selectKey(String fieldName);
    IGroupByDocumentQuery<T> selectKey(String fieldName, String projectedName);

    IDocumentQuery<T> selectSum(GroupByField field, GroupByField... fields);

    IDocumentQuery<T> selectCount();
    IDocumentQuery<T> selectCount(String projectedName);
}
