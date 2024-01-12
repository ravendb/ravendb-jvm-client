package net.ravendb.client.documents.operations.analyzers;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IVoidMaintenanceOperation;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

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
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/analyzers?name=" + urlEncode(_analyzerName);

            return new HttpDelete(url);
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }
    }
}
