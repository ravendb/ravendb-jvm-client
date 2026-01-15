package net.ravendb.client.documents.session.loaders;
import net.ravendb.client.DocumentationUrls;
/**
 * The server is instructed to pre-load referenced documents concurrently with retrieving the time series data.
 * The documents are added to the session unit of work, and subsequent requests to load them are served directly from the session cache,
 * without requiring any additional queries to the server.
 * <p>Note:</p>
 * <ul>
 *   <li>If a document ID appears in the results multiple times, the document data will be fetched only once and cached.</li>
 *   <li>In order to allow for cache usage, make sure NoTracking is set to 'false'.</li>
 * </ul>
 * {@inheritDoc}
 * @see DocumentationUrls.Session.TimeSeries#Include
 */
public interface ITimeSeriesIncludeBuilder {
    /**
     * {@inheritDoc}
     * @see ITimeSeriesIncludeBuilder
     */
    ITimeSeriesIncludeBuilder includeTags();
    /**
     * {@inheritDoc}
     * @see ITimeSeriesIncludeBuilder
     */
    ITimeSeriesIncludeBuilder includeDocument();
}