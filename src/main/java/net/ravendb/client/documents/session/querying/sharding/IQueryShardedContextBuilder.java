package net.ravendb.client.documents.session.querying.sharding;

public interface IQueryShardedContextBuilder {

    IQueryShardedContextBuilder byDocumentId(String id);

    IQueryShardedContextBuilder byDocumentIds(String[] ids);
}
