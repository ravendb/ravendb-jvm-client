package net.ravendb.client.documents.session.loaders;

import java.util.Date;

public interface IGenericTimeSeriesIncludeBuilder<TBuilder> extends IAbstractTimeSeriesIncludeBuilder<TBuilder> {
    /**
     * {@inheritDoc}
     * @see #includeTimeSeries(String, Date, Date)
     */
    TBuilder includeTimeSeries(String name);
    /**
     * {@inheritDoc}
     *
     * @param name The name of the time series to include.
     * @param from The date and time from which to start including time series entries (inclusive).
     *             If {@code null}, the collection will start from the earliest possible date and time
     *             ({@link java.time.Instant#MIN} equivalent in RavenDB).
     * @param to   The date and time at which to stop including time series entries (inclusive).
     *             If {@code null}, the collection will continue until the latest possible date and time
     *             ({@link java.time.Instant#MAX} equivalent in RavenDB).
     */
    TBuilder includeTimeSeries(String name, Date from, Date to);
}
