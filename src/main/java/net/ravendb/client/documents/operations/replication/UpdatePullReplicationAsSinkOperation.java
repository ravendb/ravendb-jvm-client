package net.ravendb.client.documents.operations.replication;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.exceptions.security.AuthorizationException;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.serverwide.operations.ModifyOngoingTaskResult;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

import java.io.IOException;

public class UpdatePullReplicationAsSinkOperation implements IMaintenanceOperation<ModifyOngoingTaskResult> {
    private final PullReplicationAsSink _pullReplication;
    private final boolean _useServerCertificate;

    public UpdatePullReplicationAsSinkOperation(PullReplicationAsSink pullReplication) {
        this(pullReplication, false);
    }

    /**
     * @param pullReplication The configuration for the pull replication sink task.
     * @param useServerCertificate Makes the replication use the server certificate.
     *                             Requires {@link PullReplicationAsSink#getCertificateWithPrivateKey()} to be null.
     */
    public UpdatePullReplicationAsSinkOperation(PullReplicationAsSink pullReplication, boolean useServerCertificate) {
        _pullReplication = pullReplication;
        _useServerCertificate = useServerCertificate;

        if (pullReplication != null && pullReplication.getCertificateWithPrivateKey() != null) {
            if (useServerCertificate) {
                throw new IllegalArgumentException("When useServerCertificate is set to true, " +
                        "CertificateWithPrivateKey should be null to use server certificate.");
            }

            if (!pullReplication.hasPrivateKey()) {
                throw new AuthorizationException("Certificate with private key is required");
            }
        }
    }

    @Override
    public RavenCommand<ModifyOngoingTaskResult> getCommand(DocumentConventions conventions) {
        return new UpdatePullEdgeReplication(conventions, _pullReplication, _useServerCertificate);
    }

    private static class UpdatePullEdgeReplication extends RavenCommand<ModifyOngoingTaskResult> implements IRaftCommand {
        private final PullReplicationAsSink _pullReplication;
        private final DocumentConventions _conventions;
        private final boolean _useServerCertificate;

        public UpdatePullEdgeReplication(DocumentConventions conventions, PullReplicationAsSink pullReplication, boolean useServerCertificate) {
            super(ModifyOngoingTaskResult.class);
            if (pullReplication == null) {
                throw new IllegalArgumentException("PullReplication cannot be null");
            }
            _pullReplication = pullReplication;
            _conventions = conventions;
            _useServerCertificate = useServerCertificate;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/tasks/sink-pull-replication";

            String name = _pullReplication.getName() != null ? _pullReplication.getName() : _pullReplication.getHubName();
            _pullReplication.setAllowedHubToSinkPaths(PullReplicationPathFilterUtils.normalizeAndValidate(_pullReplication.getAllowedHubToSinkPaths(), name));
            _pullReplication.setAllowedSinkToHubPaths(PullReplicationPathFilterUtils.normalizeAndValidate(_pullReplication.getAllowedSinkToHubPaths(), name));

            HttpPost request = new HttpPost(url);
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    ObjectNode sink = mapper.valueToTree(_pullReplication);

                    // Aligned with ServerStore.UpdatePullReplicationAsSink to not introduce breaking changes
                    if (_pullReplication.getCertificateWithPrivateKey() == null && _useServerCertificate) {
                        sink.remove("CertificateWithPrivateKey");
                    }

                    generator.writeStartObject();
                    generator.writeFieldName("PullReplicationAsSink");
                    generator.writeTree(sink);
                    generator.writeEndObject();
                }
            }, ContentType.APPLICATION_JSON, _conventions));

            return request;
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                throwInvalidResponse();
            }

            result = mapper.readValue(response, ModifyOngoingTaskResult.class);
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }
    }
}
