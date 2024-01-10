package net.ravendb.client.serverwide.operations.analyzers;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.serverwide.operations.IVoidServerOperation;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

public class DeleteServerWideAnalyzerOperation implements IVoidServerOperation {
    private final String _analyzerName;

    public DeleteServerWideAnalyzerOperation(String analyzerName) {
        if (analyzerName == null) {
            throw new IllegalArgumentException("AnalyzerName cannot be null");
        }

        _analyzerName = analyzerName;
    }

    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new DeleteServerWideAnalyzerCommand(_analyzerName);
    }

    private static class DeleteServerWideAnalyzerCommand extends VoidRavenCommand implements IRaftCommand {
        private final String _analyzerName;

        public DeleteServerWideAnalyzerCommand(String analyzerName) {
            if (analyzerName == null) {
                throw new IllegalArgumentException("AnalyzerName cannot be null");
            }

            _analyzerName = analyzerName;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/admin/analyzers?name=" + urlEncode(_analyzerName);

            return new HttpDelete(url);
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }
    }
}
