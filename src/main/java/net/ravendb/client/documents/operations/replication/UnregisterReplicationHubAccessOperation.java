package net.ravendb.client.documents.operations.replication;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IVoidMaintenanceOperation;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.util.RaftIdGenerator;
import net.ravendb.client.util.UrlUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

public class UnregisterReplicationHubAccessOperation implements IVoidMaintenanceOperation {
    private final String _hubName;
    private final String _thumbprint;

    public UnregisterReplicationHubAccessOperation(String hubName, String thumbprint) {
        if (StringUtils.isBlank(hubName)) {
            throw new IllegalArgumentException("HubName cannot be null or whitespace.");
        }
        if (StringUtils.isBlank(thumbprint)) {
            throw new IllegalArgumentException("Thumbprint cannot be null or whitespace.");
        }

        _hubName = hubName;
        _thumbprint = thumbprint;
    }

    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new UnregisterReplicationHubAccessCommand(_hubName, _thumbprint);
    }

    private static class UnregisterReplicationHubAccessCommand extends VoidRavenCommand implements IRaftCommand {
        private final String _hubName;
        private final String _thumbprint;

        public UnregisterReplicationHubAccessCommand(String hubName, String thumbprint) {
            _hubName = hubName;
            _thumbprint = thumbprint;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/tasks/pull-replication/hub/access?name="
                    + urlEncode(_hubName) + "&thumbprint=" + UrlUtils.escapeDataString(_thumbprint);

            return new HttpDelete(url);
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }
    }
}
