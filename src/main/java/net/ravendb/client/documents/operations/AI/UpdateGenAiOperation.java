package net.ravendb.client.documents.operations.AI;

import net.ravendb.client.documents.StartingPointChangeVector;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.documents.operations.etl.UpdateEtlOperation;
import net.ravendb.client.documents.operations.etl.UpdateEtlOperationResult;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.util.UrlUtils;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class UpdateGenAiOperation implements IMaintenanceOperation<UpdateEtlOperationResult> {

    private final long taskId;
    private final GenAiConfiguration configuration;
    private final StartingPointChangeVector startingPoint;
    private final boolean reset;

    public UpdateGenAiOperation(long taskId,
                                GenAiConfiguration configuration,
                                StartingPointChangeVector startingPoint,
                                boolean reset) {
        this.taskId = taskId;
        this.configuration = configuration;
        this.startingPoint = startingPoint;
        this.reset = reset;
    }

    public UpdateGenAiOperation(long taskId, GenAiConfiguration configuration, StartingPointChangeVector startingPoint) {
        this(taskId, configuration, startingPoint, false);
    }

    public UpdateGenAiOperation(long taskId, GenAiConfiguration configuration, boolean reset) {
        this(taskId, configuration, null, reset);
    }

    public UpdateGenAiOperation(long taskId,
                                GenAiConfiguration configuration) {
        this(taskId, configuration, null, false);
    }

    @Override
    public RavenCommand<UpdateEtlOperationResult> getCommand(DocumentConventions conventions) {

        List<String> transformationsToReset = null;

        if (reset) {
            transformationsToReset = new ArrayList<>();
            transformationsToReset.add(configuration.transformationName);
        }

        return new UpdateGenAiCommand(conventions, taskId, configuration, startingPoint, transformationsToReset);
    }


    final class UpdateGenAiCommand extends UpdateEtlOperation.UpdateEtlCommand {

        private final StartingPointChangeVector startingPoint;

        public UpdateGenAiCommand(DocumentConventions conventions,
                                  long taskId,
                                  GenAiConfiguration configuration,
                                  StartingPointChangeVector startingPoint,
                                  List<String> transformationsToReset) {

            super(conventions, taskId, configuration, transformationsToReset);

            this.startingPoint = (startingPoint != null)
                    ? startingPoint
                    : StartingPointChangeVector.DoNotChange;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {

            HttpUriRequestBase request = super.createRequest(node);

            String url = null;
            try {
                url = request.getUri().toString();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            url += "&changeVector=" + UrlUtils.escapeDataString(startingPoint.getValue());
            request.setUri(URI.create(url));

            return request;
        }
    }
}
