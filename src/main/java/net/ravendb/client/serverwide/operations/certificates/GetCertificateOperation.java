package net.ravendb.client.serverwide.operations.certificates;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.serverwide.operations.IServerOperation;
import net.ravendb.client.util.UrlUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class GetCertificateOperation implements IServerOperation<CertificateDefinition> {

    private final String _thumbprint;

    public GetCertificateOperation(String thumbprint) {
        if (thumbprint == null) {
            throw new IllegalArgumentException("Thumbprint cannot be null");
        }
        _thumbprint = thumbprint;
    }

    @Override
    public RavenCommand<CertificateDefinition> getCommand(DocumentConventions conventions) {
        return new GetCertificateCommand(_thumbprint);
    }

    private static class GetCertificateCommand extends RavenCommand<CertificateDefinition> {
        private final String _thumbprint;

        public GetCertificateCommand(String thumbprint) {
            super(CertificateDefinition.class);

            if (thumbprint == null) {
                throw new IllegalArgumentException("Thumbprint cannot be null");
            }

            _thumbprint = thumbprint;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/admin/certificates?thumbprint=" + UrlUtils.escapeDataString(_thumbprint);

            return new HttpGet();
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                return;
            }

            GetCertificatesResponse certificates = mapper.readValue(response, GetCertificatesResponse.class);
            if (certificates.getResults().size() != 1) {
                throwInvalidResponse();
            }

            result = certificates.getResults().get(0);
        }
    }

}
