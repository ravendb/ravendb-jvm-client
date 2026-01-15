package net.ravendb.client.documents.session;

import java.util.Collection;
import java.util.Map;
import net.ravendb.client.DocumentationUrls;

/**
 *  Provides client API for counter operations on a specific entity.<br/>
 *  Counters are numeric data variables that can be added to documents. <br/>
 *  They are designed to perform high frequency counting in a distributed manner, <br/>
 * while ensuring conflict-free behavior.
 * {@inheritDoc}
 * @see DocumentationUrls.Session.Counters#Overview
 */
public interface ISessionDocumentCounters extends ISessionDocumentCountersBase {

    /**
     * Get all counters for a specific document.
     * @return A Dictionary of counter values by counter name, containing all counters for this document
     */
    Map<String, Long> getAll();

    /**
     * Get counter value by counter name.
     * @param counter Name of the counter to get
     * @return The counter value if exists, or Null if the counter does not exist
     */
    Long get(String counter);

    /**
     * Get values of multiple counters of the same document
     * @param counters Names of the counters to get
     * @return A dictionary of counter values by counter names
     */
    Map<String, Long> get(Collection<String> counters);
}
