package net.ravendb.client.documents.session.querying.sharding;

import java.util.Collection;

public interface IQueryShardedContextBuilder {

    /**
     * Restricts the query execution to the specific shard where the document with the given ID resides.
     * This method ensures that the query is directed only to the shard that holds the document,
     * optimizing query performance by eliminating the need to query all shards.
     * @param id The ID of the document used to determine the shard to query
     * @return builder
     */
    IQueryShardedContextBuilder byDocumentId(String id);

    /**
     * Restricts the query execution to the specific shards where the documents with the given IDs reside.
     * This method ensures that the query is directed only to the shards that hold the specified documents,
     * optimizing query performance by avoiding unnecessary queries to other shards.
     * @param ids A set of document IDs used to determine the shards to query
     * @return builder
     */
    IQueryShardedContextBuilder byDocumentIds(String[] ids);

    /**
     * Restricts the query execution to the specific shards where the documents with the given IDs reside.
     * This method ensures that the query is directed only to the shards that hold the specified documents,
     * optimizing query performance by avoiding unnecessary queries to other shards.
     * @param ids A set of document IDs used to determine the shards to query
     * @return builder
     */
    IQueryShardedContextBuilder byDocumentIds(Collection<String> ids);

    /**
     * Restricts the query execution to only the shards associated with the specified prefix, as defined in the
     * database's PrefixedShardingSetting.
     * This allows the query to be executed on a subset of shards, avoiding unnecessary queries to shards not relevant
     * to the specified prefix.
     * @param prefix The document ID prefix used to determine which shards to query.
     *               This prefix corresponds to the 'PrefixSetting' configured in the database record,
     *               and only the shards associated with this prefix will be included in the query execution.
     * @return builder
     */
    IQueryShardedContextBuilder byPrefix(String prefix);

    /**
     * Restricts the query execution to only the shards associated with the specified prefixes, as defined in the
     * database's PrefixedShardingSetting.
     * This allows the query to be executed on a subset of shards, avoiding unnecessary queries to shards not relevant
     * to the specified prefixes.
     * @param prefixes A collection of document ID prefixes used to determine which shards to query.
     *                 Each prefix corresponds to a 'PrefixSetting' configured in the database record, and only the
     *                 shards associated with these prefixes will be included in the query execution.
     * @return builder
     */
    IQueryShardedContextBuilder byPrefixes(String[] prefixes);

    /**
     * Restricts the query execution to only the shards associated with the specified prefixes, as defined in the
     * database's PrefixedShardingSetting.
     * This allows the query to be executed on a subset of shards, avoiding unnecessary queries to shards not relevant
     * to the specified prefixes.
     * @param prefixes A collection of document ID prefixes used to determine which shards to query.
     *                 Each prefix corresponds to a 'PrefixSetting' configured in the database record, and only the
     *                 shards associated with these prefixes will be included in the query execution.
     * @return builder
     */
    IQueryShardedContextBuilder byPrefixes(Collection<String> prefixes);
}
