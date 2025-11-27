package net.ravendb.client.documents.session;

import net.ravendb.client.documents.queries.IFilterFactory;
import net.ravendb.client.DocumentationUrls;
import java.util.function.Consumer;
/**
 * Dynamic group-by query on collection data.
 *
 * @param <T> Document type.
 * @see DocumentationUrls.Session.Querying#GroupByQuery
 */
public interface IGroupByDocumentQuery<T> {

    /**
     * Include group-by key in query projection.
     * {@inheritDoc}
     * @see DocumentationUrls.Session.Querying#GroupByQuery
     */
    IGroupByDocumentQuery<T> selectKey();

    /**
     * Include group-by key in query projection.
     * @param fieldName GroupBy field name.
     * {@inheritDoc}
     * @see DocumentationUrls.Session.Querying#GroupByQuery
     */
    IGroupByDocumentQuery<T> selectKey(String fieldName);

    /**
     * Include group-by key in query projection.
     * @param fieldName GroupBy field name.
     * @param projectedName Projection alias.
     * {@inheritDoc}
     * @see DocumentationUrls.Session.Querying#GroupByQuery
     */
    IGroupByDocumentQuery<T> selectKey(String fieldName, String projectedName);

    /**
     * Computes the sum of numeric values for a specified field.
     * @param field GroupBy field to sum.
     * @param fields Additional fields to sum.
     * {@inheritDoc}
     * @see DocumentationUrls.Session.Querying#GroupByQuery
     */
    IDocumentQuery<T> selectSum(GroupByField field, GroupByField... fields);

    /**
     * Returns the number of elements in a group
     * {@inheritDoc}
     * @see DocumentationUrls.Session.Querying#GroupByQuery
     */
    IDocumentQuery<T> selectCount();

    /**
     * Returns the number of elements in a group
     * @param projectedName Set alias for field with count value. (Default: 'Count')
     * {@inheritDoc}
     * @see DocumentationUrls.Session.Querying#GroupByQuery
     */
    IDocumentQuery<T> selectCount(String projectedName);

    /**
     * Criteria are evaluated at query time so please use Filter wisely to avoid performance issues.
     * @param builder Builder of a Filter query.
     */
    IGroupByDocumentQuery<T> filter(Consumer<IFilterFactory<T>> builder);
    /**
     * Criteria are evaluated at query time so please use Filter wisely to avoid performance issues.
     * @param builder Builder of a Filter query.
     * @param limit Limits the number of documents processed by Filter.
     */
    IGroupByDocumentQuery<T> filter(Consumer<IFilterFactory<T>> builder, int limit);
}
