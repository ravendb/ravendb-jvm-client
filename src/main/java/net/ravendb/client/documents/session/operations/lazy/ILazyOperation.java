package net.ravendb.client.documents.session.operations.lazy;

import net.ravendb.client.documents.commands.multiGet.GetRequest;
import net.ravendb.client.documents.commands.multiGet.GetResponse;
import net.ravendb.client.documents.queries.QueryResult;

public interface ILazyOperation {
    GetRequest createRequest();
    Object getResult();
    QueryResult getQueryResult();
    boolean isRequiresRetry();
    void handleResponse(GetResponse response);
}
