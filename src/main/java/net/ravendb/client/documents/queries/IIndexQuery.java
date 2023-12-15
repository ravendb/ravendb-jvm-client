package net.ravendb.client.documents.queries;

import java.time.Duration;

public interface IIndexQuery {

    Duration getWaitForNonStaleResultsTimeout();
}
