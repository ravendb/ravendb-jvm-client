package net.ravendb.client.serverwide.operations.certificates;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.ResultsResponse;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.serverwide.operations.IServerOperation;
import net.ravendb.client.util.UrlUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class GetCertificatesMetadataOperation implements IServerOperation<CertificateMetadata[]> {

    private final String _name;

    public GetCertificatesMetadataOperation() {
        this(null);
    }

    public GetCertificatesMetadataOperation(String name) {
        this._name = name;
    }

    @Override
    public RavenCommand<CertificateMetadata[]> getCommand(DocumentConventions conventions) {
        return new GetCertificatesMetadataCommand(_name);
    }

    private static class GetCertificatesMetadataCommand extends RavenCommand<CertificateMetadata[]> {
        private final String _name;

        public GetCertificatesMetadataCommand(String name) {
            super(CertificateMetadata[].class);
            _name = name;
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            StringBuilder path = new StringBuilder(node.getUrl())
                    .append("/admin/certificates?metadataOnly=true");

            if (StringUtils.isNotEmpty(_name)) {
                path.append("&name=").append(UrlUtils.escapeDataString(_name));
            }

            url.value = path.toString();

            return new HttpGet();
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                return;
            }

            result = mapper.readValue(response, ResultsResponse.GetCertificatesMetadataResponse.class).getResults();
        }
    }
}
