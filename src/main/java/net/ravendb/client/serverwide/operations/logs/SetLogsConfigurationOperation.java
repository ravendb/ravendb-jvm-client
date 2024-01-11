package net.ravendb.client.serverwide.operations.logs;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.serverwide.operations.IVoidServerOperation;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

import java.io.IOException;
import java.time.Duration;

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
        return new SetLogsConfigurationCommand(conventions, _parameters);
    }

    private static class SetLogsConfigurationCommand extends VoidRavenCommand {
        private final DocumentConventions _conventions;
        private final Parameters _parameters;

        public SetLogsConfigurationCommand(DocumentConventions conventions, Parameters parameters) {
            if (parameters == null) {
                throw new IllegalArgumentException("Parameters cannot be null");
            }

            _parameters = parameters;
            _conventions = conventions;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/admin/logs/configuration";

            HttpPost request = new HttpPost(url);
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    ObjectNode config = mapper.valueToTree(_parameters);
                    generator.writeTree(config);
                }
            }, ContentType.APPLICATION_JSON, _conventions));
            return request;
        }
    }

    public static class Parameters {
        private LogMode mode;
        private Duration retentionTime;
        private Long retentionSize;
        private boolean compress;

        public Parameters(GetLogsConfigurationResult getLogs) {
            mode = getLogs.getMode();
            retentionTime = getLogs.getRetentionTime();
            retentionSize = getLogs.getRetentionSize();
            compress = getLogs.isCompress();
        }

        public Parameters() {
        }

        public Duration getRetentionTime() {
            return retentionTime;
        }

        public void setRetentionTime(Duration retentionTime) {
            this.retentionTime = retentionTime;
        }

        public Long getRetentionSize() {
            return retentionSize;
        }

        public void setRetentionSize(Long retentionSize) {
            this.retentionSize = retentionSize;
        }

        public boolean isCompress() {
            return compress;
        }

        public void setCompress(boolean compress) {
            this.compress = compress;
        }

        public LogMode getMode() {
            return mode;
        }

        public void setMode(LogMode mode) {
            this.mode = mode;
        }
    }
}
