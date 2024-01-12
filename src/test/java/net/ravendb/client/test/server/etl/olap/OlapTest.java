package net.ravendb.client.test.server.etl.olap;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.operations.GetOngoingTaskInfoOperation;
import net.ravendb.client.documents.operations.backups.FtpSettings;
import net.ravendb.client.documents.operations.connectionStrings.PutConnectionStringOperation;
import net.ravendb.client.documents.operations.etl.AddEtlOperation;
import net.ravendb.client.documents.operations.etl.AddEtlOperationResult;
import net.ravendb.client.documents.operations.etl.EtlType;
import net.ravendb.client.documents.operations.etl.Transformation;
import net.ravendb.client.documents.operations.etl.olap.OlapConnectionString;
import net.ravendb.client.documents.operations.etl.olap.OlapEtlConfiguration;
import net.ravendb.client.documents.operations.ongoingTasks.OngoingTaskOlapEtl;
import net.ravendb.client.documents.operations.ongoingTasks.OngoingTaskType;
import net.ravendb.client.infrastructure.DisabledOnPullRequest;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@DisabledOnPullRequest
public class OlapTest extends RemoteTestBase {
    @Test
    public void canSetupOlap() throws Exception {
        try (DocumentStore store = getDocumentStore()) {

            OlapConnectionString connectionString = new OlapConnectionString();
            connectionString.setName("o1");
            connectionString.setFtpSettings(new FtpSettings());
            connectionString.getFtpSettings().setUrl("localhost:9090");

            store.maintenance().send(new PutConnectionStringOperation<>(connectionString));

            OlapEtlConfiguration etlConfiguration = new OlapEtlConfiguration();
            etlConfiguration.setConnectionStringName("o1");
            Transformation transformation = new Transformation();
            transformation.setCollections(Collections.singletonList("Orders"));
            transformation.setScript("var userData = { UserId: id(this), Name: this.Name }; loadToTest(userData)");
            transformation.setName("Script #1");

            etlConfiguration.setTransforms(Collections.singletonList(transformation));


            AddEtlOperationResult etlResult = store.maintenance().send(new AddEtlOperation<>(etlConfiguration));

            OngoingTaskOlapEtl ongoingTask = (OngoingTaskOlapEtl) store.maintenance()
                    .send(new GetOngoingTaskInfoOperation(etlResult.getTaskId(), OngoingTaskType.OLAP_ETL));

            assertThat(ongoingTask)
                    .isNotNull();
            assertThat(ongoingTask.getTaskType())
                    .isEqualTo(OngoingTaskType.OLAP_ETL);
            assertThat(ongoingTask.getConfiguration().getEtlType())
                    .isEqualTo(EtlType.OLAP);
            assertThat(ongoingTask.getConfiguration().getTransforms())
                    .hasSize(1);

        }
    }
}
