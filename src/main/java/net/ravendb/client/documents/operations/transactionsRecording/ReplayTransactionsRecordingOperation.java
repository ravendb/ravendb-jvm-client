package net.ravendb.client.documents.operations.transactionsRecording;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.entity.mime.InputStreamBody;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;

import java.io.IOException;
import java.io.InputStream;

/**
 * Operation to replay a recorded transaction.
 * This allows re-executing previously recorded transactions for debugging or recovery purposes.
 */
public final class ReplayTransactionsRecordingOperation implements IMaintenanceOperation<ReplayTxOperationResult> {

    private final InputStream _replayStream;
    private final long _operationId;

    /**
     * Operation to replay a recorded transaction.
     * @param replayStream The stream containing the recorded transaction. It must be positioned at its beginning
     *                     and it is not closed by this operation.
     * @param operationId A unique identifier for the replay operation.
     */
    public ReplayTransactionsRecordingOperation(InputStream replayStream, long operationId) {
        if (replayStream == null) {
            throw new IllegalArgumentException("replayStream cannot be null");
        }
        _replayStream = replayStream;
        _operationId = operationId;
    }

    @Override
    public RavenCommand<ReplayTxOperationResult> getCommand(DocumentConventions conventions) {
        return new ReplayTransactionsRecordingCommand(_replayStream, _operationId);
    }

    private static final class ReplayTransactionsRecordingCommand extends RavenCommand<ReplayTxOperationResult> {
        private final InputStream _replayStream;
        private final long _operationId;

        public ReplayTransactionsRecordingCommand(InputStream replayStream, long operationId) {
            super(ReplayTxOperationResult.class);
            _replayStream = replayStream;
            _operationId = operationId;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/transactions/replay?operationId=" + _operationId;

            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
            entityBuilder.addPart("file", new InputStreamBody(CloseShieldInputStream.wrap(_replayStream), "name"));

            HttpPost request = new HttpPost(url);
            request.setEntity(entityBuilder.build());
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
        public boolean isReadRequest() {
            return false;
        }
    }
}
