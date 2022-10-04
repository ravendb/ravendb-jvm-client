package net.ravendb.client.documents.session;

import java.util.function.Function;

/**
 * @deprecated Graph API will be removed in next major version of the product.
 * @param <T> Document type
 */
public interface IGraphDocumentQuery<T> extends IQueryBase<T, IGraphDocumentQuery<T>>, IDocumentQueryBaseSingle<T>, IEnumerableQuery<T> {

    /**
     * @deprecated Graph API will be removed in next major version of the product.
     * @param alias alias
     * @param query query
     * @return graph query
     * @param <TOther> Query document type
     */
    <TOther> IGraphDocumentQuery<T> with(String alias, IDocumentQuery<TOther> query);

    /**
     * @deprecated Graph API will be removed in next major version of the product.
     * @param clazz Document type
     * @param alias alias
     * @param rawQuery raw RQL query
     * @return graph query
     * @param <TOther> Query document type
     */
    <TOther> IGraphDocumentQuery<T> with(Class<TOther> clazz, String alias, String rawQuery);

    /**
     * @deprecated Graph API will be removed in next major version of the product.
     * @param alias alias
     * @param queryFactory Query builder
     * @return graph query
     * @param <TOther> Query document type
     */
    <TOther> IGraphDocumentQuery<T> with(String alias, Function<GraphDocumentQuery.DocumentQueryBuilder, IDocumentQuery<TOther>> queryFactory);

    /**
     * @deprecated Graph API will be removed in next major version of the product.
     * @param alias alias
     * @param edgeSelector edge selector
     * @param query query
     * @return graph query
     */
    IGraphDocumentQuery<T> withEdges(String alias, String edgeSelector, String query);

}
