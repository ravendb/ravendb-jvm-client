package net.ravendb.client.documents.queries.moreLikeThis;

import net.ravendb.client.DocumentationUrls;
/**
 * Get similar documents according to the provided criteria and options.
 *
 * @param <T> Queried document type.
 * @see DocumentationUrls.Session.Querying#MoreLikeThisQuery
 */
public interface IMoreLikeThisOperations<T> {
    /**
     * Add custom parameters to your MoreLikeThis query.
     *
     * @param options Configure custom options for your query. See more at: {@link MoreLikeThisOptions}
     */
    IMoreLikeThisOperations<T> withOptions(MoreLikeThisOptions options);
}
