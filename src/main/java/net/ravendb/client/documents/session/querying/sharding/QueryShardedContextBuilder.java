package net.ravendb.client.documents.session.querying.sharding;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public class QueryShardedContextBuilder implements IQueryShardedContextBuilder {

    public Set<String> documentIds = new TreeSet<>(String::compareToIgnoreCase);

    public Set<String> prefixes = new TreeSet<>(String::compareToIgnoreCase);

    @Override
    public IQueryShardedContextBuilder byDocumentId(String id) {
        documentIds.add(id);

        return this;
    }

    @Override
    public IQueryShardedContextBuilder byDocumentIds(String[] ids) {
        Collections.addAll(documentIds, ids);

        return this;
    }

    @Override
    public IQueryShardedContextBuilder byDocumentIds(Collection<String> ids) {
        documentIds.addAll(ids);

        return this;
    }

    @Override
    public IQueryShardedContextBuilder byPrefix(String prefix) {
        prefixes.add(prefix);

        return this;
    }

    @Override
    public IQueryShardedContextBuilder byPrefixes(String[] prefixes) {
        Collections.addAll(this.prefixes, prefixes);

        return this;
    }

    @Override
    public IQueryShardedContextBuilder byPrefixes(Collection<String> prefixes) {
        this.prefixes.addAll(prefixes);

        return this;
    }
}
