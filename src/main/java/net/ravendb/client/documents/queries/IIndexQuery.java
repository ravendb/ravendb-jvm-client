package net.ravendb.client.documents.queries;

import java.time.Duration;

public interface IIndexQuery {

    int getPageSize();

    void setPageSize(int pageSize);

    Duration getWaitForNonStaleResultsTimeout();
}
