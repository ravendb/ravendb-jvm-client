package net.ravendb.client.documents.operations.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IAdminOperation;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.serverwide.ClientConfiguration;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;
import java.io.InputStream;

public class GetClientConfigurationOperation implements IAdminOperation<GetClientConfigurationOperation.Result> {
    @Override
    public RavenCommand<Result> getCommand(DocumentConventions conventions, ObjectMapper context) {
        return new GetClientConfigurationCommand();
    }

    public static class GetClientConfigurationCommand extends RavenCommand<GetClientConfigurationOperation.Result> {
        public GetClientConfigurationCommand() {
            super(GetClientConfigurationOperation.Result.class);
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpRequestBase createRequest(ObjectMapper ctx, ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/configuration/client";

            return new HttpGet();
        }

        @Override
        public void setResponse(ObjectMapper ctx, InputStream response, boolean fromCache) throws IOException {
            if (response == null) {
                return;
            }
            result = ctx.readValue(response, resultClass);
        }
    }

    public static class Result {
        private long etag; //TODO: case might be wrong...
        private ClientConfiguration configuration;

        public long getEtag() {
            return etag;
        }

        public void setEtag(long etag) {
            this.etag = etag;
        }

        public ClientConfiguration getConfiguration() {
            return configuration;
        }

        public void setConfiguration(ClientConfiguration configuration) {
            this.configuration = configuration;
        }
    }
}
