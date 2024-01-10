package net.ravendb.client.documents.operations.connectionStrings;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.SharpEnum;
import net.ravendb.client.util.RaftIdGenerator;
import net.ravendb.client.util.UrlUtils;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.io.IOException;

public class RemoveConnectionStringOperation<T extends ConnectionString> implements IMaintenanceOperation<RemoveConnectionStringResult> {
    private final T _connectionString;

    public RemoveConnectionStringOperation(T connectionString) {
        _connectionString = connectionString;
    }

    @Override
    public RavenCommand<RemoveConnectionStringResult> getCommand(DocumentConventions conventions) {
        return new RemoveConnectionStringCommand<>(_connectionString);
    }

    private static class RemoveConnectionStringCommand<T extends ConnectionString> extends RavenCommand<RemoveConnectionStringResult> implements IRaftCommand {
        private final T _connectionString;

        public RemoveConnectionStringCommand(T connectionString) {
            super(RemoveConnectionStringResult.class);
            _connectionString = connectionString;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/connection-strings?connectionString="
                    + UrlUtils.escapeDataString(_connectionString.getName()) + "&type=" + SharpEnum.value(_connectionString.getType());

            return new HttpDelete(url);
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                throwInvalidResponse();
            }

            result = mapper.readValue(response, resultClass);
        }
    }
}
