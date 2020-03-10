package net.ravendb.client.serverwide.operations.certificates;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.serverwide.operations.IVoidServerOperation;
import net.ravendb.client.util.RaftIdGenerator;
import net.ravendb.client.util.UrlUtils;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpRequestBase;

public class DeleteCertificateOperation implements IVoidServerOperation {

    private final String _thumbprint;

    public DeleteCertificateOperation(String thumbprint) {
        if (thumbprint == null) {
            throw new IllegalArgumentException("Thumbprint cannot be null");
        }

        _thumbprint = thumbprint;
    }

    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new DeleteCertificateCommand(_thumbprint);
    }

    private static class DeleteCertificateCommand extends VoidRavenCommand implements IRaftCommand {
        private final String _thumbprint;

        public DeleteCertificateCommand(String thumbprint) {
            if (thumbprint == null) {
                throw new IllegalArgumentException("Thumbprint cannot be null");
            }
            _thumbprint = thumbprint;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/admin/certificates?thumbprint=" + UrlUtils.escapeDataString(_thumbprint);

            return new HttpDelete();
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }
    }
}
