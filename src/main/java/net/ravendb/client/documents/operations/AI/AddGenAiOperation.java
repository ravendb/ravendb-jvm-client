package net.ravendb.client.documents.operations.AI;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.StartingPointChangeVector;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;
import java.io.IOException;

public class AddGenAiOperation implements IMaintenanceOperation<AddGenAiOperationResult> {

    private final GenAiConfiguration configuration;
    private final StartingPointChangeVector startingPoint;

    public AddGenAiOperation(GenAiConfiguration configuration, StartingPointChangeVector startingPoint) {
        if (configuration == null) {
            throw new IllegalArgumentException("configuration cannot be null");
        }
        this.configuration = configuration;
        this.startingPoint = startingPoint != null
                ? startingPoint
                : StartingPointChangeVector.LastDocument;
    }

    public AddGenAiOperation(GenAiConfiguration configuration) {
        this(configuration, null);
    }

    @Override
    public RavenCommand<AddGenAiOperationResult> getCommand(DocumentConventions conventions) {
        return new AddGenAiCommand(conventions, configuration, startingPoint);
    }

    final class AddGenAiCommand extends RavenCommand<AddGenAiOperationResult>
            implements IRaftCommand {

        private final DocumentConventions conventions;
        private final StartingPointChangeVector startingPoint;
        private final GenAiConfiguration configuration;
        private final String raftUniqueRequestId = RaftIdGenerator.newId();

        public AddGenAiCommand(DocumentConventions conventions, GenAiConfiguration configuration, StartingPointChangeVector startingPoint) {
            super(AddGenAiOperationResult.class);

            if (conventions == null) {
                throw new IllegalArgumentException("conventions cannot be null");
            }
            if (configuration == null) {
                throw new IllegalArgumentException("configuration cannot be null");
            }

            this.conventions = conventions;
            this.configuration = configuration;
            this.startingPoint = startingPoint != null
                    ? startingPoint
                    : StartingPointChangeVector.LastDocument;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl()
                    + "/databases/" + node.getDatabase()
                    + "/admin/etl?changeVector="
                    + urlEncode(startingPoint.getValue());

            HttpPut request = new HttpPut(url);
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    ObjectNode config = mapper.valueToTree(configuration);
                    generator.writeTree(config);
                }
            }, ContentType.APPLICATION_JSON, conventions));

            return request;
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                throwInvalidResponse();
            }

            result = mapper.readValue(response, resultClass);
        }

        @Override
        public String getRaftUniqueRequestId() {
            return raftUniqueRequestId;
        }
    }
}
