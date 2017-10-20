package net.ravendb.client.serverwide.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class GetDatabaseNamesOperation implements IServerOperation<String[]> {
    private final int _start;
    private final int _pageSize;

    public GetDatabaseNamesOperation(int _start, int _pageSize) {
        this._start = _start;
        this._pageSize = _pageSize;
    }

    @Override
    public RavenCommand<String[]> getCommand(DocumentConventions conventions) {
        return new GetDatabaseNamesCommand(_start, _pageSize);
    }

    private static class GetDatabaseNamesCommand extends RavenCommand<String[]> {
        private final int _start;
        private final int _pageSize;

        public GetDatabaseNamesCommand(int _start, int _pageSize) {
            super(String[].class);
            this._start = _start;
            this._pageSize = _pageSize;
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases?start=" + _start + "&pageSize=" + _pageSize + "&namesOnly=true";

            return new HttpGet();
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                throwInvalidResponse();
                return;
            }

            JsonNode names = mapper.readTree(response);
            if (!names.has("Databases")) {
                throwInvalidResponse();
            }

            JsonNode databases = names.get("Databases");
            if (!databases.isArray()) {
                throwInvalidResponse();
            }
            ArrayNode dbNames = (ArrayNode) databases;
            String[] databaseNames = new String[dbNames.size()];
            for (int i = 0; i < dbNames.size(); i++) {
                databaseNames[i] = dbNames.get(i).asText();
            }

            result = databaseNames;

        }
    }
}
