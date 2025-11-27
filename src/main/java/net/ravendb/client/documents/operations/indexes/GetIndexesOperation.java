package net.ravendb.client.documents.operations.indexes;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.indexes.IndexDefinition;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.documents.operations.ResultsResponse;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.util.UrlUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.io.IOException;

public class GetIndexesOperation implements IMaintenanceOperation<IndexDefinition[]> {

    private final int _start;
    private final int _pageSize;

    public GetIndexesOperation(int start, int pageSize) {
        _start = start;
        _pageSize = pageSize;
    }

    @Override
    public RavenCommand<IndexDefinition[]> getCommand(DocumentConventions conventions) {
        return new GetIndexesCommand(_start, _pageSize);
    }

    private static class GetIndexesCommand extends RavenCommand<IndexDefinition[]> {
        private final int _start;
        private final int _pageSize;
        private final String[] indexNames;

        public GetIndexesCommand(int start, int pageSize, String[] indexNames, String nodeTag) {
            super(IndexDefinition[].class);
            _start = start;
            _pageSize = pageSize;
            this.indexNames = indexNames;
            this.selectedNodeTag = nodeTag;
        }

        public GetIndexesCommand(String indexName, String nodeTag) {
            this(0,0,new String[] {indexName}, nodeTag);
        }

        public GetIndexesCommand(int start, int pageSize) {
            this(start,pageSize, null,null);
        }

        public GetIndexesCommand(String[] indexNames, String nodeTag) {
            this(0,0,indexNames, nodeTag);
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/indexes?";

            if (this.indexNames != null && this.indexNames.length > 0) {
                for (int i = 0; i < this.indexNames.length; i++) {
                    String indexName = this.indexNames[i];
                    if (i > 0) {
                        url += "&";
                    }

                    url += "name=" + UrlUtils.escapeDataString(indexName);
                }
            }
            else
                url += "start=" + _start + "&pageSize=" + _pageSize;

            return new HttpGet(url);
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                throwInvalidResponse();
            }

            result = mapper.readValue(response, ResultsResponse.GetIndexesResponse.class).getResults();
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }
    }
}

