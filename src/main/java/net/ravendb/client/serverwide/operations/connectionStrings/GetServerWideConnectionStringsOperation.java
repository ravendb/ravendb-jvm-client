package net.ravendb.client.serverwide.operations.connectionStrings;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.SharpEnum;
import net.ravendb.client.serverwide.ConnectionStringType;
import net.ravendb.client.serverwide.operations.IServerOperation;
import net.ravendb.client.util.UrlUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Operation to retrieve server-wide connection strings from the cluster.
 * Can retrieve all server-wide connection strings or filter by name and type.
 */
public class GetServerWideConnectionStringsOperation implements IServerOperation<GetServerWideConnectionStringsResult> {

    private final String _connectionStringName;
    private final ConnectionStringType _type;

    /**
     * Retrieves all server-wide connection strings of all types.
     */
    public GetServerWideConnectionStringsOperation() {
        _connectionStringName = null;
        _type = ConnectionStringType.NONE;
    }

    /**
     * @param connectionStringName the name of a specific connection string to retrieve
     * @param type the type of connection strings to retrieve
     */
    public GetServerWideConnectionStringsOperation(String connectionStringName, ConnectionStringType type) {
        if (StringUtils.isBlank(connectionStringName)) {
            throw new IllegalArgumentException("Connection string name must not be null or empty.");
        }

        _connectionStringName = connectionStringName;
        _type = type;
    }

    @Override
    public RavenCommand<GetServerWideConnectionStringsResult> getCommand(DocumentConventions conventions) {
        return new GetServerWideConnectionStringsCommand(_connectionStringName, _type);
    }

    private static class GetServerWideConnectionStringsCommand extends RavenCommand<GetServerWideConnectionStringsResult> {
        private final String _connectionStringName;
        private final ConnectionStringType _type;

        public GetServerWideConnectionStringsCommand(String connectionStringName, ConnectionStringType type) {
            super(GetServerWideConnectionStringsResult.class);

            _connectionStringName = connectionStringName;
            _type = type;
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/admin/configuration/server-wide/connection-strings";

            List<String> queryParams = new ArrayList<>();
            if (_connectionStringName != null) {
                queryParams.add("name=" + UrlUtils.escapeDataString(_connectionStringName));
            }
            if (_type != null && _type != ConnectionStringType.NONE) {
                queryParams.add("type=" + SharpEnum.value(_type));
            }

            if (!queryParams.isEmpty()) {
                url += "?" + String.join("&", queryParams);
            }

            return new HttpGet(url);
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
