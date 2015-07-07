package net.ravendb.client.metrics;

import net.ravendb.client.ConventionBase;

public interface IRequestTimeMetric {
    void update(long requestTimeInMilliseconds);

    boolean rateSurpassed(ConventionBase conventions);

    double rate();
}
