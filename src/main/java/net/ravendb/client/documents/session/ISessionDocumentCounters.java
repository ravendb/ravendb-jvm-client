package net.ravendb.client.documents.session;

import java.util.Collection;
import java.util.Map;

/**
 *  Counters advanced synchronous session operations
 */
public interface ISessionDocumentCounters extends ISessionDocumentCountersBase {

    /**
     * @return Returns all the counters for a document.
     */
    Map<String, Long> getAll();

    /**
     * Returns the counter by the counter name.
     * @param counter Counter Name
     * @return Counter value
     */
    Long get(String counter);

    /**
     * Returns the map of counter values by counter names
     * @param counters counter names
     * @return Map of counters
     */
    Map<String, Long> get(Collection<String> counters);
}
