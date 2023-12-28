package net.ravendb.client.documents.session.querying.sharding;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public class QueryShardedContextBuilder implements IQueryShardedContextBuilder {

    public Set<String> documentIds = new TreeSet<>(String::compareToIgnoreCase);

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
}
