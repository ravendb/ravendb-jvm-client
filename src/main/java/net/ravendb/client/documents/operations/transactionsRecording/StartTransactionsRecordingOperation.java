package net.ravendb.client.documents.operations.transactionsRecording;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;

import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.serverwide.operations.IVoidMaintenanceOperation;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

public class StartTransactionsRecordingOperation implements IVoidMaintenanceOperation {

    private final String _filePath;

    public StartTransactionsRecordingOperation(String filePath) {
        if (filePath == null) {
            throw new IllegalArgumentException("FilePath cannot be null");
        }
        _filePath = filePath;
    }

    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new StartTransactionsRecordingCommand(conventions, _filePath);
    }

    private static class StartTransactionsRecordingCommand extends VoidRavenCommand {
        private final String _filePath;
        private final DocumentConventions _conventions;

        public StartTransactionsRecordingCommand(DocumentConventions conventions, String filePath) {
            _filePath = filePath;
            _conventions = conventions;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/transactions/start-recording";

            HttpPost request = new HttpPost(url);
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    generator.writeStartObject();
                    generator.writeStringField("File", _filePath);
                    generator.writeEndObject();
                }
            }, ContentType.APPLICATION_JSON, _conventions));
            return request;
        }
    }
}
