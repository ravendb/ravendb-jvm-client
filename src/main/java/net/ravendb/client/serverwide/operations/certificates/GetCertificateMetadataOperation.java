package net.ravendb.client.serverwide.operations.certificates;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.ResultsResponse;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.serverwide.operations.IServerOperation;
import net.ravendb.client.util.UrlUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.io.IOException;

public class GetCertificateMetadataOperation implements IServerOperation<CertificateMetadata> {

    private final String _thumbprint;

    public GetCertificateMetadataOperation(String thumbprint) {
        if (thumbprint == null) {
            throw new IllegalArgumentException("Thumbprint cannot be null");
        }

        _thumbprint = thumbprint;
    }

    @Override
    public RavenCommand<CertificateMetadata> getCommand(DocumentConventions conventions) {
        return new GetCertificateMetadataCommand(_thumbprint);
    }

    private static class GetCertificateMetadataCommand extends RavenCommand<CertificateMetadata> {
        private final String _thumbprint;

        public GetCertificateMetadataCommand(String thumbprint) {
            super(CertificateMetadata.class);
            _thumbprint = thumbprint;
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String path = node.getUrl() +
                    "/admin/certificates?thumbprint=" +
                    UrlUtils.escapeDataString(_thumbprint) +
                    "&metadataOnly=true";

            return new HttpGet(path);
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                return;
            }

            CertificateMetadata[] results = mapper.readValue(response, ResultsResponse.GetCertificatesMetadataResponse.class).getResults();

            if (results.length != 1) {
                throwInvalidResponse();
            }

            result = results[0];
        }
    }
}
