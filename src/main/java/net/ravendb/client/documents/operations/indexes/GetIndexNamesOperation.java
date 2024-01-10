package net.ravendb.client.documents.operations.indexes;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.documents.operations.ResultsResponse;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.io.IOException;

public class GetIndexNamesOperation implements IMaintenanceOperation<String[]> {

    private final int _start;
    private final int _pageSize;

    public GetIndexNamesOperation(int start, int pageSize) {
        _start = start;
        _pageSize = pageSize;
    }

    @Override
    public RavenCommand<String[]> getCommand(DocumentConventions conventions) {
        return new GetIndexNamesCommand(_start, _pageSize);
    }

    private static class GetIndexNamesCommand extends RavenCommand<String[]> {
        private final int _start;
        private final int _pageSize;

        public GetIndexNamesCommand(int start, int pageSize) {
            super(String[].class);

            _start = start;
            _pageSize = pageSize;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase()
                    + "/indexes?start=" + _start + "&pageSize=" + _pageSize + "&namesOnly=true";

            return new HttpGet(url);
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                throwInvalidResponse();
            }

            result = mapper.readValue(response, ResultsResponse.GetIndexNamesResponse.class).getResults();
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }
    }
}
