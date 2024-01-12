package net.ravendb.client.infrastructure;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.io.IOException;

public class GenerateCertificateOperation implements IMaintenanceOperation<GenerateCertificateOperation.PullReplicationCertificate> {

    @Override
    public RavenCommand<PullReplicationCertificate> getCommand(DocumentConventions conventions) {
        return new GenerateCertificateCommand();
    }

    public static class GenerateCertificateCommand extends RavenCommand<GenerateCertificateOperation.PullReplicationCertificate> implements IRaftCommand {
        public GenerateCertificateCommand() {
            super(GenerateCertificateOperation.PullReplicationCertificate.class);
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/pull-replication/generate-certificate";

            return new HttpPost(url);
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                throwInvalidResponse();
            }

            result = mapper.readValue(response, resultClass);
        }
    }

    public static class PullReplicationCertificate {
        private String publicKey;
        private String certificate;
        private String thumbprint;

        public String getPublicKey() {
            return publicKey;
        }

        public void setPublicKey(String publicKey) {
            this.publicKey = publicKey;
        }

        public String getCertificate() {
            return certificate;
        }

        public void setCertificate(String certificate) {
            this.certificate = certificate;
        }

        public String getThumbprint() {
            return thumbprint;
        }

        public void setThumbprint(String thumbprint) {
            this.thumbprint = thumbprint;
        }
    }
}
