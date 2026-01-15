package net.ravendb.client.documents.session.loaders;

import java.util.Date;

public interface IQueryIncludeBuilder extends IGenericIncludeBuilder<IQueryIncludeBuilder> {
    /**
     * {@inheritDoc}
     *
     * @see IIncludeBuilder
     */
    IQueryIncludeBuilder includeCounter(String path, String name);
    /**
     * {@inheritDoc}
     *
     * @see IIncludeBuilder
     */
    IQueryIncludeBuilder includeCounters(String path, String[] names);
    /**
     * {@inheritDoc}
     *
     * @see IIncludeBuilder
     */
    IQueryIncludeBuilder includeAllCounters(String path);
    /**
     * {@inheritDoc}
     *
     * @see IIncludeBuilder
     */
    IQueryIncludeBuilder includeTimeSeries(String path, String name);
    /**
     * {@inheritDoc}
     *
     * @see IIncludeBuilder
     */
    IQueryIncludeBuilder includeTimeSeries(String path, String name, Date from, Date to);

    //TODO: implement expression api, should remove string api?
}
