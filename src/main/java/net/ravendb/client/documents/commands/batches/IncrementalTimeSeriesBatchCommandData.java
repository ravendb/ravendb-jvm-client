package net.ravendb.client.documents.commands.batches;

import net.ravendb.client.documents.operations.timeSeries.TimeSeriesOperation;

import java.util.List;

public class IncrementalTimeSeriesBatchCommandData extends TimeSeriesCommandData {
    public IncrementalTimeSeriesBatchCommandData(String documentId, String name, List<TimeSeriesOperation.IncrementOperation> increments) {
        super(documentId, name);

        if (increments != null) {
            for (TimeSeriesOperation.IncrementOperation incrementOperation : increments) {
                getTimeSeries().increment(incrementOperation);
            }
        }
    }

    @Override
    public CommandType getType() {
        return CommandType.TIME_SERIES_WITH_INCREMENTS;
    }
}
