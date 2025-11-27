package net.ravendb.client.documents.session.loaders;

import net.ravendb.client.DocumentationUrls;
/**
 * The server is instructed to pre-load referenced documents concurrently with retrieving the documents.
 * The documents are added to the session unit of work, and subsequent requests to load them are served directly from the session cache,
 * without requiring any additional queries to the server.
 * The server can then be instructed to pre-load the referenced object at the same time that the root object is retrieved, for example using:
 *
 * <pre>
 * Order order = session.include(Order.class, x -> x.getCustomerId()).load("orders/1-A");
 * // this will not require querying the server:
 * Customer customer = session.load(Customer.class, order.getCustomerId());
 * </pre>
 *
 * @see DocumentationUrls.Session.Querying#Includes
 */
public interface IDocumentIncludeBuilder<TBuilder> {
    /**
     * {@inheritDoc}529229
     *
     * @param path Name of the property which contains ID(s) of document(s) to include from the queried document.
     * @see DocumentationUrls.Session.Querying#Includes
     */
    TBuilder includeDocuments(String path);

    //TBD expr TBuilder IncludeDocuments(Expression<Func<T, string>> path);
    //TBD expr TBuilder IncludeDocuments(Expression<Func<T, IEnumerable<string>>> path);
    //TBD expr TBuilder IncludeDocuments<TInclude>(Expression<Func<T, string>> path);
    //TBD expr TBuilder IncludeDocuments<TInclude>(Expression<Func<T, IEnumerable<string>>> path);
}