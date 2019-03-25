package net.ravendb.client.serverwide.operations.logs;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.serverwide.operations.IVoidServerOperation;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;

public class SetLogsConfigurationOperation implements IVoidServerOperation {

    private final Parameters _parameters;

    public SetLogsConfigurationOperation(Parameters parameters) {
        if (parameters == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }

        _parameters = parameters;
    }

    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new SetLogsConfigurationCommand(_parameters);
    }

    private static class SetLogsConfigurationCommand extends VoidRavenCommand {
        private final Parameters _parameters;

        public SetLogsConfigurationCommand(Parameters parameters) {
            if (parameters == null) {
                throw new IllegalArgumentException("Parameters cannot be null");
            }

            _parameters = parameters;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/admin/logs/configuration";

            HttpPost request = new HttpPost();
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = mapper.getFactory().createGenerator(outputStream)) {
                    ObjectNode config = mapper.valueToTree(_parameters);
                    generator.writeTree(config);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, ContentType.APPLICATION_JSON));
            return request;
        }
    }

    public static class Parameters {
        private LogMode mode;

        public LogMode getMode() {
            return mode;
        }

        public void setMode(LogMode mode) {
            this.mode = mode;
        }
    }
}
