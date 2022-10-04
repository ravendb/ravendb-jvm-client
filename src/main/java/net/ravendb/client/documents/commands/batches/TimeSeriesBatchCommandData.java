package net.ravendb.client.documents.commands.batches;

import net.ravendb.client.documents.operations.timeSeries.TimeSeriesOperation;

import java.util.List;

public class TimeSeriesBatchCommandData extends TimeSeriesCommandData {
    public TimeSeriesBatchCommandData(String documentId, String name,
                                      List<TimeSeriesOperation.AppendOperation> appends,
                                      List<TimeSeriesOperation.DeleteOperation> deletes) {
        super(documentId, name);

        if (appends != null) {
            for (TimeSeriesOperation.AppendOperation appendOperation : appends) {
                getTimeSeries().append(appendOperation);
            }
        }

        if (deletes != null) {
            for (TimeSeriesOperation.DeleteOperation deleteOperation : deletes) {
                getTimeSeries().delete(deleteOperation);
            }
        }
    }

    @Override
    public CommandType getType() {
        return CommandType.TIME_SERIES;
    }
}
