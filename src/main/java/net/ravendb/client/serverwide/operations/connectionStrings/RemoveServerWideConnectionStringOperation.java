package net.ravendb.client.serverwide.operations.connectionStrings;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.connectionStrings.ConnectionString;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.SharpEnum;
import net.ravendb.client.serverwide.operations.IServerOperation;
import net.ravendb.client.util.RaftIdGenerator;
import net.ravendb.client.util.UrlUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.io.IOException;

/**
 * Operation to remove a server-wide connection string from the cluster. The connection string will also be
 * removed from all database records that received it. The operation will fail if the connection string is
 * currently in use by any ongoing task.
 *
 * @param <T> the type of the connection string to remove (e.g. {@code RavenConnectionString},
 *            {@code SqlConnectionString})
 */
public class RemoveServerWideConnectionStringOperation<T extends ConnectionString> implements IServerOperation<RemoveServerWideConnectionStringResult> {

    private final T _connectionString;

    /**
     * @param connectionString the connection string to remove. Only the
     *                         {@link ConnectionString#getName()} property is required.
     */
    public RemoveServerWideConnectionStringOperation(T connectionString) {
        if (connectionString == null) {
            throw new IllegalArgumentException("connectionString cannot be null");
        }

        if (StringUtils.isBlank(connectionString.getName())) {
            throw new IllegalArgumentException("Connection string name must not be null or empty.");
        }

        _connectionString = connectionString;
    }

    @Override
    public RavenCommand<RemoveServerWideConnectionStringResult> getCommand(DocumentConventions conventions) {
        return new RemoveServerWideConnectionStringCommand<>(_connectionString);
    }

    private static class RemoveServerWideConnectionStringCommand<T extends ConnectionString> extends RavenCommand<RemoveServerWideConnectionStringResult> implements IRaftCommand {
        private final T _connectionString;

        public RemoveServerWideConnectionStringCommand(T connectionString) {
            super(RemoveServerWideConnectionStringResult.class);

            _connectionString = connectionString;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/admin/configuration/server-wide/connection-strings"
                    + "?name=" + UrlUtils.escapeDataString(_connectionString.getName())
                    + "&type=" + SharpEnum.value(_connectionString.getType());

            return new HttpDelete(url);
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
