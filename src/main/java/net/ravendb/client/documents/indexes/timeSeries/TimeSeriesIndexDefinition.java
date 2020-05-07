package net.ravendb.client.documents.indexes.timeSeries;

import net.ravendb.client.documents.indexes.IndexDefinition;
import net.ravendb.client.documents.indexes.IndexSourceType;

public class TimeSeriesIndexDefinition extends IndexDefinition {
    @Override
    public IndexSourceType getSourceType() {
        return IndexSourceType.TIME_SERIES;
    }
}
