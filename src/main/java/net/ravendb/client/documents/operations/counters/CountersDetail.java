package net.ravendb.client.documents.operations.counters;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the result of executing a {@link GetCountersOperation} or {@link CounterBatchOperation},
 * containing details of counters associated with a document.
 */
public class CountersDetail {

    /**
     * Gets or sets the list of counter details retrieved by the operation.
     * Each {@link CounterDetail} in the list provides information about a specific counter,
     * including its name and value.
     *
     * <p>This property contains the results of either:</p>
     * <ul>
     *     <li>{@link GetCountersOperation}: Retrieves details of counters for a document.</li>
     *     <li>{@link CounterBatchOperation}: Returns details of counters affected by a batch operation.</li>
     * </ul>
     */
    private List<CounterDetail> counters;

    public CountersDetail() {
        counters = new ArrayList<>();
    }

    public List<CounterDetail> getCounters() {
        return counters;
    }

    public void setCounters(List<CounterDetail> counters) {
        this.counters = counters;
    }
}
