package net.ravendb.client.documents.session.operations.lazy;

import net.ravendb.client.documents.session.ResponseTimeInformation;

/**
 * Allow to perform eager operations on the session
 */
public interface IEagerSessionOperations {

    /**
     * Execute all the lazy requests pending within this session
     * @return Information about response times
     */
    @SuppressWarnings("UnusedReturnValue")
    ResponseTimeInformation executeAllPendingLazyOperations();
}
