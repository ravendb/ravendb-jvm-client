package net.ravendb.client.documents.session;

import net.ravendb.client.documents.queries.QueryData;

import java.util.function.Function;

public interface IGraphDocumentQuery<T> extends IQueryBase<T, IGraphDocumentQuery<T>>, IDocumentQueryBaseSingle<T>, IEnumerableQuery<T> {

    <TOther> IGraphDocumentQuery<T> with(String alias, IDocumentQuery<TOther> query);

    <TOther> IGraphDocumentQuery<T> with(Class<TOther> clazz, String alias, String rawQuery);

    <TOther> IGraphDocumentQuery<T> with(String alias, Function<GraphDocumentQuery.DocumentQueryBuilder, IDocumentQuery<TOther>> queryFactory);

    IGraphDocumentQuery<T> withEdges(String alias, String edgeSelector, String query);

}
