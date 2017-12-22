package net.ravendb.client.documents.session;

import java.util.List;

public interface IEnumerableQuery<T> {

    /**
     * Materialize query, executes request and returns with results
     * @return results as list
     */
    List<T> toList();
}
