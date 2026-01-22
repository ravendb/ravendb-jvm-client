package net.ravendb.client.documents.operations.AI;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.documents.operations.etl.UpdateEtlOperation;
import net.ravendb.client.documents.operations.etl.UpdateEtlOperationResult;
import net.ravendb.client.http.RavenCommand;

import java.util.ArrayList;
import java.util.List;

public class UpdateEmbeddingsGenerationOperation
        implements IMaintenanceOperation<UpdateEtlOperationResult> {

    private final long taskId;
    private final EmbeddingsGenerationConfiguration configuration;
    private final boolean reset;

    public UpdateEmbeddingsGenerationOperation(long taskId,
                                               EmbeddingsGenerationConfiguration configuration,
                                               boolean reset) {
        this.taskId = taskId;
        this.configuration = configuration;
        this.reset = reset;
    }

    public UpdateEmbeddingsGenerationOperation(long taskId,
                                               EmbeddingsGenerationConfiguration configuration) {
        this(taskId, configuration, false);
    }

    @Override
    public RavenCommand<UpdateEtlOperationResult> getCommand(DocumentConventions conventions) {

        List<String> transformationsToReset = null;

        if (reset) {
            transformationsToReset = new ArrayList<>();
            transformationsToReset.add(configuration.getTransformationName());
        }

        return new UpdateEtlOperation.UpdateEtlCommand(
                conventions,
                taskId,
                configuration,
                transformationsToReset
        );
    }
}
