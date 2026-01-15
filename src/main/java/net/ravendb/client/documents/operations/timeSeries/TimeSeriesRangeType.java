package net.ravendb.client.documents.operations.timeSeries;

import net.ravendb.client.primitives.UseSharpEnum;

/**
 * Specifies the type of time series range operation.
 */
@UseSharpEnum
public enum TimeSeriesRangeType {
    /**
     * No range type specified.
     */
    NONE,
    /**
     * Specifies a range that retrieves the last set of data points from the time series.
     */
    LAST
}
