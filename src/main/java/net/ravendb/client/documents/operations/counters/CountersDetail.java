package net.ravendb.client.documents.operations.counters;

import java.util.ArrayList;
import java.util.List;

public class CountersDetail {

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
