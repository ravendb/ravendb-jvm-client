package net.ravendb.client.serverwide.operations.configuration;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.serverwide.operations.IVoidServerOperation;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpRequestBase;

public class DeleteServerWideBackupConfigurationOperation implements IVoidServerOperation {

    private final String _name;

    public DeleteServerWideBackupConfigurationOperation(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        _name = name;
    }

    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new DeleteServerWideBackupConfigurationCommand(_name);
    }

    private static class DeleteServerWideBackupConfigurationCommand extends VoidRavenCommand implements IRaftCommand {
        private final String _name;

        public DeleteServerWideBackupConfigurationCommand(String name) {
            if (name == null) {
                throw new IllegalArgumentException("Name cannot be null");
            }

            _name = name;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/admin/configuration/server-wide/backup?name=" + urlEncode(_name);

            return new HttpDelete();
        }
    }
}
