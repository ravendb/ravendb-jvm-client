package net.ravendb.client.documents.operations.analyzers;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IVoidMaintenanceOperation;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpRequestBase;

public class DeleteAnalyzerOperation implements IVoidMaintenanceOperation {
    private final String _analyzerName;

    public DeleteAnalyzerOperation(String analyzerName) {
        if (analyzerName == null) {
            throw new IllegalArgumentException("AnalyzerName cannot be null");
        }

        _analyzerName = analyzerName;
    }

    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new DeleteAnalyzerCommand(_analyzerName);
    }

    private static class DeleteAnalyzerCommand extends VoidRavenCommand implements IRaftCommand {
        private final String _analyzerName;

        public DeleteAnalyzerCommand(String analyzerName) {
            if (analyzerName == null) {
                throw new IllegalArgumentException("AnalyzerName cannot be null");
            }

            _analyzerName = analyzerName;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/analyzers?name=" + urlEncode(_analyzerName);

            return new HttpDelete();
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }
    }
}
