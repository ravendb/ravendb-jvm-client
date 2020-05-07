package net.ravendb.client.documents.indexes.counters;

import net.ravendb.client.documents.indexes.IndexDefinition;
import net.ravendb.client.documents.indexes.IndexSourceType;

public class CountersIndexDefinition extends IndexDefinition {
    @Override
    public IndexSourceType getSourceType() {
        return IndexSourceType.COUNTERS;
    }
}
