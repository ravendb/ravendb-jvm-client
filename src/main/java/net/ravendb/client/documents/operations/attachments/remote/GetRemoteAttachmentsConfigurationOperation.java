package net.ravendb.client.documents.operations.attachments.remote;

import net.ravendb.client.documents.attachments.RemoteAttachmentsConfiguration;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.io.IOException;

public final class GetRemoteAttachmentsConfigurationOperation
        implements IMaintenanceOperation<RemoteAttachmentsConfiguration> {

    @Override
    public RavenCommand<RemoteAttachmentsConfiguration> getCommand(DocumentConventions conventions) {
        return new GetRemoteAttachmentsConfigurationCommand();
    }

    private static final class GetRemoteAttachmentsConfigurationCommand
            extends RavenCommand<RemoteAttachmentsConfiguration> {

        public GetRemoteAttachmentsConfigurationCommand() {
            super(RemoteAttachmentsConfiguration.class);
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl()
                    + "/databases/" + node.getDatabase()
                    + "/admin/attachments/remote/config";

            return new HttpGet(url);
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                return;
            }

            result = mapper.readValue(response, resultClass);
        }
    }
}

